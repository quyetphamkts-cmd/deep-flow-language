package vn.vietan.deepflow;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trình chọn bài trong app: duyệt các thư mục bạn đã cho phép (Storage Access Framework),
 * chọn lẫn file và thư mục, 1 hoặc nhiều. Thư mục được cấp quyền 1 lần, app nhớ cho lần sau.
 * Đọc file: chép tạm vào bộ nhớ đệm của app rồi trang web đọc qua đường dẫn nội bộ (không chuyển dữ liệu lớn qua cầu nối).
 */
@CapacitorPlugin(name = "LocalFiles")
public class LocalFilesPlugin extends Plugin {
    static final String PREFS = "deepflow", KEY = "lfRoots";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService net = Executors.newFixedThreadPool(2);

    private SharedPreferences prefs() { return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private JSONArray rootsArr() {
        try { return new JSONArray(prefs().getString(KEY, "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    @PluginMethod
    public void roots(PluginCall call) {
        JSONArray a = rootsArr(), ok = new JSONArray();
        ContentResolver cr = getContext().getContentResolver();
        java.util.Set<String> held = new java.util.HashSet<>();
        for (UriPermission p : cr.getPersistedUriPermissions()) held.add(p.getUri().toString());
        for (int i = 0; i < a.length(); i++) {
            try { if (held.contains(a.getJSONObject(i).getString("tree"))) ok.put(a.getJSONObject(i)); } catch (Exception ignored) {}
        }
        prefs().edit().putString(KEY, ok.toString()).apply();
        JSObject r = new JSObject();
        try { r.put("roots", new JSArray(ok.toString())); } catch (Exception e) { r.put("roots", new JSArray()); }
        call.resolve(r);
    }

    private static java.util.List<Object> toList(JSONArray a) throws Exception {
        java.util.List<Object> l = new java.util.ArrayList<>();
        for (int i = 0; i < a.length(); i++) l.add(a.get(i));
        return l;
    }

    @PluginMethod
    public void pick(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(call, i, "pickResult");
    }

    @ActivityCallback
    private void pickResult(PluginCall call, ActivityResult res) {
        if (call == null) return;
        if (res.getResultCode() != Activity.RESULT_OK || res.getData() == null || res.getData().getData() == null) { call.reject("cancelled"); return; }
        Uri tree = res.getData().getData();
        try { getContext().getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) { call.reject("Không giữ được quyền đọc: " + e.getMessage()); return; }
        String docId = DocumentsContract.getTreeDocumentId(tree);
        Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, docId);
        String name = nameOf(doc);
        if (name == null) name = docId;
        JSONArray a = rootsArr();
        try {
            for (int k = 0; k < a.length(); k++) if (a.getJSONObject(k).getString("tree").equals(tree.toString())) { a.remove(k); break; }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("tree", tree.toString()); o.put("doc", doc.toString()); o.put("name", name);
            a.put(o);
        } catch (Exception ignored) {}
        prefs().edit().putString(KEY, a.toString()).apply();
        JSObject r = new JSObject();
        r.put("tree", tree.toString()); r.put("doc", doc.toString()); r.put("name", name);
        call.resolve(r);
    }

    @PluginMethod
    public void forget(PluginCall call) {
        String tree = call.getString("tree", "");
        JSONArray a = rootsArr(), b = new JSONArray();
        for (int i = 0; i < a.length(); i++) {
            try { if (!a.getJSONObject(i).getString("tree").equals(tree)) b.put(a.getJSONObject(i)); } catch (Exception ignored) {}
        }
        prefs().edit().putString(KEY, b.toString()).apply();
        try { getContext().getContentResolver().releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        call.resolve();
    }

    private String nameOf(Uri doc) {
        try (Cursor c = getContext().getContentResolver().query(doc, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return null;
    }

    /** liệt kê 1 thư mục: tree = quyền gốc, doc = thư mục cần xem (bỏ trống = gốc) */
    @PluginMethod
    public void list(PluginCall call) {
        String treeS = call.getString("tree"), docS = call.getString("doc");
        io.execute(() -> {
            try {
                Uri tree = Uri.parse(treeS);
                String parentId = docS != null && !docS.isEmpty() ? DocumentsContract.getDocumentId(Uri.parse(docS)) : DocumentsContract.getTreeDocumentId(tree);
                Uri kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
                JSArray a = new JSArray();
                try (Cursor c = getContext().getContentResolver().query(kids, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED}, null, null, null)) {
                    while (c != null && c.moveToNext()) {
                        JSObject o = new JSObject();
                        String id = c.getString(0), mime = c.getString(2);
                        o.put("doc", DocumentsContract.buildDocumentUriUsingTree(tree, id).toString());
                        o.put("name", c.getString(1));
                        o.put("mime", mime);
                        o.put("dir", DocumentsContract.Document.MIME_TYPE_DIR.equals(mime));
                        o.put("size", c.isNull(3) ? 0 : c.getLong(3));
                        o.put("mt", c.isNull(4) ? 0 : c.getLong(4));
                        a.put(o);
                    }
                }
                JSObject r = new JSObject(); r.put("items", a); call.resolve(r);
            } catch (Exception e) { call.reject("Không đọc được thư mục: " + e.getMessage()); }
        });
    }

    /** chép 1 file vào bộ nhớ đệm → trả đường dẫn file để trang web đọc */
    @PluginMethod
    public void copy(PluginCall call) {
        String docS = call.getString("doc"), name = call.getString("name", "file");
        io.execute(() -> {
            try {
                File dir = new File(getContext().getCacheDir(), "lf"); dir.mkdirs();
                File f = new File(dir, System.nanoTime() + "_" + name.replaceAll("[\\\\/:*?\"<>|]", "_"));
                try (InputStream in = getContext().getContentResolver().openInputStream(Uri.parse(docS)); OutputStream out = new FileOutputStream(f)) {
                    if (in == null) throw new Exception("không mở được file");
                    byte[] buf = new byte[1 << 16]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                JSObject r = new JSObject(); r.put("path", f.getAbsolutePath()); r.put("size", f.length()); call.resolve(r);
            } catch (Exception e) { call.reject("Không đọc được file: " + e.getMessage()); }
        });
    }

    /** tải 1 file từ mạng thẳng vào bộ nhớ đệm của app (theo chuyển hướng, có tiến độ) → trả đường dẫn file; không chuyển dữ liệu lớn qua cầu nối */
    @PluginMethod
    public void download(PluginCall call) {
        String url = call.getString("url"), name = call.getString("name", "audio"), ref = call.getString("referer", null);
        net.execute(() -> {
            java.net.HttpURLConnection c = null;
            try {
                File dir = new File(getContext().getCacheDir(), "lf"); dir.mkdirs();
                File f = new File(dir, System.nanoTime() + "_" + name.replaceAll("[\\\\/:*?\"<>|]", "_"));
                String u = url; int hops = 0;
                while (true) {
                    c = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
                    c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setInstanceFollowRedirects(false);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36");
                    if (ref != null) c.setRequestProperty("Referer", ref);
                    int code = c.getResponseCode();
                    if (code >= 300 && code < 400 && c.getHeaderField("Location") != null && hops++ < 8) { u = new java.net.URL(new java.net.URL(u), c.getHeaderField("Location")).toString(); c.disconnect(); continue; }
                    if (code != 200) throw new Exception("máy chủ trả mã " + code);
                    break;
                }
                long total = c.getContentLengthLong(), got = 0, lastEv = 0;
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[1 << 16]; int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n); got += n;
                        if (got - lastEv > (1 << 20)) { lastEv = got; JSObject ev = new JSObject(); ev.put("url", url); ev.put("loaded", got); ev.put("total", total); notifyListeners("dl", ev); }
                    }
                }
                JSObject r = new JSObject(); r.put("path", f.getAbsolutePath()); r.put("size", f.length()); r.put("type", c.getContentType()); call.resolve(r);
            } catch (Exception e) { call.reject("Không tải được: " + e.getMessage()); }
            finally { if (c != null) c.disconnect(); }
        });
    }

    @PluginMethod
    public void clean(PluginCall call) {
        String p = call.getString("path");
        io.execute(() -> {
            File dir = new File(getContext().getCacheDir(), "lf");
            if (p != null) { File f = new File(p); if (f.getParentFile() != null && f.getParentFile().equals(dir)) f.delete(); }
            else { File[] fs = dir.listFiles(); if (fs != null) for (File f : fs) f.delete(); }
            call.resolve();
        });
    }
}
