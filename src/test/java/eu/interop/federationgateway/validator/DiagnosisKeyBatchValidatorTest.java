package eu.interop.federationgateway.validator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import eu.interop.federationgateway.TestData;
import eu.interop.federationgateway.model.EfgsProto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import javax.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class DiagnosisKeyBatchValidatorTest {

  private DiagnosisKeyBatchValidator validator;

  private ConstraintValidatorContext contextMock;

  private int rollingStartInterval;

  @Before
  public void setup() {

    validator = new DiagnosisKeyBatchValidator();
    contextMock = mock(ConstraintValidatorContext.class);
    when(contextMock.buildConstraintViolationWithTemplate(anyString())).thenReturn(
      mock(ConstraintValidatorContext.ConstraintViolationBuilder.class)
    );

    rollingStartInterval = Math.toIntExact(Instant.now().minus(2, ChronoUnit.DAYS).getEpochSecond() / 600);
  }

  @Test
  public void testInvalidTransmissionRiskLevel() {
    int[] invalidTRisk = {-1, 9, 100};

    for (int transmissionRisk : invalidTRisk) {
      EfgsProto.DiagnosisKey diagnosisKey =
        TestData.getDiagnosisKeyProto().toBuilder()
          .setKeyData(ByteString.copyFromUtf8("abcd1234abcd1234")) // Without valid key data, that validator trips first.
          .setTransmissionRiskLevel(transmissionRisk)
          .setRollingPeriod(144)
          .setRollingStartIntervalNumber(rollingStartInterval)
          .build();

      EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
        .addAllKeys(Arrays.asList(diagnosisKey)).build();

      Assert.assertFalse(validator.isValid(batch, contextMock));
    }
  }

  @Test
  public void testValidTransmissionRiskLevel() {
    int[] transmissionRisks = {4, 0x7fffffff};

    for (int transmissionRisk : transmissionRisks) {
      EfgsProto.DiagnosisKey diagnosisKey =
        TestData.getDiagnosisKeyProto().toBuilder()
          .setKeyData(ByteString.copyFromUtf8("abcd1234abcd1234")) // Without valid key data, that validator trips first.
          .setTransmissionRiskLevel(transmissionRisk)
          .setDaysSinceOnsetOfSymptoms(1)
          .setRollingPeriod(144)
          .setRollingStartIntervalNumber(rollingStartInterval)
          .build();

      EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
        .addAllKeys(Arrays.asList(diagnosisKey)).build();

      Assert.assertTrue(validator.isValid(batch, contextMock));
    }
  }

  @Test
  public void testInvalidKeyDataEmpty() {
    EfgsProto.DiagnosisKey diagnosisKey =
      TestData.getDiagnosisKeyProto().toBuilder()
        .setKeyData(ByteString.EMPTY)
        .setRollingStartIntervalNumber(rollingStartInterval)
        .build();

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(diagnosisKey)).build();

    Assert.assertFalse(validator.isValid(batch, contextMock));
  }

  @Test
  public void testInvalidKeyData() {
    EfgsProto.DiagnosisKey diagnosisKey =
      TestData.getDiagnosisKeyProto().toBuilder()
        .setKeyData(ByteString.copyFromUtf8("1234")) // Invalid key length.
        .setRollingStartIntervalNumber(rollingStartInterval)
        .build();

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(diagnosisKey)).build();

    Assert.assertFalse(validator.isValid(batch, contextMock));
  }

  @Test
  public void testInvalidRollingStartIntervalNumber() {
    int[] invalidRollingStartIntervalNumbers = {
      0,
      Math.toIntExact(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond() / 600),
      Math.toIntExact(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(16, ChronoUnit.DAYS).getEpochSecond() / 600)
    };

    for (int invalidRollingStartIntervalNumber : invalidRollingStartIntervalNumbers) {
      EfgsProto.DiagnosisKey diagnosisKey =
        TestData.getDiagnosisKeyProto().toBuilder().setRollingStartIntervalNumber(invalidRollingStartIntervalNumber).build();

      EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
        .addAllKeys(Arrays.asList(diagnosisKey)).build();

      Assert.assertFalse(validator.isValid(batch, contextMock));
    }
  }

  @Test
  public void testValidRollingStartIntervalNumber() {
    int[] validRollingStartIntervalNumbers = {
      Math.toIntExact(Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond() / 600),
      Math.toIntExact(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(14, ChronoUnit.DAYS).getEpochSecond() / 600)
    };

    for (int validRollingStartIntervalNumber : validRollingStartIntervalNumbers) {
      EfgsProto.DiagnosisKey diagnosisKey =
        TestData.getDiagnosisKeyProto().toBuilder().setRollingStartIntervalNumber(validRollingStartIntervalNumber).build();

      EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
        .addAllKeys(Arrays.asList(diagnosisKey)).build();

      Assert.assertTrue(validator.isValid(batch, contextMock));
    }
  }

  @Test
  public void testInvalidRollingPeriod() {
    EfgsProto.DiagnosisKey diagnosisKey =
      TestData.getDiagnosisKeyProto().toBuilder()
        .setKeyData(ByteString.copyFromUtf8("abcd1234abcd1234"))
        .setRollingPeriod(145)
        .setTransmissionRiskLevel(6)
        .setRollingStartIntervalNumber(rollingStartInterval)
        .build();

    EfgsProto.DiagnosisKeyBatch batch = EfgsProto.DiagnosisKeyBatch.newBuilder()
      .addAllKeys(Arrays.asList(diagnosisKey)).build();

    Assert.assertFalse(validator.isValid(batch, contextMock));
  }

}
