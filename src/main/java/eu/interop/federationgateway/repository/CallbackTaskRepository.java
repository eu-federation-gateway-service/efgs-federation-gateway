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

package eu.interop.federationgateway.repository;

import eu.interop.federationgateway.entity.CallbackSubscriptionEntity;
import eu.interop.federationgateway.entity.CallbackTaskEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallbackTaskRepository extends JpaRepository<CallbackTaskEntity, Long> {

  @Modifying
  @Query("UPDATE CallbackTaskEntity c SET c.executionLock = null WHERE c.executionLock < :timestamp")
  @Transactional(Transactional.TxType.REQUIRED)
  int removeTaskLocksOlderThan(@Param("timestamp") ZonedDateTime timestamp);

  CallbackTaskEntity findFirstByCallbackSubscriptionIsOrderByCreatedAtDesc(CallbackSubscriptionEntity subscription);

  @Modifying
  @Transactional(Transactional.TxType.REQUIRED)
  void deleteAllByCallbackSubscriptionIs(CallbackSubscriptionEntity subscriptionEntity);

  @Query("SELECT t FROM CallbackTaskEntity t WHERE t.notBefore is null "
    + " AND t.executionLock is null AND (t.lastTry is null OR t.lastTry < :lastTry)")
  List<CallbackTaskEntity> findNextPendingCallbackTask(@Param("lastTry") ZonedDateTime lastTry, Pageable pageable);

  Optional<CallbackTaskEntity> findFirstByNotBeforeIs(CallbackTaskEntity callbackTaskEntity);

}
