package vn.vietan.deepflow.study;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.TimeZone;

public final class StudyAlarms {
    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("study_schedule60", Context.MODE_PRIVATE); }
    public static JSONObject data(Context c) {
        try { return new JSONObject(prefs(c).getString("data", "{}")); } catch (Exception e) { return new JSONObject(); }
    }
    public static File file(Context c, String id) { if (!StudyPolicy.id(id)) throw new IllegalArgumentException("ID không hợp lệ"); return new File(c.getFilesDir(), "study60/" + id + ".audio"); }
    public static boolean exact(Context c) { AlarmManager m = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE); return m != null && (Build.VERSION.SDK_INT < 31 || m.canScheduleExactAlarms()); }
    private static PendingIntent intent(Context c, int i, long at) {
        return PendingIntent.getBroadcast(c, 6000+i, new Intent(c, StudyAlarmReceiver.class).setAction("vn.vietan.deepflow.STUDY60").putExtra("index", i).putExtra("at", at), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static synchronized void schedule(Context c) {
        AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); if(m==null)return;
        JSONArray rows=data(c).optJSONArray("rows");
        for(int i=0;i<24;i++) {
            m.cancel(intent(c,i,0)); JSONObject row=rows==null?null:rows.optJSONObject(i);
            if(row==null || !row.optBoolean("enabled",true))continue;
            try {
                long at=StudyPolicy.next(row.getString("time"), System.currentTimeMillis(), TimeZone.getDefault());
                PendingIntent pi=intent(c,i,at);
                if(exact(c))m.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
                else m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            } catch(Exception e) { log(c,"Không đặt được lịch; kiểm tra quyền Báo thức và lời nhắc."); }
        }
    }
    public static void log(Context c,String text) { prefs(c).edit().putString("message",text).putLong("messageAt",System.currentTimeMillis()).apply(); }
}
