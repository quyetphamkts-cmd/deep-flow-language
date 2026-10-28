package vn.vietan.deepflow.video;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Video tổng hợp 30 giây, không mạng/API: player service vẫn chạy khi Activity dừng. */
@RunWith(AndroidJUnit4.class)
public class VideoPlaybackTest {
 private void shell(String command) throws Exception {try(android.os.ParcelFileDescriptor p=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(p)){byte[] b=new byte[1024];while(in.read(b)!=-1){}}}
 private void ui(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
 private <T> T read(java.util.concurrent.Callable<T> fn){AtomicReference<T> r=new AtomicReference<>();ui(()->{try{r.set(fn.call());}catch(Exception e){throw new RuntimeException(e);}});return r.get();}
 @Test public void videoContinuesInServiceAndKeepsPositionWhenUiReturns() throws Exception {
  Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
  InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(app.getPackageName(),"android.permission.POST_NOTIFICATIONS");
  byte[] encoded=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("video59.mp4.b64").readAllBytes();
  File media=new File(app.getCacheDir(),"video59.mp4");Files.write(media.toPath(),Base64.decode(encoded,Base64.DEFAULT));String uri=Uri.fromFile(media).toString();
  File sub=new File(app.getFilesDir(),"video59-test.srt");Files.write(sub.toPath(),"1\n00:00:00,000 --> 00:00:29,000\nHello video learner.\n".getBytes(StandardCharsets.UTF_8));
  app.getSharedPreferences("video_lessons",Context.MODE_PRIVATE).edit().putLong("pos:"+VideoPlaybackService.key(uri),0).putString("sub:"+VideoPlaybackService.key(uri),Uri.fromFile(sub).toString()).putString("submime:"+VideoPlaybackService.key(uri),"application/x-subrip").commit();
  AtomicReference<ListenableFuture<MediaController>> ref=new AtomicReference<>();MediaController c=null;
  try(ActivityScenario<VideoActivity> scenario=ActivityScenario.launch(new Intent(app,VideoActivity.class).putExtra("uri",uri).putExtra("title","Fixture video"))){
   ui(()->ref.set(new MediaController.Builder(app,new SessionToken(app,new ComponentName(app,VideoPlaybackService.class))).buildAsync()));
   c=ref.get().get(20,java.util.concurrent.TimeUnit.SECONDS);final MediaController ctl=c;
   long end=SystemClock.elapsedRealtime()+20000;while(!read(()->ctl.getPlaybackState()==Player.STATE_READY&&ctl.isPlaying())&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);
   assertEquals(Player.STATE_READY,(int)read(ctl::getPlaybackState));assertTrue(read(ctl::isPlaying));
   assertTrue(read(()->ctl.getCurrentTracks().containsType(C.TRACK_TYPE_VIDEO)));
   assertTrue(read(()->ctl.getCurrentTracks().containsType(C.TRACK_TYPE_TEXT)));
   ui(()->ctl.seekTo(4000));SystemClock.sleep(500);long before=read(ctl::getCurrentPosition);
   scenario.moveToState(Lifecycle.State.CREATED);SystemClock.sleep(1400);
   assertTrue("Âm thanh tiếp tục khi giao diện dừng",read(ctl::isPlaying));
   assertTrue(read(()->ctl.getTrackSelectionParameters().disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)));
   assertTrue("Không reset vị trí khi ra nền",read(ctl::getCurrentPosition)>before+500);
   scenario.moveToState(Lifecycle.State.RESUMED);SystemClock.sleep(800);
   assertFalse(read(()->ctl.getTrackSelectionParameters().disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)));
   assertTrue("Trở lại màn hình không chạy lại từ đầu",read(ctl::getCurrentPosition)>before+1000);
   shell("input keyevent 223");SystemClock.sleep(1200);assertTrue("Khóa màn hình vẫn nghe",read(ctl::isPlaying));
   shell("input keyevent 224");shell("wm dismiss-keyguard");SystemClock.sleep(500);
   ui(()->{ctl.pause();ctl.stop();ctl.clearMediaItems();});
   assertFalse(read(ctl::isPlaying));assertTrue("Lưu vị trí trước khi kết thúc",app.getSharedPreferences("video_lessons",Context.MODE_PRIVATE).getLong("pos:"+VideoPlaybackService.key(uri),0)>4000);
  }finally{shell("input keyevent 224");shell("wm dismiss-keyguard");final MediaController cleanup=c;if(cleanup!=null)ui(cleanup::release);app.stopService(new Intent(app,VideoPlaybackService.class));}
 }
}
