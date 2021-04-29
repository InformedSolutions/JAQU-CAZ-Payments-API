package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.util.ResourceUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.caz.psr.ExternalCallsIT;
import uk.gov.caz.psr.annotation.IntegrationTest;

@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/csv-export/test-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@IntegrationTest
class PaymentsHistoryCsvFileSupervisorTestIT extends ExternalCallsIT {

  private static final String BUCKET_NAME = "csv-export-bucket";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("ddMMMMyyyy-HHmmss");
  @Autowired
  private S3Client s3Client;

  @Autowired
  private PaymentsHistoryCsvFileSupervisor csvFileSupervisor;

  @BeforeEach
  public void setUp() {
    createBucketInS3();
  }

  @AfterEach
  public void tearDown() {
    deleteBucketAndFilesFromS3();
  }

  @Test
  public void shouldUploadCsvFileToS3() throws IOException {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
    UUID accountUserId = UUID.fromString("88732cca-a5c7-4ad6-a60d-7edede935915");
    List<UUID> accountUserIds = Collections.singletonList(accountUserId);
    mockVccsCleanAirZonesCall();
    mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

    // when
    URL url = csvFileSupervisor.uploadCsvFileAndGetPresignedUrl(accountId, accountUserIds);

    // then
    assertThat(getS3Contents()).hasSize(1);
    assertThat(getKey()).contains("Payment-history-").contains(".csv");
    assertThat(getUploadedCsvFile()).contains(readExpectedCsv());
    assertThat(url).isNotNull();
  }

  private void createBucketInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
  }

  private void deleteBucketAndFilesFromS3() {
    deleteFilesFromS3();
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  private void deleteFilesFromS3() {
    ListObjectsResponse listObjectsResponse = s3Client
        .listObjects(ListObjectsRequest.builder().bucket(BUCKET_NAME).build());
    for (S3Object s3Object : listObjectsResponse.contents()) {
      s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(s3Object.key()));
    }
  }

  private String getUploadedCsvFile() throws IOException {
    List<S3Object> contents = getS3Contents();

    GetObjectRequest objectRequest = GetObjectRequest
        .builder()
        .key(contents.get(0).key())
        .bucket(BUCKET_NAME)
        .build();

    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
    byte[] data = objectBytes.asByteArray();

    ByteArrayInputStream input = new ByteArrayInputStream(data);
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    String line;
    StringBuilder content = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      content.append(line).append("\n");
    }
    return content.toString();
  }

  private List<S3Object> getS3Contents() {
    ListObjectsResponse listObjectsResponse = s3Client.listObjects(
        ListObjectsRequest.builder().bucket(BUCKET_NAME).build());
    return listObjectsResponse.contents();
  }

  private String getKey() {
    S3Object s3Object = getS3Contents().get(0);
    return s3Object.key();
  }

  private static String readExpectedCsv() {
    return getCsvFile(
        "classpath:data/csv/export/expected-payments-for-account-user.csv");
  }

  @SneakyThrows
  private static String getCsvFile(String file) {
    return new String(Files.readAllBytes(ResourceUtils.getFile(file).toPath()));
  }
}
