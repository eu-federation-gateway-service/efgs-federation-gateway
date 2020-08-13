/*-
 * ---license-start
 * EU-Federation-Gateway-Service / efgs-federation-gateway
 * ---
 * Copyright (C) 2020 T-Systems International GmbH and all other contributors
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(DiagnosisKeyEntityService.DiagnosisKeyInsertException.class)
  public ResponseEntity<Object> handleException(
    DiagnosisKeyEntityService.DiagnosisKeyInsertException e
  ) {
    return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(e.getResultMap());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorMessageBody> handleException(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatus()).body(new ErrorMessageBody(e.getReason()));
  }

  @AllArgsConstructor
  @Getter
  private static class ErrorMessageBody {
    private final String message;
  }
}
