package vn.vietan.deepflow;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giọng tiếng Việt KorvaTTS chạy ngay trên điện thoại (ONNX Runtime): tải mô hình 1 lần, rồi đọc không cần mạng.
 * speak = đọc ra loa (xong mới trả lời) · synth = trả file WAV (ghép MP3 song ngữ) · prefetch = đọc trước câu sắp tới.
 */
@CapacitorPlugin(name = "Korva")
public class KorvaPlugin extends Plugin {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService player = Executors.newSingleThreadExecutor();
    private final ExecutorService downloader = Executors.newSingleThreadExecutor();
    private volatile KorvaEngine engine;
    private volatile String engineKey = "";
    private volatile boolean cancelDl = false, dlRunning = false;
    private final AtomicInteger speakGen = new AtomicInteger();
    private volatile AudioTrack track;
    /** câu đã đọc sẵn (khoá = giọng|tốc độ|bước|câu) */
    private final Map<String, float[]> cache = new LinkedHashMap<String, float[]>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, float[]> e) { return size() > 12; }
    };

    private File dir() { File d = new File(getContext().getFilesDir(), "korva"); d.mkdirs(); return d; }

    private static final String[] NEED = {"duration_predictor.int8.onnx", "text_encoder.int8.onnx", "vocoder.int8.onnx", "tts.json", "unicode_indexer.json", "voices.json"};

    private String vectorFile(String q) {
        File hq = new File(dir(), "vector_estimator.onnx"), lq = new File(dir(), "vector_estimator.int8.onnx");
        if ("hq".equals(q) && hq.exists()) return hq.getName();
        if (lq.exists()) return lq.getName();
        if (hq.exists()) return hq.getName();
        return null;
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        boolean ok = true; long size = 0;
        for (String f : NEED) { File x = new File(dir(), f); if (!x.exists()) ok = false; else size += x.length(); }
        File hq = new File(dir(), "vector_estimator.onnx"), lq = new File(dir(), "vector_estimator.int8.onnx");
        if (hq.exists()) size += hq.length();
        if (lq.exists()) size += lq.length();
        r.put("installed", ok && (hq.exists() || lq.exists()));
        r.put("hq", hq.exists()); r.put("lq", lq.exists());
        r.put("size", size);
        r.put("loaded", engine != null);
        r.put("downloading", dlRunning);
        r.put("freeDisk", dir().getUsableSpace());
        r.put("cores", Runtime.getRuntime().availableProcessors());
        try {
            File v = new File(dir(), "voices.json");
            if (v.exists()) {
                JSONObject o = new JSONObject(new String(java.nio.file.Files.readAllBytes(v.toPath()), "UTF-8"));
                JSArray a = new JSArray(); for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) a.put(it.next());
                r.put("voices", a);
            }
        } catch (Exception ignored) {}
        call.resolve(r);
    }

    /** tải lần lượt các file: files = [{name, url}] ; báo tiến độ qua sự kiện "progress" */
    @PluginMethod
    public void download(PluginCall call) {
        JSArray files = call.getArray("files");
        if (files == null) { call.reject("thiếu danh sách file"); return; }
        if (dlRunning) { call.reject("đang tải"); return; }
        cancelDl = false; dlRunning = true;
        downloader.execute(() -> {
            try {
                long done = 0, all = 0;
                for (int i = 0; i < files.length(); i++) all += files.getJSONObject(i).optLong("size", 0);
                for (int i = 0; i < files.length(); i++) {
                    JSONObject f = files.getJSONObject(i);
                    String name = f.getString("name");
                    File dst = new File(dir(), name);
                    long want = f.optLong("size", 0);
                    if (dst.exists() && (want == 0 || dst.length() == want)) { done += dst.length(); continue; }
                    done += fetch(f.getString("url"), dst, done, all, name);
                }
                synchronized (this) { if (engine != null) { engine.close(); engine = null; engineKey = ""; } }
                JSObject r = new JSObject(); r.put("ok", true); call.resolve(r);
            } catch (Exception e) {
                call.reject(cancelDl ? "cancelled" : (e.getMessage() == null ? e.toString() : e.getMessage()));
            } finally { dlRunning = false; }
        });
    }

    private long fetch(String url, File dst, long base, long all, String name) throws Exception {
        File part = new File(dst.getPath() + ".part");
        long have = part.exists() ? part.length() : 0L;
        URL u = new URL(url); int redirects = 0;
        while (true) {
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(20000); c.setReadTimeout(60000);
            if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) { String loc = c.getHeaderField("Location"); c.disconnect(); u = new URL(u, loc); if (++redirects > 8) throw new IOException("chuyển hướng quá nhiều"); continue; }
            if (code == 416) { c.disconnect(); break; }
            if (code != 200 && code != 206) throw new IOException("lỗi tải " + name + " (HTTP " + code + ")");
            if (code == 200) have = 0;
            try (java.io.InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part, have > 0)) {
                byte[] buf = new byte[1 << 16]; long got = have, last = 0;
                while (true) {
                    if (cancelDl) throw new InterruptedIOException("cancel");
                    int n = in.read(buf); if (n < 0) break;
                    out.write(buf, 0, n); got += n;
                    long now = System.currentTimeMillis();
                    if (now - last > 500) { last = now; JSObject ev = new JSObject(); ev.put("loaded", base + got); ev.put("total", all); ev.put("file", name); notifyListeners("progress", ev); }
                }
            }
            break;
        }
        if (dst.exists()) dst.delete();
        if (!part.renameTo(dst)) throw new IOException("không lưu được " + name);
        return dst.length();
    }

    @PluginMethod
    public void cancelDownload(PluginCall call) { cancelDl = true; call.resolve(); }

    @PluginMethod
    public void remove(PluginCall call) {
        worker.execute(() -> {
            synchronized (this) { if (engine != null) { engine.close(); engine = null; engineKey = ""; } }
            String only = call.getString("file");
            File[] fs = dir().listFiles();
            if (fs != null) for (File f : fs) if (only == null || f.getName().equals(only)) f.delete();
            call.resolve();
        });
    }

    private KorvaEngine eng(String q) throws Exception {
        String vf = vectorFile(q);
        if (vf == null) throw new IOException("not_installed");
        int thr = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
        String key = vf + "|" + thr;
        synchronized (this) {
            if (engine == null || !key.equals(engineKey)) {
                if (engine != null) engine.close();
                engine = null; cache.clear();
                engine = new KorvaEngine(dir(), ".int8", vf, thr);
                engineKey = key;
            }
            return engine;
        }
    }

    private static String ck(String text, String voice, int steps, float speed) { return voice + "|" + speed + "|" + steps + "|" + text; }

    private float[] get(PluginCall call, KorvaEngine.Cancel cancel) throws Exception {
        String text = call.getString("text", "");
        String voice = call.getString("voice", "khanh_vy");
        int steps = Math.max(2, Math.min(32, call.getInt("steps", 12)));
        float speed = Math.max(0.5f, Math.min(2f, call.getFloat("speed", 1.05f)));
        String k = ck(text, voice, steps, speed);
        synchronized (cache) { float[] w = cache.get(k); if (w != null) return w; }
        KorvaEngine e = eng(call.getString("quality", "lq"));
        float[] w = e.synth(text, voice, steps, speed, text.hashCode() * 31L + voice.hashCode(), cancel);
        synchronized (cache) { cache.put(k, w); }
        return w;
    }

    /** tải mô hình vào RAM sẵn (lần đầu mất vài giây) */
    @PluginMethod
    public void load(PluginCall call) {
        worker.execute(() -> {
            try { long t = System.currentTimeMillis(); eng(call.getString("quality", "lq")); JSObject r = new JSObject(); r.put("ms", System.currentTimeMillis() - t); call.resolve(r); }
            catch (Throwable e) { call.reject(String.valueOf(e.getMessage())); }
        });
    }

    @PluginMethod
    public void unload(PluginCall call) {
        worker.execute(() -> { synchronized (this) { if (engine != null) { engine.close(); engine = null; engineKey = ""; } } synchronized (cache) { cache.clear(); } call.resolve(); });
    }

    private final AtomicInteger preGen = new AtomicInteger();

    @PluginMethod
    public void prefetch(PluginCall call) {
        final int g = preGen.incrementAndGet();
        worker.execute(() -> {
            // tua nhanh qua nhiều câu → bỏ các câu đọc trước đã cũ, chỉ giữ 2 câu mới nhất
            if (preGen.get() - g >= 2) { call.resolve(); return; }
            try { long t = System.currentTimeMillis(); get(call, null); JSObject r = new JSObject(); r.put("ms", System.currentTimeMillis() - t); call.resolve(r); }
            catch (Throwable e) { call.reject(String.valueOf(e.getMessage())); }
        });
    }

    @PluginMethod
    public void synth(PluginCall call) {
        worker.execute(() -> {
            try {
                long t = System.currentTimeMillis();
                float[] w = get(call, null);
                JSObject r = new JSObject();
                r.put("data", Base64.encodeToString(KorvaEngine.wav16(w, engine.sampleRate), Base64.NO_WRAP));
                r.put("mime", "audio/wav"); r.put("ms", System.currentTimeMillis() - t); r.put("sec", w.length / (double) engine.sampleRate);
                call.resolve(r);
            } catch (Throwable e) { call.reject(String.valueOf(e.getMessage())); }
        });
    }

    /** đọc ra loa; trả lời khi đọc xong (hoặc khi bị dừng) */
    @PluginMethod
    public void speak(PluginCall call) {
        final int gen = speakGen.incrementAndGet();
        final float vol = call.getFloat("volume", 1f);
        worker.execute(() -> {
            if (gen != speakGen.get()) { call.resolve(); return; }
            float[] w;
            try { w = get(call, () -> gen != speakGen.get()); }
            catch (Throwable e) { if (gen != speakGen.get()) call.resolve(); else call.reject(String.valueOf(e.getMessage())); return; }
            if (gen != speakGen.get()) { call.resolve(); return; }
            final int sr = engine.sampleRate;
            player.execute(() -> {
                try { play(w, sr, gen, vol); } catch (Throwable ignored) {}
                call.resolve();
            });
        });
    }

    private void play(float[] w, int sr, int gen, float vol) {
        short[] pcm = new short[w.length];
        for (int i = 0; i < w.length; i++) { float c = Math.max(-1f, Math.min(1f, w[i] * vol)); pcm[i] = (short) (c * 32767); }
        int min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack t = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(sr).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(min * 2, 8192)).setTransferMode(AudioTrack.MODE_STREAM).build();
        track = t;
        try {
            t.play();
            int off = 0, CH = 2048;
            while (off < pcm.length && gen == speakGen.get()) { int n = t.write(pcm, off, Math.min(CH, pcm.length - off)); if (n <= 0) break; off += n; }
            if (gen == speakGen.get()) {
                // chờ phát hết phần còn trong bộ đệm
                long end = System.currentTimeMillis() + 3000;
                while (gen == speakGen.get() && t.getPlaybackHeadPosition() < pcm.length && System.currentTimeMillis() < end) { try { Thread.sleep(20); } catch (InterruptedException e) { break; } }
            }
        } finally {
            try { t.stop(); } catch (Throwable ignored) {}
            t.release(); if (track == t) track = null;
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        speakGen.incrementAndGet();
        AudioTrack t = track; if (t != null) try { t.pause(); t.flush(); } catch (Throwable ignored) {}
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        speakGen.incrementAndGet();
        try { if (engine != null) engine.close(); } catch (Throwable ignored) {}
        worker.shutdownNow(); player.shutdownNow(); downloader.shutdownNow();
    }
}
