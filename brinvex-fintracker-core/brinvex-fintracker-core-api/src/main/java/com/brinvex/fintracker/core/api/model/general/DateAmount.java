package com.brinvex.fintracker.core.api.model.general;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DateAmount(LocalDate date, BigDecimal amount) implements Serializable {

}
