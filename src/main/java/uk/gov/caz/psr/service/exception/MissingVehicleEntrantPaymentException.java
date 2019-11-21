package uk.gov.caz.psr.service.exception;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.VehicleEntrantPayment} was not found in
 * {@code VehicleEntrantPaymentRepository} findOne methods.
 */
public class MissingVehicleEntrantPaymentException extends RuntimeException {

  public MissingVehicleEntrantPaymentException(String message) {
    super(message);
  }
}