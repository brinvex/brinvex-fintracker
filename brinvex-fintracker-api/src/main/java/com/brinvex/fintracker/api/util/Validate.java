package com.brinvex.fintracker.api.util;

import com.brinvex.fintracker.api.exception.DataException;

import java.util.regex.Pattern;

public class Validate {

    public static void notNull(Object o) {
        if (o == null) {
            throw new DataException("The validated object is null");
        }
    }

    public static void notBlank(String s) {
        if (s != null && s.isBlank()) {
            throw new DataException("The validated string is blank: '%s'".formatted(s));
        }
    }

    public static void notNullNotBlank(String s) {
        if (s == null) {
            throw new DataException("The validated object is null");
        }
        if (s.isBlank()) {
            throw new DataException("The validated string is blank: '%s'".formatted(s));
        }
    }

    public static void matches(String str, Regex regex) {
        regex.validate(str);
    }

    public static void matches(String str, Regex regex, Regex.Tolerance tolerance) {
        regex.validate(str, tolerance);
    }

    public static void matches(String str, Pattern regex) {
        Regex.validate(regex, str);
    }

    public static void matches(String str, Pattern regex, Regex.Tolerance tolerance) {
        Regex.validate(regex, str, tolerance);
    }


}
