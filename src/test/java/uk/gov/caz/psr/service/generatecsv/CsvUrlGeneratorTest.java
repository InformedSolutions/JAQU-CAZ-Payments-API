package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import uk.gov.caz.psr.service.generatecsv.exception.PresignedUrlException;

@ExtendWith(MockitoExtension.class)
class CsvUrlGeneratorTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String FILENAME = "filename";

  @Mock
  private S3Presigner s3Presigner;

  @InjectMocks
  private CsvUrlGenerator csvUrlGenerator;

  @Test
  public void shouldThrowS3ExceptionsDuringPresignUrl() {
    // given
    mockS3Bucket();
    prepareS3ClientToThrow();

    // when
    Throwable throwable = catchThrowable(
        () -> csvUrlGenerator.getPresignedUrl(FILENAME));

    // then
    then(throwable)
        .isInstanceOf(PresignedUrlException.class)
        .hasMessage(
            "Exception while getting presigned url for s3Bucket/filename");
  }

  private void mockS3Bucket() {
    ReflectionTestUtils.setField(csvUrlGenerator, "bucket", S3_BUCKET);
  }

  private void prepareS3ClientToThrow() {
    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willThrow(mockS3Exception());
  }

  private AwsServiceException mockS3Exception() {
    return S3Exception.builder().awsErrorDetails(getStubbedAwsErrorDetails()).build();
  }

  private AwsErrorDetails getStubbedAwsErrorDetails() {
    return AwsErrorDetails
        .builder()
        .errorCode("S3Exception")
        .errorMessage("")
        .build();
  }
}