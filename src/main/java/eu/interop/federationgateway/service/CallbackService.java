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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.repository.CallbackSubscriptionRepository;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackService {

  private final CallbackSubscriptionRepository callbackSubscriptionRepository;

  private final CallbackTaskRepository callbackTaskRepository;

  private final CertificateService certificateService;

  public int removeTaskLocksOlderThan(ZonedDateTime timestamp) {
    return callbackTaskRepository.removeTaskLocksOlderThan(timestamp);
  }

  public CallbackTaskEntity saveCallbackTaskEntity(CallbackTaskEntity entity) {
    return callbackTaskRepository.save(entity);
  }

  private CallbackTaskEntity getNotBeforeCallbackTask(CallbackSubscriptionEntity subscriptionEntity) {
    return callbackTaskRepository
      .findFirstByCallbackSubscriptionIsAndNotBeforeIsOrderByCreatedAtDesc(subscriptionEntity, null);
  }

  /**
   * Deletes tasks in database that are assigned to given subscription.
   *
   * @param subscription CallbackSubscriptionEntity
   */
  public void deleteAllTasksForSubscription(CallbackSubscriptionEntity subscription) {
    log.info("Deleting all CallbackTaskEntities for subscription.\", callbackId={}, country=\"{}",
      subscription.getCallbackId(), subscription.getCountry());
    callbackTaskRepository.deleteAllByCallbackSubscriptionIs(subscription);
  }

  /**
   * Creates new CallbackTasks for each callback subscription for given batch.
   *
   * @param batch The batch that has to be announced.
   */
  public void notifyAllCountriesForNewBatchTag(DiagnosisKeyBatchEntity batch) {
    List<CallbackSubscriptionEntity> callbacks = getAllCallbackSubscriptions();

    callbacks.forEach(callbackSubscription -> {
      log.info("Saving Callback Task to DB\", country={}, batchTag=\"{}",
        callbackSubscription.getCountry(), batch.getBatchName());

      CallbackTaskEntity callbackTask = new CallbackTaskEntity(
        null,
        ZonedDateTime.now(ZoneOffset.UTC),
        null,
        null,
        0,
        getNotBeforeCallbackTask(callbackSubscription),
        batch,
        callbackSubscription
      );

      saveCallbackTaskEntity(callbackTask);
    });
  }

  /**
   * Deletes the {@link CallbackSubscriptionEntity}.
   *
   * @param callbackSubscriptionEntity the entity which needs to be deleted.
   */
  public void deleteCallbackSubscription(CallbackSubscriptionEntity callbackSubscriptionEntity) {
    log.info("Start deleting callback subscription.");
    deleteAllTasksForSubscription(callbackSubscriptionEntity);
    callbackSubscriptionRepository.delete(callbackSubscriptionEntity);
  }

  /**
   * Returns all entries of {@link CallbackSubscriptionEntity}.
   *
   * @return a list of {@link CallbackSubscriptionEntity}
   */
  public List<CallbackSubscriptionEntity> getAllCallbackSubscriptions() {
    log.info("Requested all callback subscriptions.");
    return callbackSubscriptionRepository.findAll();
  }

  /**
   * Returns all entries of {@link CallbackSubscriptionEntity} for specified country.
   *
   * @return a list of {@link CallbackSubscriptionEntity}
   */
  public List<CallbackSubscriptionEntity> getAllCallbackSubscriptionsForCountry(String country) {
    return callbackSubscriptionRepository.findAllByCountryIs(country);
  }

  /**
   * Persists the {@link CallbackSubscriptionEntity} instance.
   *
   * @param callbackSubscriptionEntity the entity which will be saved
   * @return the {@link CallbackSubscriptionEntity}
   */
  public CallbackSubscriptionEntity saveCallbackSubscription(CallbackSubscriptionEntity callbackSubscriptionEntity) {
    log.info("Start saving callback subscription.");
    Optional<CallbackSubscriptionEntity> optional =
      getCallbackSubscription(callbackSubscriptionEntity.getCallbackId(), callbackSubscriptionEntity.getCountry());

    if (optional.isEmpty()) {
      callbackSubscriptionEntity.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
    } else {
      CallbackSubscriptionEntity callbackInDatabase = optional.get();
      callbackInDatabase.setUrl(callbackSubscriptionEntity.getUrl());
      callbackSubscriptionEntity = callbackInDatabase;
    }

    return callbackSubscriptionRepository.save(callbackSubscriptionEntity);
  }

  /**
   * Returns a {@link CallbackSubscriptionEntity} by a specific callback id.
   *
   * @param callbackId the id of the callback.
   * @return the {@link CallbackSubscriptionEntity} with the given id and thumbprint.
   */
  public Optional<CallbackSubscriptionEntity> getCallbackSubscription(String callbackId, String country) {
    log.info("Start finding callback subscription by callback id.");
    return callbackSubscriptionRepository.findByCallbackIdAndCountryIs(callbackId, country);
  }

  /**
   * Checks whether a given URL can be accepted for callback.
   *
   * @param urlToCheck the url that has to be checked.
   * @param country    the country code to check whether a certificate exists.
   * @return returns true if the url can be used for callbacks.
   */
  public boolean checkUrl(String urlToCheck, String country) {
    URL url;
    InetAddress hostAddress;

    try {
      url = new URL(urlToCheck);
    } catch (MalformedURLException e) {
      log.error("Could not parse URL\", url=\"{}", urlToCheck);
      return false;
    }


    if (!url.getProtocol().equals("https")) {
      log.error("Callback URL must use https\", url=\"{}", urlToCheck);
      return false;
    }

    if (url.getQuery() != null) {
      log.error("URL must not contain any parameters\", url=\"{}", urlToCheck);
      return false;
    }

    Optional<CertificateEntity> callbackCertificate =
      certificateService.getCallbackCertificateForHost(url.getHost(), country);

    if (callbackCertificate.isEmpty()) {
      log.error("Could not find a Callback Certificate for host\", host=\"{}", url.getHost());
      return false;
    }

    if (callbackCertificate.get().getRevoked()) {
      log.error("Found Callback Certificate, but it is revoked\", thumbprint=\"{}",
        callbackCertificate.get().getThumbprint());
      return false;
    }

    try {
      hostAddress = InetAddress.getByName(url.getHost());
    } catch (UnknownHostException e) {
      log.error("Could not resolve host for callback\", url=\"{}\", hostname=\"{}", urlToCheck, url.getHost());
      return false;
    }

    IpAddressMatcher[] localSubnetMatchers = {
      new IpAddressMatcher("10.0.0.0/8"),
      new IpAddressMatcher("127.0.0.0/8"),
      new IpAddressMatcher("100.64.0.0/10"),
      new IpAddressMatcher("169.254.0.0/16"),
      new IpAddressMatcher("172.16.0.0/12"),
      new IpAddressMatcher("192.168.0.0/16"),
      new IpAddressMatcher("::1"),
      new IpAddressMatcher("fc00::/7")
    };

    if (Arrays.stream(localSubnetMatchers)
      .anyMatch(matcher -> matcher.matches(hostAddress.getHostAddress()))) {
      log.error("IP Address of callback host is from private IP range.\", url=\"{}\", hostname=\"{}", urlToCheck,
        hostAddress.getHostAddress());
      return false;
    }

    return true;
  }
}
