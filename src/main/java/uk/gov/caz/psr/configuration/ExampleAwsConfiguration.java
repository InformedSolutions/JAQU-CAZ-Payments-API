package uk.gov.caz.psr.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ExampleAwsConfiguration {

  //
  // Below commented code is an example of S3Client configuration using different profiles.
  // AwsHelpers.areWeRunningLocallyUsingSam() returns true if Lambda was run locally using
  // AWS SAM Local tool.
  // You can use @Profile("localstack") and @Profile("!localstack") to instantiate beans
  // for Localstack local runs or SAM Local AND AWS Lambda.
  // You can use this code as a starting point for configuring different AWS Services in different
  // profiles.
  //

//  /**
//   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3 mocked
//   * by Localstack.
//   *
//   * @param s3Endpoint An endpoint of mocked S3. Cannot be empty or {@code null}
//   * @return An instance of {@link S3Client}
//   * @throws IllegalStateException if {@code s3Endpoint} is null or empty
//   */
//  @Profile("localstack")
//  @ConditionalOnMissingBean
//  @Bean
//  public S3Client s3LocalstackClient(@Value("${aws.s3.endpoint:}") String s3Endpoint) {
//    log.info("Running Spring-Boot app locally using Localstack.
//              Using 'dummy' AWS credentials and 'eu-west-2' region.");
//
//    logAwsVariables();
//
//    if (Strings.isNullOrEmpty(s3Endpoint)) {
//      throw new IllegalStateException("S3 endpoint must be overridden when running with "
//          + "Localstack! Please set in 'aws.s3.endpoint' property");
//    }
//
//    log.info("Using '{}' as S3 Endpoint", s3Endpoint);
//
//    return S3Client.builder()
//        .region(Region.of("eu-west-2"))
//        .endpointOverride(URI.create(s3Endpoint))
//
//        // unfortunately there is a checksum error when uploading a file to localstack
//        // so the check must be disabled
//        .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
//        .credentialsProvider(() -> AwsBasicCredentials.create("dummy", "dummy"))
//        .build();
//  }
//
//  /**
//   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3. All
//   * configuration MUST be specified by environment variables.
//   *
//   * @return An instance of {@link S3Client}
//   */
//  @Bean
//  @Profile("!localstack")
//  @ConditionalOnMissingBean
//  public S3Client s3Client() {
//    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
//      log.info("Running Lambda locally using SAM Local");
//    }
//
//    logAwsVariables();
//
//    return S3Client.create();
//  }
//
//  private void logAwsVariables() {
//    String awsAccessKeyId = AwsHelpers.getAwsAccessKeyFromEnvVar();
//    String awsRegion = AwsHelpers.getAwsRegionFromEnvVar();
//    String awsProfile = AwsHelpers.getAwsProfileFromEnvVar();
//
//    log.info("IAM env credentials: Access Key Id is '{}';
//             AWS Region is '{}'; AWS profile is '{}'",
//        awsAccessKeyId,
//        awsRegion,
//        awsProfile);
//  }
}
