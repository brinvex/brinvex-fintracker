package com.brinvex.ptfactivity.core.api.general;

public class Regex {

    /**
     * Pattern for validating Alpha-2 country codes.
     * <p>
     * This pattern checks for a two-letter uppercase code, as defined in ISO 3166-1 alpha-2.
     * Alpha-2 codes are used to represent countries and are always two uppercase letters
     * (e.g., "US" for the United States, "CZ" for Czech Republic).
     * </p>
     */
    public static final String COUNTRY_2 = "[A-Z]{2}";

    public enum Pattern {

        COUNTRY_2(java.util.regex.Pattern.compile(Regex.COUNTRY_2));

        private final java.util.regex.Pattern pattern;

        Pattern(java.util.regex.Pattern pattern) {
            this.pattern = pattern;
        }

        public java.util.regex.Pattern pattern() {
            return pattern;
        }

        public boolean matches(String str) {
            return pattern().matcher(str).matches();
        }
    }
}
