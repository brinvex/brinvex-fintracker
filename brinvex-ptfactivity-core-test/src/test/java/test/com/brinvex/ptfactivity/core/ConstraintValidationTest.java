package test.com.brinvex.ptfactivity.core;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.domain.constraints.fintransaction.FinTransactionConstraints;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("LoggingSimilarMessage")
public class ConstraintValidationTest extends CoreBaseTest {

    private final ValidatorFacade validator = testCtx.validator();

    @Test
    void deposit() {
        FinTransaction.FinTransactionBuilder finTranBldr = FinTransaction.builder();
        finTranBldr.type(FinTransactionType.DEPOSIT);

        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(7, violations.size(), "%s, %s".formatted(violations, finTran));
        }
        finTranBldr.date(LocalDate.now());
        {
            FinTransaction finTran = finTranBldr.build();
            Set<ConstraintViolation<FinTransactionConstraints>> violations = validator.validate(FinTransactionConstraints.of(finTran));
            assertEquals(6, violations.size(), "%s, %s".formatted(violations, finTran));
        }
        finTranBldr.netValue(BigDecimal.TEN);
        finTranBldr.grossValue(BigDecimal.TEN);
        finTranBldr.ccy(Currency.EUR);
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
        finTranBldr.ccy(Currency.EUR);
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
