package vn.vietan.deepflow.study;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

@RunWith(AndroidJUnit4.class)
public class StudyAlarmTest {
    @Test public void alarmWithoutHeadphonesSkipsAndDoesNotDuplicate()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();assumeFalse(StudyAlarmReceiver.headphones(c));
        long now=System.currentTimeMillis();JSONObject row=new JSONObject().put("time","23:59").put("enabled",true).put("kind","main");
        StudyAlarms.prefs(c).edit().clear().putString("data",new JSONObject().put("rows",new JSONArray().put(row)).toString()).commit();
        try{
            Intent alarm=new Intent(c,StudyAlarmReceiver.class).setAction("vn.vietan.deepflow.STUDY60").putExtra("index",0).putExtra("at",now);
            new StudyAlarmReceiver().onReceive(c,alarm);
            assertNull(StudyPlaybackService.instance);assertTrue(StudyAlarms.prefs(c).getString("message","").contains("tai nghe"));
            long messageAt=StudyAlarms.prefs(c).getLong("messageAt",0);new StudyAlarmReceiver().onReceive(c,alarm);
            assertEquals(messageAt,StudyAlarms.prefs(c).getLong("messageAt",0));
        }finally{StudyAlarms.prefs(c).edit().clear().commit();StudyAlarms.schedule(c);}
    }
    @Test public void lateAlarmDoesNotStartUnexpectedAudio()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();JSONObject row=new JSONObject().put("time","23:59").put("enabled",true);
        StudyAlarms.prefs(c).edit().clear().putString("data",new JSONObject().put("rows",new JSONArray().put(row)).toString()).commit();
        try{
            new StudyAlarmReceiver().onReceive(c,new Intent(c,StudyAlarmReceiver.class).setAction("vn.vietan.deepflow.STUDY60").putExtra("index",0).putExtra("at",System.currentTimeMillis()-11*60*1000));
            assertTrue(StudyAlarms.prefs(c).getString("message","").contains("quá muộn"));assertNull(StudyPlaybackService.instance);
        }finally{StudyAlarms.prefs(c).edit().clear().commit();StudyAlarms.schedule(c);}
    }
}
