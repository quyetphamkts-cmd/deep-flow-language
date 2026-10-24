package vn.vietan.deepflow;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Nhật ký của lõi Android: vòng đời màn hình app, thiếu bộ nhớ, WebView bị đóng,
 * và lý do Android ghi lại mỗi lần tiến trình app bị kết thúc (Android 11+).
 * App web đọc qua Headset.getNativeDiag() để đưa vào tab Nhật ký.
 */
public class NativeDiag {
    static final String PREFS = "dfdiag";
    private static final int MAX = 60;

    static synchronized void log(Context ctx, String msg) {
        if (ctx == null) return;
        try {
            SharedPreferences p = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray a = new JSONArray(p.getString("log", "[]"));
            JSONObject e = new JSONObject();
            e.put("t", System.currentTimeMillis());
            e.put("m", msg);
            a.put(e);
            JSONArray b = new JSONArray();
            for (int i = Math.max(0, a.length() - MAX); i < a.length(); i++) b.put(a.get(i));
            p.edit().putString("log", b.toString()).apply();
        } catch (Exception ignored) {}
    }

    static JSONArray readLog(Context ctx) {
        try {
            return new JSONArray(ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("log", "[]"));
        } catch (Exception e) { return new JSONArray(); }
    }

    static String reasonName(int r) {
        switch (r) {
            case 1: return "EXIT_SELF (app tự thoát)";
            case 2: return "SIGNALED (bị hệ thống kết thúc)";
            case 3: return "LOW_MEMORY (Android thiếu RAM nên đóng app)";
            case 4: return "CRASH (lỗi Java)";
            case 5: return "CRASH_NATIVE (lỗi mã máy)";
            case 6: return "ANR (app bị treo, không phản hồi)";
            case 7: return "INITIALIZATION_FAILURE (khởi động lỗi)";
            case 8: return "PERMISSION_CHANGE (quyền của app bị đổi)";
            case 9: return "EXCESSIVE_RESOURCE_USAGE (dùng quá nhiều pin/CPU ở nền)";
            case 10: return "USER_REQUESTED (người dùng buộc dừng / xoá dữ liệu / vuốt tắt)";
            case 11: return "USER_STOPPED (người dùng dừng app)";
            case 12: return "DEPENDENCY_DIED (thành phần phụ thuộc bị đóng)";
            case 13: return "OTHER (lý do khác của hệ thống)";
            case 14: return "FREEZER (bị đóng băng ở nền)";
            case 15: return "PACKAGE_STATE_CHANGE (app bị tắt / cập nhật)";
            case 16: return "PACKAGE_UPDATED (app vừa được cập nhật)";
            default: return "UNKNOWN (" + r + ")";
        }
    }

    static String importanceName(int imp) {
        if (imp <= 100) return "đang mở trên màn hình";
        if (imp <= 125) return "dịch vụ tiền cảnh (đang phát nhạc)";
        if (imp <= 200) return "đang hiện";
        if (imp <= 230) return "cảm nhận được";
        if (imp <= 300) return "dịch vụ nền";
        if (imp <= 325) return "đang ngủ";
        if (imp <= 400) return "ở nền (đã lưu bộ nhớ đệm)";
        return "không còn chạy";
    }

    /** Các lần tiến trình app bị kết thúc gần đây (Android 11+), mới nhất trước */
    static JSONArray exits(Context ctx) {
        JSONArray out = new JSONArray();
        if (Build.VERSION.SDK_INT < 30) return out;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ApplicationExitInfo> l = am.getHistoricalProcessExitReasons(ctx.getPackageName(), 0, 12);
            for (ApplicationExitInfo x : l) {
                JSONObject o = new JSONObject();
                o.put("t", x.getTimestamp());
                o.put("reason", reasonName(x.getReason()));
                o.put("code", x.getReason());
                o.put("status", x.getStatus());
                o.put("imp", importanceName(x.getImportance()));
                o.put("pss", x.getPss() / 1024);
                o.put("rss", x.getRss() / 1024);
                o.put("desc", x.getDescription() == null ? "" : x.getDescription());
                o.put("proc", x.getProcessName() == null ? "" : x.getProcessName());
                out.put(o);
            }
        } catch (Exception e) {
            try { JSONObject o = new JSONObject(); o.put("err", e.toString()); out.put(o); } catch (Exception ignored) {}
        }
        return out;
    }

    static String trimName(int level) {
        switch (level) {
            case 5: return "RUNNING_MODERATE (máy bắt đầu thiếu RAM)";
            case 10: return "RUNNING_LOW (máy thiếu RAM)";
            case 15: return "RUNNING_CRITICAL (máy rất thiếu RAM)";
            case 20: return "UI_HIDDEN (app vào nền)";
            case 40: return "BACKGROUND (app ở nền, có thể bị dọn)";
            case 60: return "MODERATE (app ở nền, sắp bị dọn)";
            case 80: return "COMPLETE (app ở nền, sẽ bị dọn đầu tiên)";
            default: return String.valueOf(level);
        }
    }
}
