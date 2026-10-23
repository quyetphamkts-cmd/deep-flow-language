package vn.vietan.deepflow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Khi app đang tắt hoặc chạy nền mà tai nghe Bluetooth được kết nối: hiện thông báo
 * "Chạm để tiếp tục nghe". (Android không cho app tự bật màn hình ứng dụng từ nền.)
 */
public class HeadsetReceiver extends BroadcastReceiver {
    static final String CHANNEL = "headset";

    static String connectedType(Intent intent) {
        String a = intent.getAction();
        if (a == null) return null;
        if (AudioManager.ACTION_HEADSET_PLUG.equals(a)) {
            return intent.getIntExtra("state", 0) == 1 ? "wired" : null;
        }
        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(a) || BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(a)) {
            return intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED ? "bluetooth" : null;
        }
        return null;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (connectedType(intent) == null) return;
        if (MainActivity.isForeground) return; // app đang mở: plugin Headset tự xử lý
        if (!ctx.getSharedPreferences(HeadsetPlugin.PREFS, Context.MODE_PRIVATE).getBoolean("autoLaunch", false)) return;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Tai nghe", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Nhắc tiếp tục nghe khi kết nối tai nghe");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra("dfAutoplay", true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, 7, open, flags);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_deepflow)
                .setContentTitle("🎧 Tai nghe đã kết nối")
                .setContentText("Chạm để tiếp tục nghe với Deep Flow")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setTimeoutAfter(5 * 60 * 1000)
                .setContentIntent(pi);
        try { NotificationManagerCompat.from(ctx).notify(71, b.build()); } catch (SecurityException ignored) {}
    }
}
