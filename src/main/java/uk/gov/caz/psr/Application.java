package uk.gov.caz.psr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.caz.psr.configuration",
    "uk.gov.caz.psr.controller",
    "uk.gov.caz.psr.repository",
    "uk.gov.caz.psr.service"
})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
