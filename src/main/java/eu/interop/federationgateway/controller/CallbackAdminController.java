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

package eu.interop.federationgateway.controller;

import eu.interop.federationgateway.model.Callback;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/diagnosiskeys")
@Validated
public class CallbackAdminController {

  private static final String CALLBACK_GET_ROUTE = "/callback";
  private static final String CALLBACK_CHANGE_ROUTE = "/callback/{id}";

  /**
   * Gets the current callback URLs.
   *
   * @return List with registered callbacks.
   */
  @Operation(
    summary = "Gets the current callback URLs.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"}
  )
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "OK."),
    @ApiResponse(responseCode = "403",
      description = "Forbidden call in cause of missing or invalid client certificate.")
  })
  @GetMapping(value = CALLBACK_GET_ROUTE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Callback[]> getCallbacks() {
    Callback[] dummy = {
      new Callback("1", "https://telekom.de"),
      new Callback("2", "https://t-systems.com"),
      new Callback("3", "https://rki.de"),
    };

    return new ResponseEntity<>(dummy, HttpStatus.OK);
  }

  /**
   * Put or Update new callback URL.
   */
  @Operation(
    summary = "Put or Update new callback URL.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"},
    responses = {
      @ApiResponse(responseCode = "200", description = "OK."),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate."),
      @ApiResponse(responseCode = "406", description = "URL has not the expected format."),
      @ApiResponse(responseCode = "500", description = "Not able to write data. Retry please."),
    },
    parameters = {
      @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "id of the entry", example = "ABC"),
      @Parameter(name = "url", in = ParameterIn.QUERY, required = true, example = "https://example.org")
    })
  @PutMapping(
    value = CALLBACK_CHANGE_ROUTE,
    consumes = MediaType.TEXT_PLAIN_VALUE
  )
  public void putOrUpdateCallback(
    @PathVariable("id") String id,
    @RequestParam("url") String url
  ) {

  }

  /**
   * Delete callback URL.
   *
   * @param id A {@link String} containing the id.
   */
  @Operation(
    summary = "Delete callback URL.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"},
    responses = {
      @ApiResponse(responseCode = "204", description = "OK."),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate."),
      @ApiResponse(responseCode = "500", description = "Not able to write data. Retry please."),
    },
    parameters = {
      @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "id of the entry")
    }
  )
  @DeleteMapping(value = CALLBACK_CHANGE_ROUTE,
    consumes = MediaType.TEXT_PLAIN_VALUE
  )
  public void deleteCallback(
    @PathVariable("id") String id
  ) {

  }

}
