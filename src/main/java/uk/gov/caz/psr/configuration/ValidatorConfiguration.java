package uk.gov.caz.psr.configuration;

import javax.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * A class responsible for setting up Spring validation. This is required for validation to work
 * when the service is exposed as a Lambda function.
 */
@Configuration
public class ValidatorConfiguration {

  /**
   * Creates a new instance of {@link Validator}.
   */
  @Bean
  @Primary
  public Validator localValidatorFactoryBean() {
    return new LocalValidatorFactoryBean();
  }
}
