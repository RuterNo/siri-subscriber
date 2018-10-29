package no.ruter.taas.validation.impl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import no.ruter.taas.validation.NullOrNotBlank;

public class NullOrNotBlankValidator implements ConstraintValidator<NullOrNotBlank, String> {

  public void initialize(NullOrNotBlank constraintAnnotation) {
  }

  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    boolean result = true;
    if (value != null) {
      result = value.trim().length() > 0;
    }
    return result;
  }
}