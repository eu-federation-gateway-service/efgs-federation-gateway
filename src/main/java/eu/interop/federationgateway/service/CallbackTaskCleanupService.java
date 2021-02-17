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
import eu.interop.federationgateway.utils.EfgsMdc;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackTaskCleanupService {

  private final CallbackService callbackService;

  private final EfgsProperties efgsProperties;


  /**
   * Removes periodically the execution locks from abandoned tasks.
   */
  @Scheduled(fixedDelay = 60000)
  @SchedulerLock(name = "CallbackTaskCleanupService_deleteAbandonedLocks", lockAtLeastFor = "PT0S",
    lockAtMostFor = "${efgs.callback.locklimit}")
  public void deleteAbandonedLocks() {
    log.info("Deleting task locks of abandoned tasks");

    ZonedDateTime timestamp = ZonedDateTime.now().minusSeconds(efgsProperties.getCallback().getTaskLockTimeout());

    int updateCount = callbackService.removeTaskLocksOlderThan(timestamp);

    EfgsMdc.put("taskCount", updateCount);
    log.info("Removing of task locks of abandoned tasks finished.");
    EfgsMdc.clear();

  }


}
