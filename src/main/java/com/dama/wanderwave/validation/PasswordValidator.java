package com.dama.wanderwave.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.function.IntPredicate;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private int min;
    private int max;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        return  checkCondition(password, Character::isUpperCase) &&
                checkCondition(password, Character::isLowerCase) &&
                checkCondition(password, ch -> "!@#$%^&*()_+-=[]{}|;':\",.<>?/`~".indexOf(ch) >= 0) &&
                isValidLength(password);
    }

    private boolean isValidLength(String password) {
        return password.length() >= min && password.length() <= max;
    }

    private boolean checkCondition(String password, IntPredicate condition) {
        return password.chars().anyMatch(condition);
    }

}