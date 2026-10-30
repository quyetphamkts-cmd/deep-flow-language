package vn.vietan.deepflow.study;

import java.util.Calendar;
import java.util.TimeZone;

/** Lịch giờ địa phương: tính lại mỗi lần, không cộng cố định 24 giờ qua DST. */
public final class StudyPolicy {
    private StudyPolicy() {}
    public static long next(String time, long now, TimeZone zone) {
        if (time == null || !time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) throw new IllegalArgumentException("Giờ không hợp lệ");
        Calendar c = Calendar.getInstance(zone); c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
        c.set(Calendar.MINUTE, Integer.parseInt(time.substring(3))); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }
    public static boolean id(String id) { return id != null && id.matches("[a-zA-Z0-9_-]{1,80}"); }
    public static int count(int n) { return Math.max(1, Math.min(99, n)); }
}
