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

import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.filter.CertificateAuthentificationRequired;
import eu.interop.federationgateway.mapper.CallbackMapper;
import eu.interop.federationgateway.model.Callback;
import eu.interop.federationgateway.service.CallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/diagnosiskeys")
@Validated
public class CallbackAdminController {

  private static final String CALLBACK_GET_ROUTE = "/callback";
  private static final String CALLBACK_CHANGE_ROUTE = "/callback/{id}";

  private final CallbackService callbackService;

  private final CallbackMapper callbackMapper;

  /**
   * Gets the current callback subscription URLs.
   *
   * @param country A {@link String} containing the country.
   * @return List with registered callbacks.
   */
  @Operation(
    summary = "Gets the current callback subscription URLs.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"}
  )
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "OK.", content = @Content),
    @ApiResponse(responseCode = "403",
      description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content)
  })
  @GetMapping(value = CALLBACK_GET_ROUTE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @CertificateAuthentificationRequired
  public ResponseEntity<List<Callback>> getCallbackSubscriptions(
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY) String country
  ) {
    List<CallbackSubscriptionEntity> allCallbackEntities =
      callbackService.getAllCallbackSubscriptionsForCountry(country);

    List<Callback> callbacks = callbackMapper.entityToCallback(allCallbackEntities);
    return ResponseEntity
      .ok()
      .body(callbacks);
  }

  /**
   * Put or Update new callback subscription URL.
   */
  @Operation(
    summary = "Create or Update new callback subscription URL.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"},
    responses = {
      @ApiResponse(responseCode = "200", description = "OK.", content = @Content),
      @ApiResponse(responseCode = "400", description = "One or more parameters are invalid.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "406", description = "URL has not the expected format.", content = @Content),
      @ApiResponse(responseCode = "500", description = "Not able to write data. Retry please.", content = @Content),
    },
    parameters = {
      @Parameter(
        name = "id",
        in = ParameterIn.PATH,
        required = true,
        description = "callbackId of the entry",
        example = "ABC"),
      @Parameter(name = "url",
        in = ParameterIn.QUERY,
        required = true,
        example = "https://example.org")
    })
  @PutMapping(value = CALLBACK_CHANGE_ROUTE)
  @CertificateAuthentificationRequired
  public ResponseEntity<Void> putOrUpdateCallbackSubscription(
    @PathVariable("id") String callbackId,
    @RequestParam("url") String url,
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY) String country
  ) {
    if (!callbackService.checkUrl(url, country)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Callback URL is invalid");
    }

    CallbackSubscriptionEntity callbackSubscriptionEntity = callbackMapper.callbackToEntity(callbackId, url, country);

    callbackService.saveCallbackSubscription(callbackSubscriptionEntity);
    return ResponseEntity
      .ok()
      .build();
  }

  /**
   * Delete a callback subscription.
   *
   * @param callbackId A {@link String} containing the callbackId.
   */
  @Operation(
    summary = "Delete callback URL.",
    tags = {"Diagnosis Keys Exchange Interface", "Callback"},
    responses = {
      @ApiResponse(responseCode = "200", description = "OK.", content = @Content),
      @ApiResponse(responseCode = "400", description = "The given callbackId is not valid.", content = @Content),
      @ApiResponse(responseCode = "403",
        description = "Forbidden call in cause of missing or invalid client certificate.", content = @Content),
      @ApiResponse(responseCode = "404", description = "Id not found or no data exists.", content = @Content),
      @ApiResponse(responseCode = "500", description = "Not able to write data. Retry please.", content = @Content),
    },
    parameters = @Parameter(
      name = "id",
      in = ParameterIn.PATH,
      required = true,
      description = "callbackId of the entry")
  )
  @DeleteMapping(value = CALLBACK_CHANGE_ROUTE)
  @CertificateAuthentificationRequired
  public ResponseEntity<Void> deleteCallbackSubscription(
    @PathVariable("id") String callbackId,
    @RequestAttribute(CertificateAuthentificationFilter.REQUEST_PROP_COUNTRY) String country
  ) {
    Optional<CallbackSubscriptionEntity> callbackSubscription =
      callbackService.getCallbackSubscription(callbackId, country);

    if (callbackSubscription.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        "Could not find any callback subscription for the given callbackId.");
    }

    callbackService.deleteCallbackSubscription(callbackSubscription.get());

    return ResponseEntity
      .ok()
      .build();
  }

}
