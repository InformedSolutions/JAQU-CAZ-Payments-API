package uk.gov.caz.psr.repository.exception;

import lombok.Value;

/**
 * Exception class which will be used to throw exception when {@code
 * VehicleEntrantPaymentRepository} found more then one VehicleEntrantPayment for FindOne methods.
 */
@Value
public class NotUniqueVehicleEntrantPaymentFoundException extends RuntimeException {

  String vrn;
  String message;
}
