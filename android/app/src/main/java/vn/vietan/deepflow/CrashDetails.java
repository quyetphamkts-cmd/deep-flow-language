package vn.vietan.deepflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Tách cause/Java frames khỏi thông báo Chromium dài để không mất nguyên nhân gốc. */
final class CrashDetails {
    static String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("(?i)(authorization|x-api-key)\\s*[:=]\\s*(?:Bearer\\s+)?[^\\s,;]+", "$1=[ẩn]")
            .replaceAll("(?i)([?&](?:key|token|access_token)=)[^&\\s]+", "$1[ẩn]")
            .replaceAll("AIza[\\w-]{20,}|sk-(?:ant-)?[\\w-]{16,}|ya29\\.[\\w.-]+", "[key]");
    }

    static List<String> describe(Throwable error) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error != null && chain.size() < 6 && seen.add(error)) {
            chain.add(error);
            error = error.getCause();
        }
        List<String> result = new ArrayList<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            Throwable e = chain.get(i);
            String msg = safe(e.getMessage()).replace('\n', ' ').replace('\r', ' ');
            if (msg.length() > 220) msg = msg.substring(0, 220) + "…";
            result.add("Lỗi Java " + (i == chain.size() - 1 ? "gốc" : "bọc") + ": " + e.getClass().getName() + ": " + msg);
            StackTraceElement[] stack = e.getStackTrace();
            for (int j = 0; j < Math.min(5, stack.length); j++) result.add("Lỗi Java vị trí: " + safe(stack[j].toString()));
        }
        return result;
    }
}
