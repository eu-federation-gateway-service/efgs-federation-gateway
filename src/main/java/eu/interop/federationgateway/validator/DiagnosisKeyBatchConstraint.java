/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

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
