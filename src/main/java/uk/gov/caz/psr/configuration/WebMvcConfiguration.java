package uk.gov.caz.psr.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
class WebMvcConfiguration implements WebMvcConfigurer {
  /**
  * Override method to set default content type to Json for all APIs.
  */
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    // set default content type for responses to Json
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }
}
