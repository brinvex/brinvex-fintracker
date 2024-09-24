package com.brinvex.fintracker.test.support;

import org.junit.jupiter.api.Assertions;

public class AssertionUtil {

    public static void assertEqualsWithMultilineMsg(String expected, String actual) {
        Assertions.assertEquals(expected, actual, () -> "\nExpected:\n%s\nActual:\n%s\n".formatted(expected, actual));
    }

}
