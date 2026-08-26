package com.pmrgsolution.shared.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);

    private DateTimeUtil() {}

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, DEFAULT_FORMATTER);
    }

    public static LocalDateTime parse(String text, String pattern) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }
}
