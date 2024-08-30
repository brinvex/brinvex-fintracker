package com.brinvex.fintracker.api.facade;

import com.brinvex.fintracker.api.model.general.Validatable;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

public interface ValidatorFacade {

    <VALIDATABLE extends Validatable> Set<ConstraintViolation<VALIDATABLE>> validate(VALIDATABLE validatable);
}
