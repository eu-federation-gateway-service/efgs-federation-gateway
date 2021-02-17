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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosisKeyControllerTest {

  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private EfgsProperties properties;
  @Autowired
  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;
  private MockMvc mockMvc;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .build();
  }

  @Test
  public void testOptionsReturnEndpointInformation() throws Exception {
    mockMvc.perform(options("/diagnosiskeys")
      .accept("application/json; version=1.0")
    )
      .andExpect(status().isOk())
      .andExpect(mvcResult -> {
        String allowHeader = mvcResult.getResponse().getHeader(HttpHeaders.ALLOW);
        String acceptHeader = mvcResult.getResponse().getHeader(HttpHeaders.ACCEPT);
        Assert.assertEquals(HttpMethod.POST + "," + HttpMethod.GET.name() + "," + HttpMethod.OPTIONS.name(),
          allowHeader);
        Assert.assertEquals("application/json; version=1.0, application/protobuf; version=1.0", acceptHeader);
      });
  }
}
