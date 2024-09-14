package com.brinvex.fintracker.core.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexTest {

    @Test
    void ccy() {
        assertFalse(Regex.Pattern.CCY.matches(""));
        assertFalse(Regex.Pattern.CCY.matches(" "));
        assertFalse(Regex.Pattern.CCY.matches("\n"));
        assertFalse(Regex.Pattern.CCY.matches("\t"));
        assertFalse(Regex.Pattern.CCY.matches("AB "));
        assertFalse(Regex.Pattern.CCY.matches(" AB"));
        assertFalse(Regex.Pattern.CCY.matches(" A1"));
        assertTrue(Regex.Pattern.CCY.matches("ABC"));
        assertFalse(Regex.Pattern.CCY.matches("abc"));
    }
}
