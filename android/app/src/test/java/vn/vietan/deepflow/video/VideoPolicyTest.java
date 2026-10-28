package vn.vietan.deepflow.video;
import org.junit.Test;
import static org.junit.Assert.*;
public class VideoPolicyTest {
 @Test public void acceptsDirectHttpsKeepingSignedQuery(){assertEquals("https://media.example/test.MP4?token=a%2Bb",VideoPolicy.remote("https://media.example/test.MP4?token=a%2Bb"));assertTrue(VideoPolicy.remote("https://media.example/a.m3u8").endsWith("m3u8"));}
 @Test public void rejectsPagesCredentialsAndUnsafeSchemes(){for(String s:new String[]{"http://media.example/a.mp4","https://u:password@media.example/a.mp4","https://youtube.com/a.mp4","https://player.vimeo.com/a.mp4","https://example.com/page","file:///a.mp4","javascript:alert(1)","https://example.com/a.mp4#token"}){try{VideoPolicy.remote(s);fail("accepted unsafe source");}catch(IllegalArgumentException expected){assertFalse(expected.getMessage().contains("password"));}}}
 @Test public void errorsDistinguishRatePermissionAndCodec(){assertTrue(VideoPolicy.error(429).contains("429"));assertTrue(VideoPolicy.error(403).contains("hết hạn"));assertTrue(VideoPolicy.error(404).contains("404"));assertTrue(VideoPolicy.error(4001).contains("giải mã"));}
}
