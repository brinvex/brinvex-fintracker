package com.brinvex.fintracker.api.util;

import java.util.regex.Pattern;

public enum Regex {

    CCY(Pattern.compile("[A-Z]{3}")),

    /**
     * Pattern for validating Alpha-2 country codes.
     * <p>
     * This pattern checks for a two-letter uppercase code, as defined in ISO 3166-1 alpha-2.
     * Alpha-2 codes are used to represent countries and are always two uppercase letters
     * (e.g., "US" for the United States, "CZ" for Czech Republic).
     * </p>
     */
    COUNTRY_2(Pattern.compile("[A-Z]{2}"));

    private final Pattern pattern;

    Regex(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean matches(String str) {
        return pattern().matcher(str).matches();
    }


}
