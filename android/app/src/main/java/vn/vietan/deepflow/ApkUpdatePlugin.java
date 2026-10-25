package vn.vietan.deepflow;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cập nhật APK ngay trong app: tải bản mới vào bộ nhớ riêng của app → mở trình cài đặt của Android.
 * Cài xong (lần mở app kế tiếp) → tự xoá file APK đã tải, không để lại rác trong máy.
 */
@CapacitorPlugin(name = "ApkUpd")
public class ApkUpdatePlugin extends Plugin {
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private volatile boolean busy = false, cancel = false;

    private File dir() { File d = new File(getContext().getCacheDir(), "apk"); d.mkdirs(); return d; }
    private File apkFile(int code) { return new File(dir(), "DeepFlow-" + code + ".apk"); }

    private long myCode() {
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) { return 0; }
    }

    /** Xoá mọi file APK đã tải mà bản đó (hoặc mới hơn) đã được cài, và file tải dở */
    private int cleanup() {
        int n = 0; long me = myCode();
        File[] fs = dir().listFiles();
        if (fs == null) return 0;
        for (File f : fs) {
            String nm = f.getName();
            boolean del;
            try { long c = Long.parseLong(nm.replaceAll("\\D", "")); del = c <= me; } catch (Exception e) { del = !busy; }
            if (del && f.delete()) n++;
        }
        return n;
    }

    @Override
    public void load() { ex.execute(this::cleanup); }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        r.put("versionCode", myCode());
        r.put("busy", busy);
        r.put("canInstall", Build.VERSION.SDK_INT < 26 || getContext().getPackageManager().canRequestPackageInstalls());
        Integer code = call.getInt("versionCode");
        if (code != null) { File f = apkFile(code); r.put("ready", f.exists() && f.length() > 0); }
        call.resolve(r);
    }

    @PluginMethod
    public void cleanup(PluginCall call) { ex.execute(() -> { JSObject r = new JSObject(); r.put("deleted", cleanup()); call.resolve(r); }); }

    @PluginMethod
    public void cancel(PluginCall call) { cancel = true; call.resolve(); }

    @PluginMethod
    public void download(PluginCall call) {
        String url = call.getString("url");
        Integer code = call.getInt("versionCode");
        if (url == null || code == null) { call.reject("thiếu url / versionCode"); return; }
        if (busy) { call.reject("đang tải"); return; }
        busy = true; cancel = false;
        new Thread(() -> {
            try {
                File dst = apkFile(code);
                if (!(dst.exists() && dst.length() > 0 && validApk(dst))) {
                    File part = new File(dir(), "DeepFlow-" + code + ".apk.part");
                    long have = part.exists() ? part.length() : 0;
                    URL u = new URL(url); int redirects = 0; HttpURLConnection c;
                    while (true) {
                        c = (HttpURLConnection) u.openConnection();
                        c.setInstanceFollowRedirects(false); c.setConnectTimeout(20000); c.setReadTimeout(60000);
                        if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
                        int rc = c.getResponseCode();
                        if (rc >= 300 && rc < 400) { String loc = c.getHeaderField("Location"); c.disconnect(); u = new URL(u, loc); if (++redirects > 8) throw new IOException("chuyển hướng quá nhiều lần"); continue; }
                        if (rc == 416) { c.disconnect(); c = null; break; }
                        if (rc != 200 && rc != 206) throw new IOException("lỗi tải HTTP " + rc);
                        if (rc == 200) have = 0;
                        break;
                    }
                    if (c != null) {
                        long len = c.getContentLengthLong(); long total = have + Math.max(0, len);
                        try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part, have > 0)) {
                            byte[] buf = new byte[1 << 16]; long got = have, last = 0; int n;
                            while ((n = in.read(buf)) >= 0) {
                                if (cancel) throw new IOException("cancelled");
                                out.write(buf, 0, n); got += n;
                                long now = System.currentTimeMillis();
                                if (now - last > 400) { last = now; JSObject ev = new JSObject(); ev.put("loaded", got); ev.put("total", total); notifyListeners("progress", ev); }
                            }
                        }
                    }
                    if (dst.exists()) dst.delete();
                    if (!part.renameTo(dst)) throw new IOException("không lưu được file APK");
                    if (!validApk(dst)) { dst.delete(); throw new IOException("file APK tải về bị lỗi — thử lại"); }
                }
                JSObject r = new JSObject(); r.put("size", dst.length()); call.resolve(r);
            } catch (Exception e) {
                call.reject(cancel ? "cancelled" : (e.getMessage() != null ? e.getMessage() : e.toString()));
            } finally { busy = false; }
        }).start();
    }

    private boolean validApk(File f) {
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageArchiveInfo(f.getPath(), 0);
            return pi != null && getContext().getPackageName().equals(pi.packageName);
        } catch (Exception e) { return false; }
    }

    /** Mở trình cài đặt. Lần đầu Android cần cho phép “Cài ứng dụng không rõ nguồn gốc” cho Deep Flow → mở trang đó, quay lại thì gọi lại install */
    @PluginMethod
    public void install(PluginCall call) {
        Integer code = call.getInt("versionCode");
        if (code == null) { call.reject("thiếu versionCode"); return; }
        File f = apkFile(code);
        if (!f.exists()) { call.reject("chưa tải APK"); return; }
        JSObject r = new JSObject();
        if (Build.VERSION.SDK_INT >= 26 && !getContext().getPackageManager().canRequestPackageInstalls()) {
            Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getContext().getPackageName()));
            s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(s);
            r.put("needPermission", true); call.resolve(r); return;
        }
        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        r.put("started", true); call.resolve(r);
    }
}
