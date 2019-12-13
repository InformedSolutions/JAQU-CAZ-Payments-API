package uk.gov.caz.psr.dto.external;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws.secretsmanager")
@Data
public class SecretsManagerProperties {
  String prefix;
  String name;
  String profileSeparator;
  String environment;
}
