package vn.vietan.deepflow;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * KorvaTTS (Apache-2.0, dogenthq/KorvaTTS — kiến trúc Supertonic 3, MIT) chạy bằng ONNX Runtime ngay trên máy.
 * Chép lại đúng quy trình của gói Python korvatts 0.1.3: chuẩn hoá chữ (NFKD) → duration_predictor → text_encoder
 * → lặp vector_estimator (flow matching) → vocoder → sóng âm 44,1 kHz → cắt tiếng rè cuối câu.
 * Không dùng thư viện Android nào để có thể chạy thử trên máy tính (CI).
 */
public class KorvaEngine implements AutoCloseable {
    public final int sampleRate;
    private final int baseChunk, compress, latentDim;
    private final int[] indexer;
    private final OrtEnvironment env;
    private final OrtSession dp, te, ve, voc;
    private final Map<String, float[][]> voices = new LinkedHashMap<>();
    private final long[] ttlShape, dpShape;

    public KorvaEngine(File dir, String vectorFile, int threads) throws Exception {
        JSONObject cfg = new JSONObject(read(new File(dir, "tts.json")));
        sampleRate = cfg.getJSONObject("ae").getInt("sample_rate");
        baseChunk = cfg.getJSONObject("ae").getInt("base_chunk_size");
        compress = cfg.getJSONObject("ttl").getInt("chunk_compress_factor");
        latentDim = cfg.getJSONObject("ttl").getInt("latent_dim");
        JSONArray ix = new JSONArray(read(new File(dir, "unicode_indexer.json")));
        indexer = new int[ix.length()];
        for (int i = 0; i < indexer.length; i++) indexer[i] = ix.getInt(i);
        // voices.json: { "<tên>": { "style_ttl": {dims,data}, "style_dp": {dims,data} }, ... }
        JSONObject vs = new JSONObject(read(new File(dir, "voices.json")));
        long[] ts = null, ds = null;
        for (java.util.Iterator<String> it = vs.keys(); it.hasNext(); ) {
            String k = it.next();
            JSONObject v = vs.getJSONObject(k);
            JSONObject t = v.getJSONObject("style_ttl"), d = v.getJSONObject("style_dp");
            ts = dims(t); ds = dims(d);
            voices.put(k, new float[][]{flat(t.get("data")), flat(d.get("data"))});
        }
        ttlShape = ts; dpShape = ds;
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions o = new OrtSession.SessionOptions();
        if (threads > 0) o.setIntraOpNumThreads(threads);
        o.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        dp = env.createSession(new File(dir, "duration_predictor.onnx").getAbsolutePath(), o);
        te = env.createSession(new File(dir, "text_encoder.onnx").getAbsolutePath(), o);
        ve = env.createSession(new File(dir, vectorFile).getAbsolutePath(), o);
        voc = env.createSession(new File(dir, "vocoder.onnx").getAbsolutePath(), o);
    }

    public List<String> voiceNames() { return new ArrayList<>(voices.keySet()); }
    public boolean hasVoice(String v) { return voices.containsKey(v); }

