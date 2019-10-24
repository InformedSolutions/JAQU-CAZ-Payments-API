package uk.gov.caz.psr;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.ApplicationConfiguration;

@Configuration
@ComponentScan(basePackages = {
    "uk.gov.caz.psr.configuration",
    "uk.gov.caz.psr.service",
    "uk.gov.caz.psr.controller"
})
public class PsrSpringConfig implements ApplicationConfiguration {
}
