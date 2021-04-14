package uk.gov.caz.psr.service.generatecsv;

import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import uk.gov.caz.psr.service.generatecsv.exception.PresignedUrlException;

/**
 * Generates URL for uploaded file.
 */
@Slf4j
@Component
public class CsvUrlGenerator {

  private static final Duration DURATION_60 = Duration.ofMinutes(60);
  private final String bucket;
  private final S3Presigner s3Presigner;

  /**
   * Default constructor.
   */
  public CsvUrlGenerator(@Value("${csv-export-bucket}") String bucket, S3Presigner s3Presigner) {
    this.bucket = bucket;
    this.s3Presigner = s3Presigner;
  }

  /**
   * Get presignedUrl for given key.
   *
   * @param fileName filename of the file.
   * @return {@link URL}
   */
  public URL getPresignedUrl(String fileName) {
    try {
      GetObjectPresignRequest getObjectPresignRequest = createGetObjectPresignRequest(fileName);
      PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner
          .presignGetObject(getObjectPresignRequest);
      return presignedGetObjectRequest.url();
    } catch (S3Exception e) {
      String error = String
          .format("Exception while getting presigned url for %s/%s", bucket, fileName);
      logAwsExceptionDetails(e);
      throw new PresignedUrlException(error);
    }
  }

  /**
   * Helper method to create {@link GetObjectPresignRequest}.
   */
  private GetObjectPresignRequest createGetObjectPresignRequest(String fileName) {
    return GetObjectPresignRequest.builder()
        .signatureDuration(DURATION_60)
        .getObjectRequest(createGetObjectRequest(fileName))
        .build();
  }

  /**
   * Helper method to create {@link GetObjectRequest}.
   */
  private GetObjectRequest createGetObjectRequest(String fileName) {
    return GetObjectRequest.builder()
        .bucket(bucket)
        .key(fileName)
        .build();
  }

  /**
   * Logs details of the {@code exception}.
   */
  private void logAwsExceptionDetails(S3Exception exception) {
    AwsErrorDetails details = exception.awsErrorDetails();
    log.warn("Unable to get presigned url: error code: {}, error message: {}",
        details.errorCode(), details.errorMessage());
  }
}