    private static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }
    private static long[] dims(JSONObject e) throws Exception { JSONArray a = e.getJSONArray("dims"); long[] r = new long[a.length()]; for (int i = 0; i < r.length; i++) r[i] = a.getLong(i); return r; }
    private static float[] flat(Object data) throws Exception { List<Float> out = new ArrayList<>(); walk(data, out); float[] r = new float[out.size()]; for (int i = 0; i < r.length; i++) r[i] = out.get(i); return r; }
    private static void walk(Object o, List<Float> out) throws Exception {
        if (o instanceof JSONArray) { JSONArray a = (JSONArray) o; for (int i = 0; i < a.length(); i++) walk(a.get(i), out); }
        else out.add(((Number) o).floatValue());
    }

    // ---------------- chữ → mã ký tự (giống korvatts/text_processor.py) ----------------
    private static final Pattern EMOJI = Pattern.compile("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F700}-\\x{1F77F}\\x{1F780}-\\x{1F7FF}\\x{1F800}-\\x{1F8FF}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{1FA70}-\\x{1FAFF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F1E6}-\\x{1F1FF}]+");
    private static final String[][] CHARS = {{"–", "-"}, {"‑", "-"}, {"—", "-"}, {"_", " "}, {"“", "\""}, {"”", "\""}, {"‘", "'"}, {"’", "'"}, {"´", "'"}, {"`", "'"},
            {"[", " "}, {"]", " "}, {"|", " "}, {"/", " "}, {"#", " "}, {"→", " "}, {"←", " "}};
    private static final String[][] EXPR = {{"@", " at "}, {"e.g.,", "for example, "}, {"i.e.,", "that is, "}};
    private static final Pattern TRAIL = Pattern.compile("[.!?;:,'\"')\\]}…。」』】〉》›»]$");
    private static final Pattern SPLIT = Pattern.compile("(?<!Mr\\.)(?<!Mrs\\.)(?<!Ms\\.)(?<!Dr\\.)(?<!Prof\\.)(?<!Sr\\.)(?<!Jr\\.)(?<!Ph\\.D\\.)(?<!etc\\.)(?<!e\\.g\\.)(?<!i\\.e\\.)(?<!vs\\.)(?<!Inc\\.)(?<!Ltd\\.)(?<!Co\\.)(?<!Corp\\.)(?<!St\\.)(?<!Ave\\.)(?<!Blvd\\.)(?<!\\b[A-Z]\\.)(?<=[.!?])\\s+");

    public static String normalize(String text, String lang) {
        text = Normalizer.normalize(text, Normalizer.Form.NFKD);
        text = EMOJI.matcher(text).replaceAll("");
        for (String[] r : CHARS) text = text.replace(r[0], r[1]);
        text = text.replaceAll("[♥☆♡©\\\\]", "");
        for (String[] r : EXPR) text = text.replace(r[0], r[1]);
        text = text.replaceAll(" ([,.!?;:'])", "$1");
        for (String dup : new String[]{"\"\"", "''", "``"}) while (text.contains(dup)) text = text.replace(dup, dup.substring(0, 1));
        text = text.replaceAll("\\s+", " ").trim();
        if (!text.isEmpty() && !TRAIL.matcher(text).find()) text += ".";
        return "<" + lang + ">" + text + "</" + lang + ">";
    }

    long[] ids(String text) {
        int[] cps = text.codePoints().toArray();
        long[] tmp = new long[cps.length]; int n = 0;
        for (int c : cps) if (c < indexer.length && indexer[c] >= 0) tmp[n++] = indexer[c];
        long[] r = new long[n]; System.arraycopy(tmp, 0, r, 0, n); return r;
    }

    public static List<String> chunks(String text, int maxLen) {
        List<String> out = new ArrayList<>();
        for (String para : text.trim().split("\\n\\s*\\n+")) {
            para = para.trim(); if (para.isEmpty()) continue;
            String cur = "";
            for (String sen : SPLIT.split(para)) {
                for (String piece : splitLong(sen, maxLen)) {
                    if (cur.length() + piece.length() + 1 <= maxLen) cur += (cur.isEmpty() ? "" : " ") + piece;
                    else { if (!cur.isEmpty()) out.add(cur.trim()); cur = piece; }
                }
            }
            if (!cur.isEmpty()) out.add(cur.trim());
        }
        return out;
    }

    private static List<String> splitLong(String s, int maxLen) {
        List<String> r = new ArrayList<>();
        if (s.length() <= maxLen) { r.add(s); return r; }
        List<String> pieces = new ArrayList<>(); String cur = "";
        for (String w : s.trim().split("\\s+")) {
            if (!cur.isEmpty() && cur.length() + w.length() + 1 > maxLen) { pieces.add(cur); cur = w; }
            else cur += (cur.isEmpty() ? "" : " ") + w;
        }
        if (!cur.isEmpty()) pieces.add(cur);
        for (String p : pieces) for (int i = 0; i < p.length(); i += maxLen) r.add(p.substring(i, Math.min(p.length(), i + maxLen)));
        return r;
    }

    // ---------------- tổng hợp ----------------
    public interface Cancel { boolean cancelled(); }

    /** đọc cả đoạn (tự chia câu dài), trả về sóng âm 44,1 kHz; nghỉ 0,3 s giữa các đoạn */
    public float[] synth(String text, String voice, int steps, float speed, long seed, Cancel cancel) throws Exception {
        float[][] st = voices.get(voice);
        if (st == null) st = voices.values().iterator().next();
        Random rng = new Random(seed);
        List<float[]> parts = new ArrayList<>();
        int sil = (int) (0.3 * sampleRate), total = 0;
        for (String ch : chunks(text, 300)) {
            if (cancel != null && cancel.cancelled()) throw new InterruptedException("cancel");
            float[] w = infer(ch, "vi", st, steps, speed, rng, cancel);
            if (w == null || w.length == 0) continue;
            if (!parts.isEmpty()) { parts.add(new float[sil]); total += sil; }
            parts.add(w); total += w.length;
        }
        float[] out = new float[total]; int o = 0;
        for (float[] p : parts) { System.arraycopy(p, 0, out, o, p.length); o += p.length; }
        return out;
    }

    private float[] infer(String text, String lang, float[][] st, int steps, float speed, Random rng, Cancel cancel) throws Exception {
        long[] id = ids(normalize(text, lang));
        if (id.length == 0) return new float[0];
        int L = id.length;
        float[] tm = new float[L]; java.util.Arrays.fill(tm, 1f);
        Map<String, OnnxTensor> in = new HashMap<>();
        try (OnnxTensor tIds = OnnxTensor.createTensor(env, LongBuffer.wrap(id), new long[]{1, L});
             OnnxTensor tMask = OnnxTensor.createTensor(env, FloatBuffer.wrap(tm), new long[]{1, 1, L});
             OnnxTensor sTtl = OnnxTensor.createTensor(env, FloatBuffer.wrap(st[0]), ttlShape);
             OnnxTensor sDp = OnnxTensor.createTensor(env, FloatBuffer.wrap(st[1]), dpShape)) {
            float dur;
            in.put("text_ids", tIds); in.put("style_dp", sDp); in.put("text_mask", tMask);
            try (OrtSession.Result r = dp.run(in)) { dur = ((OnnxTensor) r.get(0)).getFloatBuffer().get(0) / speed; }
            in.clear(); in.put("text_ids", tIds); in.put("style_ttl", sTtl); in.put("text_mask", tMask);
            try (OrtSession.Result er = te.run(in)) {
                OnnxTensor emb = (OnnxTensor) er.get(0);
                int wavLen = (int) (dur * sampleRate), chunk = baseChunk * compress;
                int lat = Math.max(1, (wavLen + chunk - 1) / chunk), D = latentDim * compress;
                float[] x = new float[D * lat];
                for (int i = 0; i < x.length; i++) x[i] = (float) rng.nextGaussian();
                float[] lm = new float[lat]; java.util.Arrays.fill(lm, 1f);
                float[] totalStep = {steps};
                try (OnnxTensor tLm = OnnxTensor.createTensor(env, FloatBuffer.wrap(lm), new long[]{1, 1, lat});
                     OnnxTensor tTot = OnnxTensor.createTensor(env, FloatBuffer.wrap(totalStep), new long[]{1})) {
                    for (int s = 0; s < steps; s++) {
                        if (cancel != null && cancel.cancelled()) throw new InterruptedException("cancel");
                        try (OnnxTensor tx = OnnxTensor.createTensor(env, FloatBuffer.wrap(x), new long[]{1, D, lat});
                             OnnxTensor tCur = OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[]{s}), new long[]{1})) {
                            in.clear();
                            in.put("noisy_latent", tx); in.put("text_emb", emb); in.put("style_ttl", sTtl);
                            in.put("text_mask", tMask); in.put("latent_mask", tLm); in.put("current_step", tCur); in.put("total_step", tTot);
                            try (OrtSession.Result vr = ve.run(in)) {
                                FloatBuffer fb = ((OnnxTensor) vr.get(0)).getFloatBuffer();
                                float[] nx = new float[fb.remaining()]; fb.get(nx); x = nx;
                            }
                        }
                    }
                }
                in.clear();
                try (OnnxTensor tx = OnnxTensor.createTensor(env, FloatBuffer.wrap(x), new long[]{1, D, lat})) {
                    in.put("latent", tx);
                    try (OrtSession.Result wr = voc.run(in)) {
                        FloatBuffer fb = ((OnnxTensor) wr.get(0)).getFloatBuffer();
                        int n = Math.min(fb.remaining(), Math.max(0, wavLen));
                        float[] w = new float[n]; fb.get(w, 0, n);
                        return cleanTail(w, sampleRate);
                    }
                }
            }
        }
    }

    // ---------------- cắt tiếng rè cuối câu (giống korvatts/audio_post.py) ----------------
    static float[] cleanTail(float[] wav, int sr) {
        int frame = (int) (0.02 * sr), start = Math.max(0, wav.length - (int) (0.6 * sr));
        int gapStart = -1, run = 0, best = -1, minGap = (int) (0.05 * sr);
        for (int i = start; i < wav.length - frame; i += frame) {
            double s = 0; for (int k = i; k < i + frame; k++) s += wav[k] * wav[k];
            double rms = Math.sqrt(s / frame);
            if (rms < 0.01) { if (run == 0) gapStart = i; run += frame; }
            else { if (run >= minGap) best = gapStart; run = 0; }
        }
        if (run >= minGap) best = gapStart;
        int cut = -1;
        if (best >= 0) {
            int a = best + minGap; float pk = 0;
            for (int k = a; k < wav.length; k++) pk = Math.max(pk, Math.abs(wav[k]));
            int afterLen = Math.max(0, wav.length - a);
            if (pk < 0.05f) cut = best;
            else if (afterLen <= (int) (0.35 * sr) && pk >= 0.4f) cut = best;
        }
        if (cut >= 0) {
            int n = Math.min(wav.length, cut + (int) (0.05 * sr));
            float[] w2 = new float[n]; System.arraycopy(wav, 0, w2, 0, n); wav = w2;
        }
        int n = Math.min(wav.length, (int) (0.03 * sr));
        for (int i = 0; i < n; i++) wav[wav.length - n + i] *= 1f - (float) i / Math.max(1, n - 1);
        return wav;
    }

    /** sóng âm → WAV 16-bit mono */
    public static byte[] wav16(float[] w, int sr) {
        int n = w.length; byte[] b = new byte[44 + n * 2];
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes()); bb.putInt(36 + n * 2); bb.put("WAVE".getBytes()); bb.put("fmt ".getBytes());
        bb.putInt(16); bb.putShort((short) 1); bb.putShort((short) 1); bb.putInt(sr); bb.putInt(sr * 2); bb.putShort((short) 2); bb.putShort((short) 16);
        bb.put("data".getBytes()); bb.putInt(n * 2);
        for (float v : w) { float c = Math.max(-1f, Math.min(1f, v)); bb.putShort((short) (c * 32767)); }
        return b;
    }

    @Override
    public void close() {
        for (OrtSession s : new OrtSession[]{dp, te, ve, voc}) try { if (s != null) s.close(); } catch (OrtException ignored) {}
    }

    /** chạy thử trên máy tính: java KorvaEngine <thư mục> <file vector_estimator> <giọng> <số bước> "<câu>" <ra.wav> */
    public static void main(String[] a) throws Exception {
        long t0 = System.currentTimeMillis();
        try (KorvaEngine e = new KorvaEngine(new File(a[0]), a[1], 4)) {
            long t1 = System.currentTimeMillis();
            System.out.println("voices=" + e.voiceNames() + " load_ms=" + (t1 - t0));
            System.out.println("norm=" + normalize(a[4], "vi") + " ids=" + e.ids(normalize(a[4], "vi")).length);
            float[] w = e.synth(a[4], a[2], Integer.parseInt(a[3]), 1.05f, 42, null);
            long t2 = System.currentTimeMillis();
            double s = 0; for (float v : w) s += v * v;
            System.out.println("samples=" + w.length + " sec=" + (w.length / (double) e.sampleRate) + " rms=" + Math.sqrt(s / Math.max(1, w.length)) + " synth_ms=" + (t2 - t1));
            Files.write(new File(a[5]).toPath(), wav16(w, e.sampleRate));
        }
    }
}
