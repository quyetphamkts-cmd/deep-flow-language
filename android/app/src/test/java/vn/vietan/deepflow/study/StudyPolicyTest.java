package vn.vietan.deepflow.study;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.TimeZone;

public class StudyPolicyTest {
    private long at(String zone,int y,int m,int d,int h,int min){Calendar c=Calendar.getInstance(TimeZone.getTimeZone(zone));c.clear();c.set(y,m-1,d,h,min);return c.getTimeInMillis();}
    @Test public void localNextAndTomorrow(){String z="Asia/Ho_Chi_Minh";long now=at(z,2026,10,30,8,0);assertEquals(at(z,2026,10,30,9,30),StudyPolicy.next("09:30",now,TimeZone.getTimeZone(z)));assertEquals(at(z,2026,10,31,8,0),StudyPolicy.next("08:00",now,TimeZone.getTimeZone(z)));}
    @Test public void daylightSavingDoesNotDriftClock(){String z="America/New_York";long now=at(z,2026,10,31,10,0);assertEquals(at(z,2026,11,1,9,0),StudyPolicy.next("09:00",now,TimeZone.getTimeZone(z)));}
    @Test public void malformedTimesRejected(){for(String t:new String[]{"24:00","12:60","1:02","09:30:00","","-1:10"}){try{StudyPolicy.next(t,0,TimeZone.getDefault());fail(t);}catch(IllegalArgumentException expected){}}}
    @Test public void idsCannotEscapePrivateAudioDirectory(){assertTrue(StudyPolicy.id("abc_123-Z"));for(String s:new String[]{"../x","a/b","a\\b","","x.y"})assertFalse(StudyPolicy.id(s));assertEquals(1,StudyPolicy.count(0));assertEquals(99,StudyPolicy.count(1000));}
}
