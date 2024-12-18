package com.brinvex.ptfactivity.core.api.facade;

import com.brinvex.ptfactivity.core.api.general.Validatable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ValidatorFacade {

    <VALIDATABLE extends Validatable> Set<ConstraintViolation<VALIDATABLE>> validate(VALIDATABLE validatable);

    default <VALIDATABLE extends Validatable> void validateAndThrow(VALIDATABLE validatable) {
        validateAndThrow(validatable, VALIDATABLE::toString);
    }

    default <VALIDATABLE extends Validatable> void validateAndThrow(VALIDATABLE validatable, Function<VALIDATABLE, String> msgPrefix) {
        Set<ConstraintViolation<VALIDATABLE>> violations = validate(validatable);
        if (!violations.isEmpty()) {
            ConstraintViolationException msgHelperException = new ConstraintViolationException(violations);
            String msg = "%s - %s".formatted(msgPrefix.apply(validatable), msgHelperException.getMessage());
            throw new ConstraintViolationException(msg, violations);
        }
    }

    default <VALIDATABLE extends Validatable> void validateAndThrow(Stream<VALIDATABLE> validatables) {
        validatables.forEach(this::validateAndThrow);
    }

    default <VALIDATABLE extends Validatable> void validateAndThrow(Collection<VALIDATABLE> validatables) {
        validateAndThrow(validatables.stream());
    }

    default <BUSINESS_OBJECT, VALIDATABLE extends Validatable> void validateAndThrow(
            Collection<BUSINESS_OBJECT> businessObjects, Function<BUSINESS_OBJECT, VALIDATABLE> validationWrapper
    ) {
        validateAndThrow(businessObjects.stream().map(validationWrapper));
    }

}
