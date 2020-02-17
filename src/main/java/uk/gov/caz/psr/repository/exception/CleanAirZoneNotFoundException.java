package uk.gov.caz.psr.repository.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception which is thrown when repository is not able to find CleanAirZone in VCCS api.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CleanAirZoneNotFoundException extends ApplicationRuntimeException {
  public CleanAirZoneNotFoundException(UUID cleanAirZoneId) {
    super("Clean Air Zone not found in VCCS: " + cleanAirZoneId);
  }
}
