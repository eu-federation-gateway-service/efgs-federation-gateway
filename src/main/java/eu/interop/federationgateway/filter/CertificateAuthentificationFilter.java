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

package eu.interop.federationgateway.filter;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.service.CertificateService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@Component
@AllArgsConstructor
public class CertificateAuthentificationFilter extends OncePerRequestFilter {

  public static final String REQUEST_PROP_COUNTRY = "reqPropCountry";
  public static final String REQUEST_PROP_THUMBPRINT = "reqPropCertThumbprint";

  private final RequestMappingHandlerMapping requestMap;

  private final EfgsProperties properties;

  private final CertificateService certificateService;

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  @SneakyThrows
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    HandlerExecutionChain handlerExecutionChain = requestMap.getHandler(request);

    if (handlerExecutionChain == null) {
      return true;
    } else {
      return !((HandlerMethod) handlerExecutionChain.getHandler()).getMethod()
        .isAnnotationPresent(CertificateAuthentificationRequired.class);

    }
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest httpServletRequest,
    HttpServletResponse httpServletResponse,
    FilterChain filterChain
  ) throws ServletException, IOException {
    logger.debug("Checking request for auth headers");

    String headerDistinguishedName =
      httpServletRequest.getHeader(properties.getCertAuth().getHeaderFields().getDistinguishedName());

    String headerCertThumbprint =
      httpServletRequest.getHeader(properties.getCertAuth().getHeaderFields().getThumbprint());

    if (headerDistinguishedName == null || headerCertThumbprint == null) {
      log.error("No thumbprint or distinguish name");
      handlerExceptionResolver.resolveException(
        httpServletRequest, httpServletResponse, null, new ResponseStatusException(HttpStatus.FORBIDDEN));
      return;
    }

    Map<String, String> distinguishNameMap = parseDistinguishNameString(headerDistinguishedName);

    if (!distinguishNameMap.containsKey("C")) {
      log.error("Country property is missing\", dnString=\"{}\", thumbprint=\"{}",
        headerDistinguishedName, headerCertThumbprint);
      handlerExceptionResolver.resolveException(
        httpServletRequest, httpServletResponse, null,
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client Certificate must contain country property"));
      return;
    }

    Optional<CertificateEntity> certFromDb = certificateService.getCertificate(
      headerCertThumbprint,
      distinguishNameMap.get("C"),
      CertificateEntity.CertificateType.AUTHENTICATION
    );

    if (certFromDb.isEmpty()) {
      log.error("Unknown client certificate\", dnString=\"{}\", thumbprint=\"{}",
        headerDistinguishedName, headerCertThumbprint);
      handlerExceptionResolver.resolveException(
        httpServletRequest, httpServletResponse, null,
        new ResponseStatusException(HttpStatus.FORBIDDEN, "Client is not authorized to access the service"));
      return;
    }

    if (certFromDb.get().getRevoked()) {
      log.error("Certificate is revoked\", dnString=\"{}\", thumbprint=\"{}",
        headerDistinguishedName, headerCertThumbprint);
      handlerExceptionResolver.resolveException(
        httpServletRequest, httpServletResponse, null,
        new ResponseStatusException(HttpStatus.FORBIDDEN, "Client certificate is revoked"));
      return;
    }

    log.info("Successful Authentication\", dnString=\"{}\", thumbprint=\"{}",
      headerDistinguishedName, headerCertThumbprint);
    httpServletRequest.setAttribute(REQUEST_PROP_COUNTRY, distinguishNameMap.get("C"));
    httpServletRequest.setAttribute(REQUEST_PROP_THUMBPRINT, headerCertThumbprint);

    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

  /**
   * Parses a given Distinguish Name string (e.g. "C=DE,OU=Test Unit,O=Test Company").
   *
   * @param dnString the DN string to parse.
   * @return Map with properties of the DN string.
   */
  private Map<String, String> parseDistinguishNameString(String dnString) {
    return Arrays.stream(dnString.split(","))
      .map(part -> part.split("="))
      .filter(entry -> entry.length == 2)
      .collect(Collectors.toMap(arr -> arr[0].toUpperCase(), arr -> arr[1]));
  }
}
