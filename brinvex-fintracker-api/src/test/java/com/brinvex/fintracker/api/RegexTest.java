package com.brinvex.fintracker.api;

import com.brinvex.fintracker.api.exception.DataException;
import com.brinvex.fintracker.api.util.Regex;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RegexTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegexTest.class);

    @Test
    void ccy() {

        assertFalse(Regex.CCY.matches(null));
        assertFalse(Regex.CCY.matches(null, Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches(null, Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches(""));
        assertFalse(Regex.CCY.matches("", Regex.Tolerance.NULL_OK));
        assertTrue(Regex.CCY.matches("", Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches("", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches(" "));
        assertFalse(Regex.CCY.matches(" ", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches(" ", Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches(" ", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("\n"));
        assertFalse(Regex.CCY.matches("\n", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("\n", Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches("\n", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("\t"));
        assertFalse(Regex.CCY.matches("\t", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("\t", Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches("\t", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("A"));
        assertFalse(Regex.CCY.matches("A", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("A", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches("A", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("AB"));
        assertFalse(Regex.CCY.matches("AB", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("AB", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches("AB", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("AB "));
        assertFalse(Regex.CCY.matches("AB ", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("AB ", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches("AB ", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches(" AB"));
        assertFalse(Regex.CCY.matches(" AB", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches(" AB", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches(" AB", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches(" A1"));
        assertFalse(Regex.CCY.matches(" A1", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches(" A1", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches(" A1", Regex.Tolerance.BLANK_OK));

        assertTrue(Regex.CCY.matches("ABC"));
        assertTrue(Regex.CCY.matches("ABC", Regex.Tolerance.NULL_OK));
        assertTrue(Regex.CCY.matches("ABC", Regex.Tolerance.EMPTY_OK));
        assertTrue(Regex.CCY.matches("ABC", Regex.Tolerance.BLANK_OK));

        assertFalse(Regex.CCY.matches("abc"));
        assertFalse(Regex.CCY.matches("abc", Regex.Tolerance.NULL_OK));
        assertFalse(Regex.CCY.matches("abc", Regex.Tolerance.EMPTY_OK));
        assertFalse(Regex.CCY.matches("abc", Regex.Tolerance.BLANK_OK));

        try {
            Regex.CCY.validate("123");
            fail();
        } catch (DataException de) {
            LOG.debug("Caught expected exception: {}", de.getMessage());
        }
    }
}
