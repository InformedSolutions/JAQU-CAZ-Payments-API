package uk.gov.caz.psr.service.exception;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,
    reason = "Not able to get account vehicles - Bad request")
public class AccountVehiclesBadRequest extends ApplicationRuntimeException {

  public AccountVehiclesBadRequest(Optional<String> message) {
    super(message.orElse("Not able to get account vehicles - Bad request"));
  }

  public AccountVehiclesBadRequest() {
    this(Optional.empty());
  }
}