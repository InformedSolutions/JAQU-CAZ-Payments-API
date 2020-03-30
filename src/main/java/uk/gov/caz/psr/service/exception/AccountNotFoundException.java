package uk.gov.caz.psr.service.exception;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Account not found")
public class AccountNotFoundException extends ApplicationRuntimeException {

  private static final long serialVersionUID = -50430560923110204L;

  public AccountNotFoundException(Optional<String> message) {
    super(message.orElse("Account not found"));
  }

  public AccountNotFoundException() {
    this(Optional.empty());
  }
}
