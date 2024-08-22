package com.brinvex.fintracker.common.impl;

import com.brinvex.fintracker.api.exception.DataException;
import com.brinvex.fintracker.api.util.Regex;

import java.util.regex.Pattern;

@SuppressWarnings("SameParameterValue")
public class Validate {


    public static void notNull(Object o) {
        if (o == null) {
            throw new DataException("The validated object is null");
        }
    }

    public static void notBlank(String s) {
        if (s != null && s.isBlank()) {
            throw new DataException("The validated inputing is blank: '%s'".formatted(s));
        }
    }

    public static void notNullNotBlank(String s) {
        if (s == null) {
            throw new DataException("The validated object is null");
        }
        if (s.isBlank()) {
            throw new DataException("The validated inputing is blank: '%s'".formatted(s));
        }
    }

    public static void matches(String input, Pattern pattern) {
        matches(input, pattern, (Regex.Tolerance) null);
    }

    public static void matches(String input, Pattern pattern, Regex.Tolerance tolerance) {
        boolean matches = Regex.matches(pattern, input, tolerance);
        if (!matches) {
            String inputPart = input == null ? "null" : "'%s'".formatted(input);
            throw new DataException("Invalid value: %s, pattern='%s', tolerance=%s".formatted(inputPart, pattern, tolerance));
        }
    }

    public static void matches(String input, Regex regex) {
        matches(input, regex, null);
    }

    public static void matches(String input, Regex regex, Regex.Tolerance tolerance) {
        boolean matches = regex.matches(input, tolerance);
        if (!matches) {
            String inputPart = input == null ? "null" : "'%s'".formatted(input);
            throw new DataException("Invalid value: %s, pattern='%s', tolerance=%s, %s$%s"
                    .formatted(inputPart, regex.pattern(), tolerance, regex.getClass().getSimpleName(), regex));
        }
    }

}
