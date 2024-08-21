package com.brinvex.fintracker.api.util;

import com.brinvex.fintracker.api.exception.DataException;

import java.util.regex.Pattern;

public enum Regex {

    CCY(Pattern.compile("[A-Z]{3}"));

    public enum Tolerance {
        NULL_OK,
        EMPTY_OK,
        BLANK_OK
    }

    public static boolean matches(Pattern pattern, String str) {
        return matches(pattern, str, null);
    }

    public static boolean matches(Pattern pattern, String str, Tolerance tolerance) {
        if (str == null) {
            return tolerance == Tolerance.NULL_OK || tolerance == Tolerance.BLANK_OK;
        }
        if (str.isEmpty()) {
            return tolerance == Tolerance.EMPTY_OK || tolerance == Tolerance.BLANK_OK;
        }
        if (str.isBlank()) {
            return tolerance == Tolerance.BLANK_OK;
        }
        return pattern.matcher(str).matches();
    }

    public static void validate(Pattern pattern, String str) {
        validate(pattern, str, null, null);
    }

    public static void validate(Pattern pattern, String str, String patternDesc) {
        validate(pattern, str, null, patternDesc);
    }

    public static void validate(Pattern pattern, String str, Tolerance tolerance) {
        validate(pattern, str, tolerance, null);
    }

    public static void validate(Pattern pattern, String str, Tolerance tolerance, String msg) {
        if (!matches(pattern, str, tolerance)) {
            String strPart = str == null ? "null" : "'%s'".formatted(str);
            String patternDescPart = msg == null ? "" : ", %s".formatted(msg);
            throw new DataException("Invalid value: %s, pattern='%s', tolerance=%s%s".formatted(strPart, pattern, tolerance, patternDescPart));
        }
    }

    private final Pattern pattern;

    private final String fullName = this.getClass().getName() + "#" + name();

    Regex(Pattern pattern) {
        this.pattern = pattern;
    }

    public boolean matches(String str) {
        return Regex.matches(pattern, str);
    }

    public boolean matches(String str, Regex.Tolerance tolerance) {
        return Regex.matches(pattern, str, tolerance);
    }

    public void validate(String str) {
        validate(str, null);
    }

    public void validate(String str, Regex.Tolerance tolerance) {
        Regex.validate(pattern, str, tolerance, fullName);
    }
}
