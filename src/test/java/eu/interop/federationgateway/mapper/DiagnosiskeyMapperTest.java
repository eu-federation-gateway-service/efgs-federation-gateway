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

package eu.interop.federationgateway.mapper;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.entity.DiagnosisKeyEntity;
import eu.interop.federationgateway.model.EfgsProto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosiskeyMapperTest {

  @Autowired
  DiagnosisKeyMapper mapper;

  @Test
  public void testMappingFromProtoToEntity() {
    List<DiagnosisKeyEntity> converted = mapper.protoToEntity(
      Arrays.asList(TestData.getDiagnosisKeyProto(), TestData.getDiagnosisKeyProto()),
      TestData.FIRST_BATCHTAG,
      "b", "c", "d", "e",
      new MediaType("application", "json", Map.of("version", "1.0"))
    );

    List<DiagnosisKeyEntity> expected = Arrays.asList(
      TestData.getDiagnosisKeyTestEntityforCreation(),
      TestData.getDiagnosisKeyTestEntityforCreation()
    );

    Assert.assertEquals(expected, converted);
  }

  @Test
  public void testMappingFromEntityToProto() {
    List<EfgsProto.DiagnosisKey> converted = mapper.entityToProto(Arrays.asList(
      TestData.getDiagnosisKeyTestEntityforCreation(),
      TestData.getDiagnosisKeyTestEntityforCreation()
    ));

    List<EfgsProto.DiagnosisKey> expected = Arrays.asList(
      TestData.getDiagnosisKeyProto(),
      TestData.getDiagnosisKeyProto()
    );

    Assert.assertEquals(converted, expected);
  }

  @Test
  public void testMappingFromEntityToProtoVisitedCountriesEmpty() {
    DiagnosisKeyEntity entity = TestData.getDiagnosisKeyTestEntityforCreation();
    entity.getPayload().setVisitedCountries("");

    List<EfgsProto.DiagnosisKey> converted = mapper.entityToProto(Collections.singletonList(
      entity
    ));

    List<EfgsProto.DiagnosisKey> expected = Collections.singletonList(
      TestData.getDiagnosisKeyProto().toBuilder().clearVisitedCountries().build()
    );

    Assert.assertEquals(converted, expected);
  }
}
