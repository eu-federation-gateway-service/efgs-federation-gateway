package eu.interop.federationgateway.validator;

import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.batchsigning.BatchSignatureUtilsTest;
import eu.interop.federationgateway.batchsigning.SignatureGenerator;
import eu.interop.federationgateway.config.EfgsProperties;
import eu.interop.federationgateway.filter.CertificateAuthentificationFilter;
import eu.interop.federationgateway.model.EfgsProto;
import eu.interop.federationgateway.repository.CertificateRepository;
import eu.interop.federationgateway.repository.DiagnosisKeyEntityRepository;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class DiagnosisKeyBatchValidatorTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private EfgsProperties properties;

  @Autowired
  private DiagnosisKeyEntityRepository diagnosisKeyEntityRepository;

  @Autowired
  private CertificateAuthentificationFilter certFilter;

  @Autowired
  private CertificateRepository certificateRepository;

  private SignatureGenerator signatureGenerator;


  private MockMvc mockMvc;

  @Before
  public void setup() throws NoSuchAlgorithmException, CertificateException, CertIOException,
    OperatorCreationException {
    signatureGenerator = new SignatureGenerator(certificateRepository);
    TestData.insertCertificatesForAuthentication(certificateRepository);

    diagnosisKeyEntityRepository.deleteAll();
    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .addFilter(certFilter)
      .build();
  }

  @Test
  public void testInvalidDiagnosisKey() throws Exception {
    EfgsProto.DiagnosisKey key1 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(1).build();
    EfgsProto.DiagnosisKey key2 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(2).build();
    EfgsProto.DiagnosisKey key3 = TestData.getDiagnosisKeyProto().toBuilder().setTransmissionRiskLevel(3).build();

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(key1, key2, key3)).build();

    byte[] bytesToSign = BatchSignatureUtilsTest.createBytesToSign(batch);
    String signature = signatureGenerator.sign(bytesToSign, TestData.validCertificate);

    mockMvc.perform(post("/diagnosiskeys/upload")
      .contentType("application/protobuf; version=1.0")
      .header("batchTag", TestData.FIRST_BATCHTAG)
      .header("batchSignature", signature)
      .header(properties.getCertAuth().getHeaderFields().getThumbprint(), TestData.AUTH_CERT_HASH)
      .header(properties.getCertAuth().getHeaderFields().getDistinguishedName(), TestData.DN_STRING_DE)
      .content(batch.toByteArray())
    )
      .andExpect(status().isCreated())
      .andExpect(result -> Assert.assertEquals(batch.getKeysCount(), diagnosisKeyEntityRepository.count()));
  }
}
