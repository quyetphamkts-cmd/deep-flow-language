package vn.vietan.deepflow;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giọng đọc của điện thoại (Google TTS…) → file WAV, để ghép giọng tiếng Việt vào file MP3 song ngữ
 * xuất cho máy nghe nhạc. (Plugin TextToSpeech chỉ đọc ra loa, không lưu được thành file.)
 */
@CapacitorPlugin(name = "VoiceFile")
public class VoiceFilePlugin extends Plugin {
    private TextToSpeech tts;
    /** 0 = đang khởi động, 1 = sẵn sàng, -1 = máy không có bộ đọc */
    private volatile int ready = 0;
    private final Map<String, PluginCall> pending = new ConcurrentHashMap<>();
    private final Map<String, File> files = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void load() { init(); }

    private void init() {
        if (tts != null) return;
        ready = 0;
        tts = new TextToSpeech(getContext(), st -> {
            ready = st == TextToSpeech.SUCCESS ? 1 : -1;
            if (ready == 1) tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) { finish(id, null); }
                @Override public void onError(String id) { finish(id, "Bộ đọc báo lỗi"); }
                @Override public void onError(String id, int code) { finish(id, "Bộ đọc báo lỗi " + code); }
            });
        });
    }

    private void finish(String id, String err) {
        PluginCall c = pending.remove(id);
        File f = files.remove(id);
        if (c == null) { if (f != null) f.delete(); return; }
        if (err != null || f == null || !f.exists() || f.length() < 100) {
            if (f != null) f.delete();
            c.reject(err != null ? err : "Không tạo được âm thanh");
            return;
        }
        try {
            byte[] b = new byte[(int) f.length()];
            try (FileInputStream in = new FileInputStream(f)) {
                int off = 0, n;
                while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
            }
            JSObject r = new JSObject();
            r.put("data", Base64.encodeToString(b, Base64.NO_WRAP));
            r.put("mime", "audio/wav");
            c.resolve(r);
        } catch (Exception e) {
            c.reject("Đọc file lỗi: " + e.getMessage());
        } finally {
            f.delete();
        }
    }

    private Locale loc(String tag) {
        Locale l = Locale.forLanguageTag(tag == null || tag.isEmpty() ? "vi-VN" : tag);
        return l.getLanguage().isEmpty() ? new Locale("vi", "VN") : l;
    }

    private boolean has(Locale l) {
        int a = tts.isLanguageAvailable(l);
        return a >= TextToSpeech.LANG_AVAILABLE;
    }

    /** máy có giọng của ngôn ngữ này không (giọng offline / cần mạng) */
    @PluginMethod
    public void status(PluginCall call) {
        whenReady(call, () -> {
            JSObject r = new JSObject();
            Locale l = loc(call.getString("lang", "vi-VN"));
            r.put("ready", ready == 1);
            boolean ok = ready == 1 && has(l);
            r.put("available", ok);
            boolean offline = false, net = false;
            if (ok) {
                try {
                    for (Voice v : tts.getVoices()) {
                        if (v.getLocale() != null && v.getLocale().getLanguage().equals(l.getLanguage())) {
                            if (v.isNetworkConnectionRequired()) net = true; else offline = true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            r.put("offline", offline);
            r.put("network", net);
            try { r.put("engine", tts.getDefaultEngine()); } catch (Exception ignored) {}
            call.resolve(r);
        });
    }

    /** đọc text thành file WAV, trả về base64 */
    @PluginMethod
    public void synth(PluginCall call) {
        whenReady(call, () -> {
            if (ready != 1) { call.reject("no_tts"); return; }
            String text = call.getString("text", "");
            if (text == null || text.trim().isEmpty()) { call.reject("empty"); return; }
            Locale l = loc(call.getString("lang", "vi-VN"));
            if (!has(l)) { call.reject("no_voice"); return; }
            tts.setLanguage(l);
            Double rate = call.getDouble("rate", 1.0), pitch = call.getDouble("pitch", 1.0);
            tts.setSpeechRate(rate == null ? 1f : rate.floatValue());
            tts.setPitch(pitch == null ? 1f : pitch.floatValue());
            String id = "vf" + seq.incrementAndGet();
            File f = new File(getContext().getCacheDir(), id + ".wav");
            pending.put(id, call);
            files.put(id, f);
            Bundle p = new Bundle();
            int r = tts.synthesizeToFile(text.length() > 3900 ? text.substring(0, 3900) : text, p, f, id);
            if (r != TextToSpeech.SUCCESS) { finish(id, "Bộ đọc không nhận câu này"); return; }
            main.postDelayed(() -> { if (pending.containsKey(id)) finish(id, "timeout"); }, 30000);
        });
    }

    private void whenReady(PluginCall call, Runnable r) {
        if (tts == null) init();
        if (ready != 0) { r.run(); return; }
        final long t0 = System.currentTimeMillis();
        main.post(new Runnable() {
            @Override public void run() {
                if (ready != 0 || System.currentTimeMillis() - t0 > 6000) { r.run(); return; }
                main.postDelayed(this, 100);
            }
        });
    }

    @Override
    protected void handleOnDestroy() {
        try { if (tts != null) tts.shutdown(); } catch (Exception ignored) {}
        tts = null;
    }
}
