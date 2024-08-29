package com.brinvex.fintracker.api.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegexTest.class);

    @Test
    void ccy() {

        assertFalse(Regex.CCY.matches(""));
        assertFalse(Regex.CCY.matches(" "));
        assertFalse(Regex.CCY.matches("\n"));
        assertFalse(Regex.CCY.matches("\t"));
        assertFalse(Regex.CCY.matches("AB "));
        assertFalse(Regex.CCY.matches(" AB"));
        assertFalse(Regex.CCY.matches(" A1"));
        assertTrue(Regex.CCY.matches("ABC"));
        assertFalse(Regex.CCY.matches("abc"));
    }
}
