package vn.vietan.deepflow.study;
import org.junit.Test;
import static org.junit.Assert.*;
public class StudyMemoryTest {
    @Test public void scheduleBeforeNinetyPercent(){long now=1000000000L,due=StudyMemory.due(now,2);assertTrue(due>now);assertTrue(due<now+2*StudyMemory.DAY);assertEquals(.92,StudyMemory.retention(now,2,due),.000001);}
    @Test public void exposureIsWeakerThanConfirmedRecall(){long now=1000000000L;double exposure=StudyMemory.nextInterval(now,2,.25,null,now+StudyMemory.DAY),remembered=StudyMemory.nextInterval(now,2,.25,90.0,now+StudyMemory.DAY);assertEquals(2.4,exposure,.000001);assertTrue(remembered>exposure);assertTrue(StudyMemory.nextInterval(now,2,.25,20.0,now+StudyMemory.DAY)<2);}
    @Test public void clampsPreventRunawayIntervals(){assertEquals(.125,StudyMemory.nextInterval(0,.125,1,0.0,1000),0);assertEquals(365,StudyMemory.nextInterval(0,365,0,100.0,1000),0);assertEquals(1,StudyMemory.retention(2000,1,1000),0);}
}
