package uk.gov.caz.psr.service.generatecsv.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception that indicates that for a given account id there is at least one not finished register
 * job.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PresignedUrlException extends ApplicationRuntimeException {

  public PresignedUrlException(String message) {
    super(message);
  }
}