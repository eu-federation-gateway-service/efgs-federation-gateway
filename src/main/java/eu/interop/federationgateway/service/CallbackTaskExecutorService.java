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

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.entity.CertificateEntity;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class CallbackTaskExecutorService {

  private final EfgsProperties efgsProperties;

  private final WebClient webClient;

  private final CallbackService callbackService;

  private final CallbackTaskRepository callbackTaskRepository;

  private final CertificateService certificateService;

  @Scheduled(fixedDelayString = "${efgs.callback.execute-interval}")
  void execute() {
    log.info("Callback processing started.");

    CallbackTaskEntity currentTask = getNextCallbackTask();

    while (currentTask != null) {
      setExecutionLock(currentTask);
      CallbackSubscriptionEntity subscription = currentTask.getCallbackSubscription();

      if (!callbackService.checkUrl(subscription.getUrl(), subscription.getCountry())) {
        log.error("Security check for callback url has failed. Deleting callback subscription.\","
            + " callbackId={}, country={}, url=\"{}",
          subscription.getCallbackId(), subscription.getCountry(), subscription.getUrl());

        callbackService.deleteCallbackSubscription(subscription);
        currentTask = getNextCallbackTask();
        continue;
      }

      Optional<CertificateEntity> callbackCertificateOptional = certificateService.getCallbackCertificateForUrl(
        subscription.getUrl(), subscription.getCountry());

      if (callbackCertificateOptional.isEmpty()) {
        log.error("Could not find callback certificate.\", callbackId={}, country=\"{}",
          subscription.getCallbackId(), subscription.getCountry());

        callbackService.deleteCallbackSubscription(subscription);
        currentTask = getNextCallbackTask();
        continue;
      }

      boolean callbackResult = sendCallback(currentTask, callbackCertificateOptional.get());

      if (callbackResult) {
        log.info("Successfully executed callback. Deleting callback task from database\", "
            + "retry={}, callbackId={}, country=\"{}", currentTask.getRetries(),
          subscription.getCallbackId(), subscription.getCountry());

        removeNotBeforeForNextTask(currentTask);
        deleteTask(currentTask);
      } else {
        if (currentTask.getRetries() >= efgsProperties.getCallback().getMaxRetries()) {
          log.error("Callback reached max amount of retries. Deleting callback subscription.\""
            + ", callbackId={}, country=\"{}", subscription.getCallbackId(), subscription.getCountry());

          callbackService.deleteCallbackSubscription(subscription);
        } else {
          currentTask.setRetries(currentTask.getRetries() + 1);
          currentTask.setLastTry(ZonedDateTime.now(ZoneOffset.UTC));

          removeExecutionLock(currentTask);
        }
      }

      currentTask = getNextCallbackTask();
    }

    log.info("Callback processing finished.");
  }

  private void removeNotBeforeForNextTask(CallbackTaskEntity currentTask) {
    callbackTaskRepository.findFirstByNotBeforeIs(currentTask).ifPresent(task -> {
      log.info("Removing notBefore restriction from CallbackTask.\", callbackId={}, country=\"{}",
        currentTask.getCallbackSubscription().getCallbackId(),
        currentTask.getCallbackSubscription().getCountry());

      task.setNotBefore(null);
      callbackTaskRepository.save(task);
    });
  }

  boolean sendCallback(CallbackTaskEntity callbackTask, CertificateEntity certificate) {
    CallbackSubscriptionEntity callbackSubscription = callbackTask.getCallbackSubscription();

    URI requestUri = UriComponentsBuilder.fromHttpUrl(callbackSubscription.getUrl())
      .queryParam("batchTag", callbackTask.getBatch().getBatchName())
      .queryParam("date", callbackTask.getBatch().getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE))
      .build().toUri();

    ClientResponse callbackResponse = webClient.get()
      .uri(requestUri)
      .header(efgsProperties.getCertAuth().getHeaderFields().getThumbprint(), certificate.getThumbprint())
      .exchange()
      .block();

    if (callbackResponse != null && callbackResponse.statusCode().is2xxSuccessful()) {
      log.info("Got 2xx response for callback.\", callbackId={}, country=\"{}",
        callbackSubscription.getCallbackId(), callbackSubscription.getCountry());

      return true;
    } else {
      if (callbackResponse != null) {
        log.error("Got a non 2xx response for callback.\", statusCode={}, callbackId={}, country=\"{}",
          callbackResponse.rawStatusCode(), callbackSubscription.getCallbackId(), callbackSubscription.getCountry());
      } else {
        log.error("Got no response for callback.\", callbackId={}, country=\"{}",
          callbackSubscription.getCallbackId(), callbackSubscription.getCountry());
      }

      return false;
    }
  }

  /**
   * Deletes a CallbackTaskEntity from database.
   *
   * @param task the task that has to be deleted.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void deleteTask(CallbackTaskEntity task) {
    log.info("Deleting CallbackTask from db.\", taskId=\"{}", task.getId());
    callbackTaskRepository.delete(task);
  }

  /**
   * Sets execution lock for given task so that no other instance will work on this task.
   *
   * @param task The task to be locked.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void setExecutionLock(CallbackTaskEntity task) {
    log.info("Setting execution lock for CallbackTask.\", taskId=\"{}", task.getId());
    task.setExecutionLock(ZonedDateTime.now(ZoneOffset.UTC));
    callbackTaskRepository.save(task);
  }

  /**
   * Removes execution lock from task.
   *
   * @param task the task.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void removeExecutionLock(CallbackTaskEntity task) {
    log.info("Removing execution lock for CallbackTask.\", taskId=\"{}", task.getId());
    task.setExecutionLock(null);
    callbackTaskRepository.save(task);
  }

  private CallbackTaskEntity getNextCallbackTask() {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(
      efgsProperties.getCallback().getRetryWait()
    );

    return callbackTaskRepository.findNextPendingCallbackTask(timestamp);
  }
}
