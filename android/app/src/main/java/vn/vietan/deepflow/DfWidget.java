package vn.vietan.deepflow;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

/** Widget màn hình chính: câu đang phát + tên bài + nút lùi / phát-dừng / tiếp (giống màn hình Nghe). */
public class DfWidget extends AppWidgetProvider {
    static final String ACT = "vn.vietan.deepflow.WIDGET";

    static int[] ids(Context ctx) {
        return AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx, DfWidget.class));
    }

    private static PendingIntent cmd(Context ctx, String c, int code) {
        Intent i = new Intent(ctx, DfWidget.class).setAction(ACT).putExtra("cmd", c);
        int f = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(ctx, code, i, f);
    }

    static void refresh(Context ctx) {
        int[] ids = ids(ctx);
        if (ids.length == 0) return;
        SharedPreferences p = ctx.getSharedPreferences(HeadsetPlugin.PREFS, Context.MODE_PRIVATE);
        String line = p.getString("wLine", "");
        String title = p.getString("wTitle", "");
        boolean playing = p.getBoolean("wPlaying", false) && MainActivity.playing;
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_player);
        v.setTextViewText(R.id.w_line, line.isEmpty() ? (title.isEmpty() ? "Deep Flow Language" : title) : line);
        v.setTextViewText(R.id.w_title, title.isEmpty() ? "Chạm để mở app" : title);
        v.setImageViewResource(R.id.w_play, playing ? R.drawable.ic_w_pause : R.drawable.ic_w_play);
        Intent open = new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int f = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        v.setOnClickPendingIntent(R.id.w_body, PendingIntent.getActivity(ctx, 21, open, f));
        v.setOnClickPendingIntent(R.id.w_prev, cmd(ctx, "prev", 22));
        v.setOnClickPendingIntent(R.id.w_play, cmd(ctx, "play", 23));
        v.setOnClickPendingIntent(R.id.w_next, cmd(ctx, "next", 24));
        AppWidgetManager.getInstance(ctx).updateAppWidget(ids, v);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] appWidgetIds) {
        refresh(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (!ACT.equals(intent.getAction())) return;
        String c = intent.getStringExtra("cmd");
        HeadsetPlugin hp = HeadsetPlugin.inst;
        if (hp != null && hp.fireWidget(c)) return;
        // App chưa chạy: mở app (bấm phát thì phát tiếp bài đang nghe dở)
        Intent open = new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if ("play".equals(c)) open.putExtra("dfAutoplay", true);
        try { ctx.startActivity(open); } catch (Exception ignored) {}
    }
}
