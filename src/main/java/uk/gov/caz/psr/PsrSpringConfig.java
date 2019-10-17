package uk.gov.caz.psr;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import uk.gov.caz.ApplicationConfiguration;

@SpringBootConfiguration
@ComponentScan(basePackages = {
    "uk.gov.caz.psr.configuration",
    "uk.gov.caz.psr.controller"
})
public class PsrSpringConfig implements ApplicationConfiguration {
}
