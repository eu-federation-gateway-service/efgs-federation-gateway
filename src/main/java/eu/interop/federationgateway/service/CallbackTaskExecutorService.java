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

package eu.interop.federationgateway.service;

import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class CallbackTaskExecutorService {

  protected static final String MDC_PROP_TASK_ID = "taskId";
  protected static final String MDC_PROP_COUNTRY = "country";
  protected static final String MDC_PROP_CALLBACK_ID = "callbackId";

  private final EfgsProperties efgsProperties;
  private final WebClient webClient;
  private final CallbackService callbackService;
  private final CallbackTaskRepository callbackTaskRepository;
  private final TransactionalCallbackTaskExecutorService transactionalCallbackTaskExecutorService;

  /**
   * Execute Callback processing.
   */
  @Scheduled(fixedDelayString = "${efgs.callback.execute-interval}")
  public void execute() {
    log.info("Callback processing started.");

    CallbackTaskEntity currentTask = getNextCallbackTask();

    while (currentTask != null) {
      transactionalCallbackTaskExecutorService.setExecutionLock(currentTask);
      CallbackSubscriptionEntity subscription = currentTask.getCallbackSubscription();

      EfgsMdc.put(MDC_PROP_CALLBACK_ID, subscription.getCallbackId());
      EfgsMdc.put(MDC_PROP_COUNTRY, subscription.getCountry());
      EfgsMdc.put("url", subscription.getUrl());

      if (!callbackService.checkUrl(subscription.getUrl(), subscription.getCountry())) {
        log.error("Security check for callback url has failed. Deleting callback subscription.");

        callbackService.deleteCallbackSubscription(subscription);
        currentTask = getNextCallbackTask();
        continue;
      }

      boolean callbackResult = sendCallback(currentTask);
      EfgsMdc.put("retry", currentTask.getRetries());

      if (callbackResult) {
        log.info("Successfully executed callback. Deleting callback task from database");
        transactionalCallbackTaskExecutorService.removeNotBeforeForNextTaskAndDeleteTask(currentTask);
      } else {
        if (currentTask.getRetries() >= efgsProperties.getCallback().getMaxRetries()) {
          log.error("Callback reached max amount of retries. Deleting callback subscription.");

          callbackService.deleteCallbackSubscription(subscription);
        } else {
          currentTask.setRetries(currentTask.getRetries() + 1);
          currentTask.setLastTry(ZonedDateTime.now(ZoneOffset.UTC));

          transactionalCallbackTaskExecutorService.removeExecutionLock(currentTask);
        }
      }
      EfgsMdc.clear();
      currentTask = getNextCallbackTask();
    }
    log.info("Callback processing finished.");
  }

  boolean sendCallback(CallbackTaskEntity callbackTask) {
    CallbackSubscriptionEntity callbackSubscription = callbackTask.getCallbackSubscription();

    EfgsMdc.put(MDC_PROP_CALLBACK_ID, callbackSubscription.getCallbackId());
    EfgsMdc.put(MDC_PROP_COUNTRY, callbackSubscription.getCountry());

    URI requestUri = UriComponentsBuilder.fromHttpUrl(callbackSubscription.getUrl())
      .queryParam("batchTag", callbackTask.getBatch().getBatchName())
      .queryParam("date", callbackTask.getBatch().getCreatedAt()
        .withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE))
      .build().toUri();

    ResponseEntity<Void> callbackResponse;
    try {
      callbackResponse = webClient.get()
        .uri(requestUri)
        .retrieve()
        .toBodilessEntity()
        .block();
    } catch (Exception e) {
      EfgsMdc.put("callbackErrorMessage", e.getMessage());
      log.error("Callback request failed");
      return false;
    }

    if (callbackResponse != null) {
      if (callbackResponse.getStatusCode().is2xxSuccessful()) {
        log.info("Got 2xx response for callback.");

        return true;
      } else {
        EfgsMdc.put("statusCode", callbackResponse.getStatusCodeValue());
        log.error("Got a non 2xx response for callback.");
      }
    } else {
      log.error("Got no response for callback.");
    }

    return false;
  }

  private CallbackTaskEntity getNextCallbackTask() {
    ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(
      efgsProperties.getCallback().getRetryWait()
    );

    List<CallbackTaskEntity> callbackTaskEntities = callbackTaskRepository
      .findNextPendingCallbackTask(timestamp, PageRequest.of(0, 1));

    return callbackTaskEntities.size() == 1 ? callbackTaskEntities.get(0) : null;
  }
}
