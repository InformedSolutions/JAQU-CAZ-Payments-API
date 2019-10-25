package uk.gov.caz.psr.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration class for {@link org.springframework.web.client.RestTemplate} operations.
 */
@Configuration
public class RestTemplateConfiguration {

  /**
   * Creates and initializes {@link RestTemplateBuilder}.
   *
   * @param readTimeoutSeconds the timeout on waiting to read data.
   * @param connectTimeoutSeconds timeout for making the initial connection.
   * @return A configured RestTemplateBuilder.
   */
  @Bean
  public RestTemplateBuilder commonRestTemplateBuilder(
      @Value("${services.read-timeout-seconds}") Integer readTimeoutSeconds,
      @Value("${services.connection-timeout-seconds}") Integer connectTimeoutSeconds) {
    return new RestTemplateBuilder()
        .setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
  }
}
