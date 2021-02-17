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
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CallbackTaskCleanupServiceTest {

  CallbackTaskCleanupService callbackTaskCleanupService;

  EfgsProperties efgsProperties;

  CallbackService callbackServiceMock;

  @Before
  public void setup() {
    callbackServiceMock = Mockito.mock(CallbackService.class);
    efgsProperties = Mockito.mock(EfgsProperties.class);
    callbackTaskCleanupService = new CallbackTaskCleanupService(callbackServiceMock, efgsProperties);
  }

  @Test
  public void cleanupJobShouldCallServiceMethod() {

    int taskLockTimeout = 500;

    EfgsProperties.Callback callbackProperties = new EfgsProperties.Callback();
    callbackProperties.setTaskLockTimeout(taskLockTimeout);

    Mockito.when(efgsProperties.getCallback()).thenReturn(callbackProperties);

    ZonedDateTime expectedTimestamp = ZonedDateTime.now().minusSeconds(taskLockTimeout);

    ArgumentCaptor<ZonedDateTime> captor = ArgumentCaptor.forClass(ZonedDateTime.class);

    callbackTaskCleanupService.deleteAbandonedLocks();

    Mockito.verify(callbackServiceMock).removeTaskLocksOlderThan(captor.capture());
    Assert.assertEquals(expectedTimestamp.withNano(0), captor.getValue().withNano(0));

  }

}
