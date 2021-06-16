package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.caz.psr.service.generatecsv.exception.CsvExportException;

@ExtendWith(MockitoExtension.class)
class PaymentsHistoryCsvFileSupervisorTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String FILENAME = "filename";
  private static final List<UUID> ACCOUNT_USER_IDS = Arrays.asList(
      UUID.randomUUID(), UUID.randomUUID());

  @Mock
  private S3Client s3Client;

  @Mock
  private CsvFileNameGenerator csvFileNameGenerator;

  @Mock
  private CsvWriter csvWriter;

  @InjectMocks
  private PaymentsHistoryCsvFileSupervisor csvFileSupervisor;

  @Test
  public void shouldThrowS3ExceptionsDuringUploadingCsv() throws IOException {
    // given
    mockData();
    prepareS3ClientToThrow();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileSupervisor
            .uploadCsvFileAndGetFileName(UUID.randomUUID(), ACCOUNT_USER_IDS));

    // then
    then(throwable)
        .isInstanceOf(CsvExportException.class)
        .hasMessage(
            "Exception while uploading file s3Bucket/filename");
  }

  @Test
  public void shouldThrowIOExceptionsDuringUploadingCsv() throws IOException {
    // given
    mockData();
    mockIOException();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileSupervisor
            .uploadCsvFileAndGetFileName(UUID.randomUUID(), ACCOUNT_USER_IDS));

    // then
    then(throwable)
        .isInstanceOf(CsvExportException.class)
        .hasMessage(
            "Exception while uploading file s3Bucket/filename");
  }

  private void mockData() throws IOException {
    mockS3Bucket();
    mockGenerateFileName();
    mockCreateWriterWithCsvContent();
  }

  private void mockIOException() throws IOException {
    given(csvWriter.createWriterWithCsvContent(any(), any())).willThrow(IOException.class);
  }

  private void mockS3Bucket() {
    ReflectionTestUtils.setField(csvFileSupervisor, "bucket", S3_BUCKET);
  }

  private void prepareS3ClientToThrow() {
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(mockS3Exception());
  }

  private AwsServiceException mockS3Exception() {
    return S3Exception.builder().awsErrorDetails(getStubbedAwsErrorDetails()).build();
  }

  private void mockGenerateFileName() {
    given(csvFileNameGenerator.generate()).willReturn(FILENAME);
  }

  private void mockCreateWriterWithCsvContent() throws IOException {
    given(csvWriter.createWriterWithCsvContent(any(), any())).willReturn(new StringWriter());
  }

  private AwsErrorDetails getStubbedAwsErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("S3Exception")
        .errorMessage("")
        .build();
  }
}