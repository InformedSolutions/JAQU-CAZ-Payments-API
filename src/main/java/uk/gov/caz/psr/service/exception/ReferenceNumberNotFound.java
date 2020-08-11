package uk.gov.caz.psr.service.exception;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.NOT_FOUND,
    reason = "Payment with given reference number not found")
public class ReferenceNumberNotFound extends ApplicationRuntimeException {

  private static final long serialVersionUID = -106488539783813842L;

  public ReferenceNumberNotFound(Optional<String> message) {
    super(message.orElse("Payment with given reference number not found"));
  }

  public ReferenceNumberNotFound() {
    this(Optional.empty());
  }
}