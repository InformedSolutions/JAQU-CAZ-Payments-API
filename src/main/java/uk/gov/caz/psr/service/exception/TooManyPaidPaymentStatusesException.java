package uk.gov.caz.psr.service.exception;

import lombok.Value;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.PaymentStatus} was not found more than 1 time with status PAID
 * in {@link uk.gov.caz.psr.repository.PaymentStatusRepository}.
 */
@Value
public class TooManyPaidPaymentStatusesException extends IllegalStateException {

  String vrn;
  String message;
}
