package uk.gov.caz.psr.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.common.base.Strings;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import uk.gov.caz.awslambda.AwsHelpers;

/**
 * AWS client builder configuration.
 */
@Configuration
@Slf4j
public class AwsConfiguration {

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * Returns an instance of {@link AmazonSecretsManager} which is used to to retrieve secrets from
   * AWS secrets manager.
   * 
   * @param secretsManagerEndpoint An endpoint of mocked Secrets Manager. Cannot be empty or
   *        {@code null}
   * @return An instance of {@link AmazonSecretsManager}
   * @throws IllegalStateException if {@code secretsManagerEndpoint} is null or empty
   */
  @Bean
  @Primary
  @Profile({"integration-tests", "localstack"})
  public AWSSecretsManager secretsManagerLocalstackClient(
      @Value("${aws.secretsmanager.endpoint:}") String secretsManagerEndpoint) {
    if (Strings.isNullOrEmpty(secretsManagerEndpoint)) {
      throw new IllegalStateException(
          "Secrets Manager endpoint must be overridden when running with "
              + "Localstack! Please set in 'aws.secretsmanager.endpoint' property");
    }

    log.info("Using '{}' as Secrets Manager Endpoint", secretsManagerEndpoint);

    return AWSSecretsManagerClientBuilder.standard().withCredentials(dummyCredentialsProvider())
        .withEndpointConfiguration(new EndpointConfiguration(secretsManagerEndpoint, region))
        .build();
  }

  /**
   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3 mocked by
   * Localstack.
   *
   * @param s3Endpoint An endpoint of mocked S3. Cannot be empty or {@code null}
   * @return An instance of {@link S3Client}
   * @throws IllegalStateException if {@code s3Endpoint} is null or empty
   */
  @Profile({"integration-tests", "localstack"})
  @Bean
  public S3Client s3LocalstackClient(@Value("${aws.s3.endpoint}") String s3Endpoint) {
    log.info("Running Spring-Boot app locally using Localstack. "
        + "Using 'dummy' AWS credentials and 'eu-west-2' region.");

    if (Strings.isNullOrEmpty(s3Endpoint)) {
      throw new IllegalStateException("S3 endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.s3.endpoint' property");
    }

    log.info("Using '{}' as S3 Endpoint", s3Endpoint);

    return S3Client.builder()
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create(s3Endpoint))

        // unfortunately there is a checksum error when uploading a file to localstack
        // so the check must be disabled
        .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
        .credentialsProvider(() -> AwsBasicCredentials.create("dummy", "dummy"))
        .build();
  }

  /**
   * Returns an instance of {@link S3Presigner} which is used to retrieve presigned URL from S3
   * mocked by Localstack.
   *
   * @param s3Endpoint An endpoint of mocked S3. Cannot be empty or {@code null}
   * @return An instance of {@link S3Client}
   * @throws IllegalStateException if {@code s3Endpoint} is null or empty
   */
  @Profile({"integration-tests", "localstack"})
  @Bean
  public S3Presigner s3PresignerLocalstackClient(@Value("${aws.s3.endpoint:}") String s3Endpoint) {
    log.info("Running Spring-Boot app locally using Localstack. "
        + "Using 'dummy' AWS credentials and 'eu-west-2' region.");

    if (Strings.isNullOrEmpty(s3Endpoint)) {
      throw new IllegalStateException("S3 endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.s3.endpoint' property");
    }

    log.info("Using '{}' as S3 Endpoint", s3Endpoint);

    return S3Presigner.builder()
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create(s3Endpoint))
        .credentialsProvider(() -> AwsBasicCredentials.create("dummy", "dummy"))
        .build();
  }

  /**
   * Returns an instance of {@link AWSSecretsManager} which is used to retrieve secrets from AWS
   * secrets manager.
   * 
   * @param region the AWS region
   * @return an instance of {@link AWSSecretsManager}
   */
  @Bean
  @Primary
  @Profile("!integration-tests & !localstack")
  public AWSSecretsManager awsSecretsManagerClientBuilder(
      @Value("${cloud.aws.region.static}") String region) {
    return AWSSecretsManagerClientBuilder.standard().withRegion(region).build();
  }

  /**
   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3. All
   * configuration MUST be specified by environment variables.
   *
   * @return An instance of {@link S3Client}
   */
  @Bean
  @Profile("!integration-tests & !localstack")
  public S3Client s3Client() {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info("Running Lambda locally using SAM Local");
    }
    logAwsVariables();
    return S3Client.create();
  }

  /**
   * Returns an instance of {@link S3Presigner} which is used to retrieve presigned URL from S3. All
   * configuration MUST be specified by environment variables.
   *
   * @return An instance of {@link S3Presigner}
   */
  @Bean
  @Profile("!integration-tests & !localstack")
  public S3Presigner s3Presigner() {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info("Running Lambda locally using SAM Local");
    }
    logAwsVariables();
    return S3Presigner.create();
  }

  /**
   * Returns an instance of {@link AmazonSQS} which is used to send a message to a SQS queue mocked
   * by Localstack.
   *
   * @param sqsEndpoint An endpoint of mocked SQS. Cannot be empty or {@code null}
   * @return An instance of {@link AmazonSQS}
   * @throws IllegalStateException if {@code sqsEndpoint} is null or empty
   */
  @Bean
  @Primary
  @Profile({"integration-tests", "localstack"})
  public AmazonSQS sqsLocalstackClient(@Value("${aws.sqs.endpoint:}") String sqsEndpoint) {
    log.info("Running Spring-Boot app locally using Localstack. ");

    if (Strings.isNullOrEmpty(sqsEndpoint)) {
      throw new IllegalStateException("SQS endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.sqs.endpoint' property");
    }

    log.info("Using '{}' as SQS Endpoint", sqsEndpoint);

    return AmazonSQSClientBuilder.standard().withCredentials(dummyCredentialsProvider())
        .withEndpointConfiguration(new EndpointConfiguration(sqsEndpoint, region)).build();
  }

  private AWSStaticCredentialsProvider dummyCredentialsProvider() {
    return new AWSStaticCredentialsProvider(
        new BasicAWSCredentials("dummy-access-key", "dummy-secret-key"));
  }

  /**
   * Creates the AmazonSqsAsync Bean. Overriding the default SQS Client Bean config because
   * AmazonSqsBufferedAsyncClient is not currently supported by FIFO queues.
   * 
   * @return the AmazonSqsAsync Bean
   */
  @Bean
  @Primary
  @Profile("!integration-tests & !localstack")
  public AmazonSQS amazonSqs() {
    return AmazonSQSClientBuilder.standard().withRegion(region).build();
  }

  private void logAwsVariables() {
    String awsAccessKeyId = AwsHelpers.getAwsAccessKeyFromEnvVar();
    String awsRegion = AwsHelpers.getAwsRegionFromEnvVar();
    String awsProfile = AwsHelpers.getAwsProfileFromEnvVar();

    log.info("IAM env credentials: Access Key Id is '{}'; AWS Region is '{}'; AWS profile is '{}'",
        awsAccessKeyId, awsRegion, awsProfile);
  }
}
