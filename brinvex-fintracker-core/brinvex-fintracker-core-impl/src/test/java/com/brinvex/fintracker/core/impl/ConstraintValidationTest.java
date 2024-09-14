package com.brinvex.fintracker.core.impl;

import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.facade.ValidatorFacade;
import com.brinvex.fintracker.core.api.model.domain.FinTransaction;
import com.brinvex.fintracker.core.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.core.api.model.domain.constraints.fintransaction.FinTransactionConstraints;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("LoggingSimilarMessage")
public class ConstraintValidationTest {

    private final ValidatorFacade validator = new FinTrackerSharedContext(new FinTrackerConfig()).validator();

    @Test
    void deposit() {
        FinTransaction.FinTransactionBuilder finTranBldr = FinTransaction.builder();
        finTranBldr.type(FinTransactionType.DEPOSIT);

        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(8, violations.size(), "%s, %s".formatted(violations, finTran));
        }
        finTranBldr.date(LocalDate.now());
        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(7, violations.size(), "%s, %s".formatted(violations, finTran));
        }
        finTranBldr.netValue(BigDecimal.TEN);
        finTranBldr.grossValue(BigDecimal.TEN);
        finTranBldr.ccy("EUR");
        finTranBldr.fee(BigDecimal.ZERO);
        finTranBldr.qty(BigDecimal.ZERO);
        finTranBldr.settleDate(LocalDate.now());
        finTranBldr.tax(BigDecimal.ZERO);
        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(0, violations.size(), "%s, %s".formatted(violations, finTran));
        }

    }

    @SuppressWarnings("LoggingSimilarMessage")
    @Test
    void grossValueCalcDeviation() {
        BigDecimal fee = new BigDecimal("-1.50");

        FinTransaction.FinTransactionBuilder finTranBldr = FinTransaction.builder();
        finTranBldr.date(LocalDate.now());
        finTranBldr.netValue(BigDecimal.TEN);
        finTranBldr.grossValue(BigDecimal.TEN);
        finTranBldr.ccy("EUR");
        finTranBldr.fee(fee);
        finTranBldr.type(FinTransactionType.DEPOSIT);
        finTranBldr.qty(BigDecimal.ZERO);
        finTranBldr.settleDate(LocalDate.now());
        finTranBldr.tax(BigDecimal.ZERO);
        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(1, violations.size(), "%s, %s".formatted(violations, finTran));
        }
        finTranBldr.netValue(BigDecimal.TEN.add(fee));
        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(0, violations.size(), "%s, %s".formatted(violations, finTran));
        }
    }
}
