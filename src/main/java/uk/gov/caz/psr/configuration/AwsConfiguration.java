package uk.gov.caz.psr.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * AWS client builder configuration.
 */
@Configuration
@Slf4j
public class AwsConfiguration {

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * Returns an instance of {@link AmazonSQS} which is used to send a message to a SQS queue mocked
   * by Localstack.
   *
   * @param sqsEndpoint An endpoint of mocked SQS. Cannot be empty or {@code null}
   * @return An instance of {@link AmazonSQSAsync}
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

    AmazonSQSClientBuilder builder =
        AmazonSQSClientBuilder.standard().withCredentials(dummyCredentialsProvider())
            .withEndpointConfiguration(new EndpointConfiguration(sqsEndpoint, region));
    AmazonSQS build = builder.build();
    log.info("client: {}", build);
    return build;
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
  @Profile("!integration-tests & !localstack")
  public AmazonSQS amazonSqs() {
    AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
    builder.withRegion(region);
    return builder.build();
  }

}
