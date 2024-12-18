package com.brinvex.ptfactivity.core.internal.facade;

import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.ptfactivity.core.api.general.Validatable;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

public class ValidatorFacadeImpl implements ValidatorFacade {

    private final jakarta.validation.Validator jakartaValidator;

    public ValidatorFacadeImpl(jakarta.validation.Validator jakartaValidator) {
        this.jakartaValidator = jakartaValidator;
    }

    @Override
    public <T extends Validatable> Set<ConstraintViolation<T>> validate(T validatable) {
        return jakartaValidator.validate(validatable);
    }

}
