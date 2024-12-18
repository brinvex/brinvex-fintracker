package com.brinvex.ptfactivity.connector.rvlt.internal.service.parser;

import java.math.BigDecimal;

class RvltParsingUtil {

    public static BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String normalized = s
                .replace("US$", "")
                .replace("$", "")
                .replace(",", "");
        return new BigDecimal(normalized);
    }

    public static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String normalized = s.replace(",", "");
        return new BigDecimal(normalized);
    }


}
