package uk.gov.caz.psr.service.exception;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Service unavailable")
public class ExternalServiceCallException extends ApplicationRuntimeException {

  private static final long serialVersionUID = -50430560923110204L;

  public ExternalServiceCallException(Optional<String> message) {
    super(message.orElse("Service unavailable"));
  }

  public ExternalServiceCallException(String message) {
    super(message);
  }

  public ExternalServiceCallException() {
    this(Optional.empty());
  }
}
