package vn.vietan.deepflow.video;
import org.junit.Test;
import static org.junit.Assert.*;
public class VideoVocabularyTest {
 @Test public void countsCueTextNotTimestampsHeadersOrNotes(){VideoVocabulary s=VideoVocabulary.parse("WEBVTT\n\nNOTE ignore these words\nnot spoken\n\ncue-identifier\n00:00:01.000 --> 00:00:03.000 align:center\n<v David>Hello <b>world</b>. Hello!\n\n00:00:04.000 --> 00:00:06.000\nDon't go.\n");assertEquals(5,s.total);assertEquals(4,s.counts.size());assertEquals(Integer.valueOf(2),s.counts.get("hello"));assertFalse(s.counts.containsKey("David"));}
 @Test public void countsBilingualLiterallyWithoutPretendingEnglishOnlyOrFamilies(){VideoVocabulary s=VideoVocabulary.parse("1\n00:00:00,000 --> 00:00:03,000\nGo goes.\nĐi thôi.\n");assertEquals(4,s.total);assertEquals(4,s.counts.size());assertTrue(s.counts.containsKey("đi"));}
}
