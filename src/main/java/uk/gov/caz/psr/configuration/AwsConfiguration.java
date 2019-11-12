package uk.gov.caz.psr.configuration;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AwsConfiguration {

  @Bean
  @Primary
  public AWSSecretsManager awsSecretsManagerClientBuilder(
      @Value("${cloud.aws.region.static}") String region) {
    return AWSSecretsManagerClientBuilder.standard().withRegion(region).build();
  }

}
