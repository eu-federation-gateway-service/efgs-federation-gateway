package eu.interop.federationgateway.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {DiagnosisKeyBatchValidator.class})
public @interface DiagnosisKeyBatchConstraint {

  /**
   * The default key for creating error messages in case the constraint is violated.
   *
   * @return the message of the validation failure
   */
  String message() default "One or more diagnosis keys are not valid";

  /**
   * Allows the specification of validation groups, to which this constraint belongs.
   *
   * @return validation groups
   */
  Class<?>[] groups() default {};

  /**
   * Assigns custom payload objects to a constraint.
   *
   * @return custom payload objects
   */
  Class<? extends Payload>[] payload() default {};
}
