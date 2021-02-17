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

package eu.interop.federationgateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
public class DiagnosisKeyController {

  private static final String OPTIONS_ROUTE = "/diagnosiskeys";

  /**
   * Offers the options for the diagnosiskeys routes.
   *
   * @return the endpoint information
   */
  @Operation(
    summary = "Options for diagnosis keys. E.g. supported media types",
    description = "Offers the options for the diagnosiskeys routes.",
    tags = {"Diagnosis Keys Exchange Interface"},
    responses = {
      @ApiResponse(responseCode = "200", description = "OK.",
        headers = {
          @Header(name = HttpHeaders.ACCEPT,
            schema = @Schema(example = "application/json; version=1.0, application/protobuf; version=1.0")),
          @Header(name = HttpHeaders.ALLOW,
            schema = @Schema(example = "POST,GET,OPTIONS"))
        })
    })
  @RequestMapping(value = OPTIONS_ROUTE, method = RequestMethod.OPTIONS)
  public ResponseEntity<Void> getOptions() {
    return ResponseEntity
      .ok()
      .allow(HttpMethod.POST, HttpMethod.GET, HttpMethod.OPTIONS)
      .header(HttpHeaders.ACCEPT, "application/json; version=1.0, application/protobuf; version=1.0")
      .build();
  }
}
