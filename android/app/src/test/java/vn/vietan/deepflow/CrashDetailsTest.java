package vn.vietan.deepflow;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class CrashDetailsTest {
    @Test public void preservesRootCauseBeforeLongChromiumMessage() {
        Throwable root = new IllegalStateException("WebView owner disposed");
        root.setStackTrace(new StackTraceElement[]{new StackTraceElement("Owner", "close", "Owner.java", 42)});
        Throwable wrapped = new RuntimeException("Native stack trace:\n" + "#00 chromium ".repeat(300), root);
        List<String> rows = CrashDetails.describe(wrapped);
        assertTrue(rows.get(0).contains("WebView owner disposed"));
        assertTrue(rows.get(1).contains("Owner.java:42"));
        assertTrue(rows.stream().allMatch(row -> row.length() <= 600));
    }
    @Test public void redactsKeysAndTokensBeforeWritingNativeLog() {
        String key = "AIza" + "a".repeat(30);
        String value = CrashDetails.safe("Failed https://example.test?key=" + key + "&access_token=private Authorization: Bearer secret sk-" + "b".repeat(30));
        assertFalse(value.contains(key)); assertFalse(value.contains("private")); assertFalse(value.contains("secret")); assertFalse(value.contains("b".repeat(30)));
    }
    @Test public void capsCauseCyclesAndDepth() {
        RuntimeException a = new RuntimeException("a"), b = new RuntimeException("b");
        a.initCause(b); b.initCause(a);
        assertTrue(CrashDetails.describe(a).size() <= 12);
    }
}
