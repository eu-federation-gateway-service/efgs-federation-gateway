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

package eu.interop.federationgateway.config;

import eu.interop.federationgateway.service.DiagnosisKeyEntityService;
import javax.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

  /**
   * Error Handler for DiagnosisKeyInsertException.
   *
   * @param e the thrown exception
   * @return A ResponseEntity with formatted information which keys were uploaded successfully
   */
  @ExceptionHandler(DiagnosisKeyEntityService.DiagnosisKeyInsertException.class)
  public ResponseEntity<Object> handleException(
    DiagnosisKeyEntityService.DiagnosisKeyInsertException e
  ) {
    return ResponseEntity
      .status(HttpStatus.MULTI_STATUS)
      .contentType(MediaType.APPLICATION_JSON)
      .body(e.getResultMap());
  }

  /**
   * Handles {@link ConstraintViolationException} when a validation failed.
   *
   * @param e the thrown {@link ConstraintViolationException}
   * @return A ResponseEntity with a ErrorMessage inside.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorMessageBody> handleException(ConstraintViolationException e) {
    log.error(e.getMessage());
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .contentType(MediaType.APPLICATION_JSON)
      .body(new ErrorMessageBody(e.getMessage()));
  }

  /**
   * Global Exception Handler to wrap exceptions into a readable JSON Object.
   *
   * @param e the thrown exception
   * @return ResponseEntity with readable data.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorMessageBody> handleException(Exception e) {
    if (e instanceof ResponseStatusException) {
      return ResponseEntity
        .status(((ResponseStatusException) e).getStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorMessageBody(((ResponseStatusException) e).getReason()));
    } else {
      log.error("Uncatched exception", e);
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorMessageBody("Internal Server Error."));
    }
  }

  @AllArgsConstructor
  @Getter
  private static class ErrorMessageBody {
    private final String message;
  }
}
