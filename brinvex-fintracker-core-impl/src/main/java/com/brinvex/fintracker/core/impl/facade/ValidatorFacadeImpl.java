package com.brinvex.fintracker.core.impl.facade;

import com.brinvex.fintracker.api.facade.ValidatorFacade;
import com.brinvex.fintracker.api.model.general.Validatable;
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
