package uk.gov.caz.psr.service.exception;

import lombok.Value;

/**
 * Exception class which will be used to throw exception when {@link
 * uk.gov.caz.psr.model.VehicleEntrantPayment} was not found in {@code
 * VehicleEntrantPaymentRepository} findOne methods.
 */
@Value
public class MissingVehicleEntrantPaymentException extends IllegalArgumentException {

  String vrn;
  String message;
}