package uk.gov.caz.psr.service.exception;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * A custom exception to denote when a chargeable account vehicle could not be reconciled
 * against a particular clean air zone. Note this is primarily used by the search endpoint
 * for consumption by the payment matrix.
 *
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND,
    reason = "No chargeable account vehicle found for requested zone")
public class ChargeableAccountVehicleNotFoundException
    extends ApplicationRuntimeException {

  private static final long serialVersionUID = -2153378952601238088L;

  public ChargeableAccountVehicleNotFoundException(Optional<String> message) {
    super(message
        .orElse("No chargeable account vehicle found for requested zone"));
  }

  public ChargeableAccountVehicleNotFoundException() {
    this(Optional.empty());
  }

}
