package vn.vietan.deepflow;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.documentfile.provider.DocumentFile;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chép bài thẳng vào máy nghe nhạc (MP3 player) cắm vào điện thoại qua cáp USB OTG.
 * Máy nghe nhạc kiểu M15 là “ổ USB” (USB Mass Storage): Android gắn nó như 1 bộ nhớ ngoài.
 * Người dùng chọn ổ 1 lần (Storage Access Framework) → app giữ quyền ghi, lần sau cắm vào là chép được ngay.
 */
@CapacitorPlugin(name = "UsbDrive")
public class UsbDrivePlugin extends Plugin {
    static final String PREFS = "deepflow";
    static final String KEY = "usbTreeUri";
    private BroadcastReceiver receiver;
    /** đường dẫn thư mục → DocumentFile (tìm thư mục con bằng SAF rất chậm, nên nhớ lại) */
    private final Map<String, DocumentFile> dirCache = new HashMap<>();

    @Override
    public void load() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                dirCache.clear();
                JSObject d = new JSObject();
                d.put("mounted", Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction()));
                notifyListeners("change", d);
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addAction(Intent.ACTION_MEDIA_REMOVED);
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addDataScheme("file");
        try {
            if (Build.VERSION.SDK_INT >= 33) getContext().registerReceiver(receiver, f, Context.RECEIVER_EXPORTED);
            else getContext().registerReceiver(receiver, f);
        } catch (Exception ignored) {}
    }

    @Override
    protected void handleOnDestroy() {
        try { if (receiver != null) getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    private SharedPreferences prefs() { return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private DocumentFile root() {
        String s = prefs().getString(KEY, null);
        if (s == null) return null;
        try {
            DocumentFile d = DocumentFile.fromTreeUri(getContext(), Uri.parse(s));
            if (d != null && d.exists() && d.canWrite()) return d;
        } catch (Exception ignored) {}
        return null;
    }

    /** các ổ ngoài (USB / thẻ nhớ) đang gắn — để báo “đã cắm máy nghe nhạc” */
    private JSArray volumes() {
        JSArray a = new JSArray();
        try {
            StorageManager sm = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
            List<StorageVolume> vs = sm.getStorageVolumes();
            for (StorageVolume v : vs) {
                if (v.isPrimary() || !v.isRemovable()) continue;
                JSObject o = new JSObject();
                o.put("name", v.getDescription(getContext()));
                o.put("state", v.getState());
                if (Build.VERSION.SDK_INT >= 24) o.put("uuid", v.getUuid());
                a.put(o);
            }
        } catch (Exception ignored) {}
        return a;
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        DocumentFile d = root();
        r.put("picked", prefs().getString(KEY, null) != null);
        r.put("ready", d != null);
        if (d != null) r.put("name", d.getName());
        r.put("volumes", volumes());
        call.resolve(r);
    }

    @PluginMethod
    public void pick(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                StorageManager sm = (StorageManager) getContext().getSystemService(Context.STORAGE_SERVICE);
                for (StorageVolume v : sm.getStorageVolumes()) {
                    if (!v.isPrimary() && v.isRemovable() && "mounted".equals(v.getState())) {
                        Intent vi = v.createOpenDocumentTreeIntent();
                        Uri init = vi.getParcelableExtra("android.provider.extra.INITIAL_URI");
                        if (init != null) i.putExtra("android.provider.extra.INITIAL_URI", init);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        startActivityForResult(call, i, "pickResult");
    }

    @ActivityCallback
    private void pickResult(PluginCall call, ActivityResult res) {
        if (call == null) return;
        if (res.getResultCode() != Activity.RESULT_OK || res.getData() == null || res.getData().getData() == null) {
            call.reject("cancelled");
            return;
        }
        Uri u = res.getData().getData();
        try {
            ContentResolver cr = getContext().getContentResolver();
            // bỏ quyền ổ cũ (nếu có) để không giữ quá nhiều quyền
            String old = prefs().getString(KEY, null);
            if (old != null && !old.equals(u.toString())) {
                for (UriPermission p : cr.getPersistedUriPermissions()) {
                    if (p.getUri().toString().equals(old)) {
                        try { cr.releasePersistableUriPermission(p.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (Exception ignored) {}
                    }
                }
            }
            cr.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            call.reject("Không giữ được quyền ghi: " + e.getMessage());
            return;
        }
        prefs().edit().putString(KEY, u.toString()).apply();
        dirCache.clear();
        DocumentFile d = DocumentFile.fromTreeUri(getContext(), u);
        JSObject r = new JSObject();
        r.put("ready", d != null && d.canWrite());
        r.put("name", d != null ? d.getName() : "");
        call.resolve(r);
    }

    /** thư mục theo đường dẫn "Music/1.Original. 2.1.Bad choices" (tạo nếu chưa có) */
    private DocumentFile dir(DocumentFile r, String path, boolean create) {
        if (path == null || path.isEmpty()) return r;
        DocumentFile cur = r;
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) continue;
            if (sb.length() > 0) sb.append('/');
            sb.append(seg);
            String key = sb.toString();
            DocumentFile c = dirCache.get(key);
            if (c == null || !c.exists()) {
                c = cur.findFile(seg);
                if (c == null) {
                    if (!create) return null;
                    c = cur.createDirectory(seg);
                    if (c == null) return null;
                } else if (!c.isDirectory()) return null;
                dirCache.put(key, c);
            }
            cur = c;
        }
        return cur;
    }

    private static String parentOf(String p) { int i = p.lastIndexOf('/'); return i < 0 ? "" : p.substring(0, i); }
    private static String nameOf(String p) { int i = p.lastIndexOf('/'); return i < 0 ? p : p.substring(i + 1); }

    /** liệt kê 1 thư mục: tên + dung lượng (để bỏ qua bài đã có trên máy) */
    @PluginMethod
    public void list(PluginCall call) {
        DocumentFile r = root();
        if (r == null) { call.reject("not_ready"); return; }
        DocumentFile d = dir(r, call.getString("path", ""), false);
        JSArray a = new JSArray();
        if (d != null) {
            for (DocumentFile f : d.listFiles()) {
                JSObject o = new JSObject();
                o.put("name", f.getName());
                o.put("dir", f.isDirectory());
                o.put("size", f.length());
                a.put(o);
            }
        }
        JSObject res = new JSObject();
        res.put("files", a);
        call.resolve(res);
    }

    /** ghi file theo từng phần base64: append=false → tạo mới (ghi đè), append=true → nối tiếp */
    @PluginMethod
    public void write(PluginCall call) {
        String path = call.getString("path");
        String data = call.getString("data", "");
        boolean append = Boolean.TRUE.equals(call.getBoolean("append", false));
        String mime = call.getString("mime", "application/octet-stream");
        DocumentFile r = root();
        if (r == null) { call.reject("not_ready"); return; }
        if (path == null || path.isEmpty()) { call.reject("no_path"); return; }
        try {
            DocumentFile d = dir(r, parentOf(path), true);
            if (d == null) { call.reject("Không tạo được thư mục " + parentOf(path)); return; }
            String nm = nameOf(path);
            DocumentFile f = d.findFile(nm);
            if (!append) {
                if (f != null) f.delete();
                f = d.createFile(mime, nm);
            }
            if (f == null) { call.reject("Không tạo được file " + nm); return; }
            byte[] b = Base64.decode(data, Base64.DEFAULT);
            try (OutputStream os = getContext().getContentResolver().openOutputStream(f.getUri(), append ? "wa" : "wt")) {
                if (os == null) throw new Exception("không mở được file để ghi");
                os.write(b);
                os.flush();
            }
            // SAF có thể tự đổi tên nếu trùng (vd. "x (1).mp3") — báo lại tên thật
            JSObject res = new JSObject();
            res.put("name", f.getName());
            call.resolve(res);
        } catch (Exception e) {
            call.reject("Ghi lỗi: " + e.getMessage());
        }
    }

    @PluginMethod
    public void remove(PluginCall call) {
        DocumentFile r = root();
        if (r == null) { call.reject("not_ready"); return; }
        String path = call.getString("path", "");
        DocumentFile d = dir(r, parentOf(path), false);
        DocumentFile f = d == null ? null : d.findFile(nameOf(path));
        boolean ok = f != null && f.delete();
        JSObject res = new JSObject();
        res.put("ok", ok);
        call.resolve(res);
    }

    @PluginMethod
    public void forget(PluginCall call) {
        prefs().edit().remove(KEY).apply();
        dirCache.clear();
        call.resolve();
    }
}
