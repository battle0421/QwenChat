package org.qwen.aiqwen.util;

import java.util.regex.Pattern;

public class NumberUtil {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)");

    public static Integer extract(String input) {
        var m = PATTERN.matcher(input);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }
}