package uk.gov.caz.psr.service.exception;

import lombok.Getter;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.PaymentStatus} was not found more than 1 time with status PAID
 * in {@link uk.gov.caz.psr.repository.PaymentStatusRepository}.
 */
public class TooManyPaidPaymentStatusesException extends RuntimeException {

  private final @Getter String vrn;
  private final @Getter String message;

  public TooManyPaidPaymentStatusesException(String vrn, String message) {
    this.vrn = vrn;
    this.message = message;
  }
}
