package com.brinvex.fintracker.api.util;

import java.util.regex.Pattern;

public enum Regex {

    CCY(Pattern.compile("[A-Z]{3}"));

    public enum Tolerance {
        NULL_OK,
        EMPTY_OK,
        BLANK_OK
    }

    private final Pattern pattern;

    Regex(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean matches(String str) {
        return Regex.matches(pattern, str);
    }

    public boolean matches(String str, Tolerance tolerance) {
        return Regex.matches(pattern, str, tolerance);
    }

    public static boolean matches(Regex regex, String str) {
        return matches(regex.pattern(), str, null);
    }

    public static boolean matches(Regex regex, String str, Tolerance tolerance) {
        return matches(regex.pattern(), str, tolerance);
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


}
