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

import static eu.interop.federationgateway.service.CallbackTaskExecutorService.MDC_PROP_CALLBACK_ID;
import static eu.interop.federationgateway.service.CallbackTaskExecutorService.MDC_PROP_COUNTRY;
import static eu.interop.federationgateway.service.CallbackTaskExecutorService.MDC_PROP_TASK_ID;

import eu.interop.federationgateway.entity.CallbackTaskEntity;
import eu.interop.federationgateway.repository.CallbackTaskRepository;
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionalCallbackTaskExecutorService {

  private final CallbackTaskRepository callbackTaskRepository;

  /**
   * Queries the database for the following task of a CallbackTaskEntity and removes the notBefore property.
   *
   * @param currentTask the tasks the following should be searched for.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  void removeNotBeforeForNextTaskAndDeleteTask(CallbackTaskEntity currentTask) {
    callbackTaskRepository.findFirstByNotBeforeIs(currentTask).ifPresent(task -> {
      EfgsMdc.put(MDC_PROP_CALLBACK_ID, currentTask.getCallbackSubscription().getCallbackId());
      EfgsMdc.put(MDC_PROP_COUNTRY, currentTask.getCallbackSubscription().getCountry());

      log.info("Removing notBefore restriction from CallbackTask.");

      task.setNotBefore(null);
      callbackTaskRepository.save(task);
    });

    log.info("Deleting CallbackTask from db");
    callbackTaskRepository.delete(currentTask);
  }

  /**
   * Sets execution lock for given task so that no other instance will work on this task.
   *
   * @param task The task to be locked.
   */
  void setExecutionLock(CallbackTaskEntity task) {
    EfgsMdc.put(MDC_PROP_TASK_ID, task.getId());
    log.info("Setting execution lock for CallbackTask");
    task.setExecutionLock(ZonedDateTime.now(ZoneOffset.UTC));
    callbackTaskRepository.save(task);
  }

  /**
   * Removes execution lock from task.
   *
   * @param task the task.
   */
  void removeExecutionLock(CallbackTaskEntity task) {
    EfgsMdc.put(MDC_PROP_TASK_ID, task.getId());
    log.info("Removing execution lock for CallbackTask.");
    task.setExecutionLock(null);
    callbackTaskRepository.save(task);
  }
}
