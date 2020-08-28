package uk.gov.caz.psr.service.exception;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "User not found")
public class UserNotFoundException extends ApplicationRuntimeException {

  private static final long serialVersionUID = -123323598515802055L;

  public UserNotFoundException(Optional<String> message) {
    super(message.orElse("User not found"));
  }

  public UserNotFoundException() {
    this(Optional.empty());
  }
}