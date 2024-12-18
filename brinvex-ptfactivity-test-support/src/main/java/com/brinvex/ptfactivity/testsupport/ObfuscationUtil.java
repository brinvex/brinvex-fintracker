package com.brinvex.ptfactivity.testsupport;

import com.brinvex.java.Num;

import java.math.BigDecimal;
import java.util.function.Function;

public class ObfuscationUtil {

    public static final Function<BigDecimal, BigDecimal> remainder10 = d -> d.remainder(BigDecimal.TEN);

    public static final Function<BigDecimal, BigDecimal> remainder100 = d -> d.remainder(Num._100);

    public static final Function<BigDecimal, BigDecimal> remainder1000 = d -> d.remainder(Num._1000);
}
