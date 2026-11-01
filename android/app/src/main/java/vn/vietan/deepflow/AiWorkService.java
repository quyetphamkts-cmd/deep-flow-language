package vn.vietan.deepflow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Giữ app chạy tiếp khi AI đang tạo phụ đề mà người dùng dừng phát, tắt màn hình hoặc chuyển sang app khác:
 * dịch vụ chạy nổi (thông báo "Đang tạo phụ đề…") + khoá CPU, để JavaScript trong WebView làm xong việc.
 * Xong việc thì tự tắt và hiện thông báo "Đã tạo xong phụ đề".
 */
public class AiWorkService extends Service {
    static final String CH_WORK = "aiwork", CH_DONE = "aidone";
    static final int ID_WORK = 1201, ID_DONE = 1202;
    static volatile boolean running = false;
    private PowerManager.WakeLock wl;

    static void start(Context c, String title) {
        try {
            Intent i = new Intent(c, AiWorkService.class).putExtra("title", title == null ? "" : title);
            ContextCompat.startForegroundService(c, i);
        } catch (Exception e) {
            // Android 12+ không cho mở dịch vụ nổi khi app đã ở nền: vẫn chạy tiếp được lúc WebView còn sống
            NativeDiag.log(c, "Không mở được dịch vụ giữ AI chạy nền: " + e.getClass().getSimpleName());
        }
    }

    static void stop(Context c, String doneText) {
        try { c.stopService(new Intent(c, AiWorkService.class)); } catch (Exception ignored) {}
        if (doneText != null && !doneText.isEmpty() && !MainActivity.isForeground) {
            try {
                NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
                channels(nm);
                PendingIntent open = PendingIntent.getActivity(c, 1202, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                nm.notify(ID_DONE, new NotificationCompat.Builder(c, CH_DONE).setSmallIcon(R.drawable.ic_stat_deepflow)
                    .setContentTitle("Deep Flow").setContentText(doneText).setAutoCancel(true).setContentIntent(open).build());
            } catch (Exception ignored) {}
        }
    }

    static void channels(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH_WORK, "AI đang làm phụ đề", NotificationManager.IMPORTANCE_LOW));
            nm.createNotificationChannel(new NotificationChannel(CH_DONE, "AI đã làm xong", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent != null ? intent.getStringExtra("title") : "";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        channels(nm);
        PendingIntent open = PendingIntent.getActivity(this, 1201, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, CH_WORK).setSmallIcon(R.drawable.ic_stat_deepflow)
            .setContentTitle("Đang tạo phụ đề" + (title == null || title.isEmpty() ? "" : " · " + title))
            .setContentText("Bạn có thể dừng nghe, tắt màn hình — xong sẽ báo").setOngoing(true).setProgress(0, 0, true)
            .setContentIntent(open).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID_WORK, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(ID_WORK, n);
        } catch (Exception e) { NativeDiag.log(this, "Dịch vụ giữ AI chạy nền lỗi: " + e.getClass().getSimpleName()); stopSelf(); return START_NOT_STICKY; }
        running = true;
        try {
            if (wl == null) { wl = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "deepflow:aiwork"); wl.setReferenceCounted(false); }
            wl.acquire(3 * 60 * 60 * 1000L); // tối đa 3 giờ
        } catch (Exception ignored) {}
        MainActivity.keepAlive();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
