package test.com.brinvex.ptfactivity.core;

import com.brinvex.ptfactivity.core.api.general.Regex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexTest {

    @EnabledIfSystemProperty(named = "enableStableTests", matches = "true")
    @Test
    void countryAlpha2Code() {
        assertFalse(Regex.Pattern.COUNTRY_2.matches(""));
        assertFalse(Regex.Pattern.COUNTRY_2.matches(" "));
        assertFalse(Regex.Pattern.COUNTRY_2.matches("\n"));
        assertFalse(Regex.Pattern.COUNTRY_2.matches("\t"));
        assertFalse(Regex.Pattern.COUNTRY_2.matches("US "));
        assertFalse(Regex.Pattern.COUNTRY_2.matches(" US"));
        assertFalse(Regex.Pattern.COUNTRY_2.matches(" U2"));
        assertFalse(Regex.Pattern.COUNTRY_2.matches("us"));
        assertTrue(Regex.Pattern.COUNTRY_2.matches("US"));
    }
}
