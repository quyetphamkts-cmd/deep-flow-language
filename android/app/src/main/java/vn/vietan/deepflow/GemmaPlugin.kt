package vn.vietan.deepflow

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Gemma chạy ngay trên điện thoại (LiteRT-LM): tải mô hình, nạp với mức RAM tuỳ chọn, sinh chữ (kèm âm thanh), giải phóng RAM.
 * Mọi việc nặng chạy ở luồng nền; mỗi lần chỉ làm 1 yêu cầu để không tràn RAM.
 */
@CapacitorPlugin(name = "Gemma")
class GemmaPlugin : Plugin() {
    private val worker = Executors.newSingleThreadExecutor()
    private val downloader = Executors.newSingleThreadExecutor()
    @Volatile private var engine: Engine? = null
    @Volatile private var loadedKey: String? = null
    @Volatile private var loadedFile: String? = null
    @Volatile private var backendUsed = ""
    @Volatile private var loadMs = 0L
    @Volatile private var running: Conversation? = null
    @Volatile private var cancelDl = false
    @Volatile private var dlRunning = false
    @Volatile private var trimHooked = false

    private fun dir(): File = File(context.filesDir, "models").apply { mkdirs() }

    // Android báo thiếu RAM → tự giải phóng mô hình để app không bị đóng
    private fun hookTrim() {
        if (trimHooked) return
        trimHooked = true
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL && level != ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                    worker.execute { closeEngine("Android báo thiếu RAM (mức $level)") }
                }
            }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() { worker.execute { closeEngine("Android báo thiếu RAM") } }
        })
    }

    private fun closeEngine(why: String?) {
        try { running?.cancelProcess() } catch (_: Throwable) {}
        val e = engine
        engine = null; loadedKey = null; loadedFile = null; backendUsed = ""
        try { e?.close() } catch (_: Throwable) {}
        if (why != null && e != null) {
            val ev = JSObject(); ev.put("reason", why); notifyListeners("unloaded", ev)
        }
    }

    @PluginMethod
    fun status(call: PluginCall) {
        val r = JSObject()
        val f = call.getString("file")
        if (f != null) {
            val m = File(dir(), f)
            r.put("downloaded", m.exists() && m.length() > 0)
            r.put("size", if (m.exists()) m.length() else 0L)
            val p = File(dir(), "$f.part")
            r.put("partial", if (p.exists()) p.length() else 0L)
        }
        r.put("loaded", engine != null)
        r.put("loadedFile", loadedFile)
        r.put("backend", backendUsed)
        r.put("loadMs", loadMs)
        r.put("downloading", dlRunning)
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
            r.put("ramTotal", mi.totalMem); r.put("ramAvail", mi.availMem); r.put("lowMem", mi.lowMemory)
        } catch (_: Throwable) {}
        r.put("freeDisk", dir().usableSpace)
        call.resolve(r)
    }

    @PluginMethod
    fun download(call: PluginCall) {
        val url = call.getString("url") ?: return call.reject("thiếu url")
        val file = call.getString("file") ?: return call.reject("thiếu tên file")
        val token = call.getString("token")
        if (dlRunning) return call.reject("đang tải mô hình khác")
        cancelDl = false; dlRunning = true
        downloader.execute {
            try {
                val part = File(dir(), "$file.part")
                var have = if (part.exists()) part.length() else 0L
                var u = URL(url)
                var redirects = 0
                while (true) {
                    val conn = u.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 20000; conn.readTimeout = 60000
                    if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
                    if (!token.isNullOrEmpty() && u.host.endsWith("huggingface.co")) conn.setRequestProperty("Authorization", "Bearer $token")
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location"); conn.disconnect()
                        u = URL(u, loc); if (++redirects > 8) throw IOException("chuyển hướng quá nhiều lần"); continue
                    }
                    if (code == 416) { conn.disconnect(); break } // đã tải đủ
                    if (code == 401 || code == 403) throw IOException("máy chủ từ chối tải (HTTP $code) — mô hình có thể cần token Hugging Face")
                    if (code != 200 && code != 206) throw IOException("lỗi tải HTTP $code")
                    if (code == 200) have = 0L
                    val len = conn.getHeaderFieldLong("Content-Length", -1L)
                    val total = have + (if (len < 0) 0L else len)
                    conn.inputStream.use { ins ->
                        FileOutputStream(part, have > 0).use { out ->
                            val buf = ByteArray(1 shl 16); var got = have; var last = 0L
                            while (true) {
                                if (cancelDl) throw InterruptedIOException("cancel")
                                val n = ins.read(buf); if (n < 0) break
                                out.write(buf, 0, n); got += n
                                val now = System.currentTimeMillis()
                                if (now - last > 500) {
                                    last = now
                                    val ev = JSObject(); ev.put("loaded", got); ev.put("total", total); ev.put("file", file)
                                    notifyListeners("progress", ev)
                                }
                            }
                        }
                    }
                    break
                }
                val dst = File(dir(), file)
                if (dst.exists()) dst.delete()
                if (!part.renameTo(dst)) throw IOException("không lưu được file mô hình")
                val r = JSObject(); r.put("size", dst.length()); call.resolve(r)
            } catch (e: Exception) {
                call.reject(if (cancelDl) "cancelled" else (e.message ?: e.toString()))
            } finally { dlRunning = false }
        }
    }

    @PluginMethod
    fun cancelDownload(call: PluginCall) { cancelDl = true; call.resolve() }

    @PluginMethod
    fun deleteModel(call: PluginCall) {
        val file = call.getString("file") ?: return call.reject("thiếu tên file")
        worker.execute {
            if (loadedFile == file) closeEngine(null)
            File(dir(), file).delete(); File(dir(), "$file.part").delete()
            call.resolve()
        }
    }

    @PluginMethod
    fun load(call: PluginCall) {
        val file = call.getString("file") ?: return call.reject("thiếu tên file")
        val want = call.getString("backend", "cpu") ?: "cpu"
        val maxTok = call.getInt("maxTokens", 2048) ?: 2048
        val threads = call.getInt("threads", 0) ?: 0
        val audio = call.getBoolean("audio", false) ?: false
        val key = "$file|$want|$maxTok|$threads|$audio"
        worker.execute {
            try {
                val f = File(dir(), file)
                if (!f.exists()) throw IOException("chưa tải mô hình")
                if (engine != null && loadedKey == key) {
                    val r = JSObject(); r.put("backend", backendUsed); r.put("ms", 0); r.put("cached", true); call.resolve(r); return@execute
                }
                closeEngine(null)
                hookTrim()
                val t0 = System.currentTimeMillis()
                fun cpu(): Backend = if (threads > 0) Backend.CPU(threads) else Backend.CPU()
                fun make(b: Backend): Engine {
                    val cfg = EngineConfig(f.absolutePath, b, null, if (audio) cpu() else null, maxTok, null, context.cacheDir.path)
                    val e = Engine(cfg); e.initialize(); return e
                }
                var used = want
                val e = try {
                    make(if (want == "gpu") Backend.GPU() else cpu())
                } catch (t: Throwable) {
                    if (want == "gpu") { used = "cpu"; make(cpu()) } else throw t
                }
                engine = e; loadedKey = key; loadedFile = file; backendUsed = used
                loadMs = System.currentTimeMillis() - t0
                val r = JSObject(); r.put("backend", used); r.put("ms", loadMs); call.resolve(r)
            } catch (t: Throwable) {
                closeEngine(null)
                call.reject("không nạp được mô hình: " + (t.message ?: t.toString()))
            }
        }
    }

    @PluginMethod
    fun generate(call: PluginCall) {
        val prompt = call.getString("prompt") ?: ""
        val system = call.getString("system")
        val temp = call.getDouble("temperature", 0.3) ?: 0.3
        val topK = call.getInt("topK", 40) ?: 40
        val audioB64 = call.getString("audio") // PCM 16-bit mono 16 kHz (base64)
        worker.execute {
            val e = engine
            if (e == null) { call.reject("mô hình chưa được nạp"); return@execute }
            try {
                val t0 = System.currentTimeMillis()
                val cfg = ConversationConfig(
                    systemInstruction = if (system.isNullOrEmpty()) null else Contents.of(system),
                    samplerConfig = SamplerConfig(topK = topK, topP = 0.95, temperature = temp)
                )
                val parts = ArrayList<Content>()
                if (!audioB64.isNullOrEmpty()) parts.add(Content.AudioBytes(Base64.decode(audioB64, Base64.DEFAULT)))
                parts.add(Content.Text(prompt))
                val text = e.createConversation(cfg).use { conv ->
                    running = conv
                    try { conv.sendMessage(Contents.of(parts)).toString() } finally { running = null }
                }
                val r = JSObject(); r.put("text", text); r.put("ms", System.currentTimeMillis() - t0); call.resolve(r)
            } catch (t: Throwable) {
                call.reject("Gemma lỗi: " + (t.message ?: t.toString()))
            }
        }
    }

    @PluginMethod
    fun cancel(call: PluginCall) { try { running?.cancelProcess() } catch (_: Throwable) {}; call.resolve() }

    @PluginMethod
    fun unload(call: PluginCall) { worker.execute { closeEngine(null); call.resolve() } }

    override fun handleOnDestroy() { try { closeEngine(null) } catch (_: Throwable) {}; super.handleOnDestroy() }
}
