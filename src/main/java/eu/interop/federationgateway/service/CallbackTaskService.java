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
import eu.interop.federationgateway.entity.DiagnosisKeyBatchEntity;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CallbackTaskService {

  private final CallbackTaskRepository callbackTaskRepository;

  private final CallbackService callbackService;

  public int removeTaskLocksOlderThan(ZonedDateTime timestamp) {
    return callbackTaskRepository.removeTaskLocksOlderThan(timestamp);
  }

  public CallbackTaskEntity saveCallbackTaskEntity(CallbackTaskEntity entity) {
    return callbackTaskRepository.save(entity);
  }
  public void notifyAllCountriesForNewBatchTag(DiagnosisKeyBatchEntity batch) {
    List<CallbackSubscriptionEntity> callbacks = callbackService.getAllCallbackSubscriptions();

    callbacks.forEach(callback -> {
      if (callback.getCertificateEntity().getRevoked()) {
        log.error("Callback certificate is revoked.\", country={}, certThumbprint=\"{}",
          callback.getCountry(), callback.getCertificateEntity().getThumbprint());
        return;
      }

      log.info("Saving Callback Task to DB\", country={}, batchTag=\"{}",
        callback.getCountry(), batch.getBatchName());

      CallbackTaskEntity callbackTask = new CallbackTaskEntity(
        null,
        ZonedDateTime.now(ZoneOffset.UTC),
        null,
        0,
        batch,
        callback
      );

      saveCallbackTaskEntity(callbackTask);
    });
  }

}
