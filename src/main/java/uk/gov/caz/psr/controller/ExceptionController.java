package uk.gov.caz.psr.controller;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.caz.GlobalExceptionHandler;
import uk.gov.caz.psr.controller.exception.DtoValidationException;
import uk.gov.caz.psr.dto.ErrorResponse;
import uk.gov.caz.psr.dto.ErrorsResponse;
import uk.gov.caz.psr.model.ValidationError;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;
import uk.gov.caz.psr.service.exception.MissingVehicleEntrantPaymentException;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionController extends GlobalExceptionHandler {

  /**
   * Method to handle Exception while VehicleEntrantPayment was not found and failed with {@link
   * MissingVehicleEntrantPaymentException}.
   *
   * @param e Exception object.
   */
  @ExceptionHandler(MissingVehicleEntrantPaymentException.class)
  ResponseEntity<ErrorsResponse> handleMissingVehicleEntrantPaymentException(
      MissingVehicleEntrantPaymentException e) {

    log.info("MissingVehicleEntrantPaymentException occurred: {}", e);
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(e.getVrn(), e.getMessage()));
  }

  /**
   * Method to handle Exception while VehicleEntrantPayment was not found and failed with {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param e Exception object.
   */
  @ExceptionHandler(NotUniqueVehicleEntrantPaymentFoundException.class)
  ResponseEntity<ErrorsResponse> handleNotUniqueVehicleEntrantPaymentFoundException(
      NotUniqueVehicleEntrantPaymentFoundException e) {

    log.info("NotUniqueVehicleEntrantPaymentFoundException occurred: {}", e);
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(e.getVrn(), e.getMessage()));
  }

  /**
   * Method to handle Exception while validation of request DTO failed with {@link
   * DtoValidationException}.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(DtoValidationException.class)
  public ResponseEntity<ErrorsResponse> handleValidationExceptions(DtoValidationException ex) {
    List<ErrorResponse> errorsList = ex.getBindingResult().getAllErrors().stream()
        .map(error -> ErrorResponse.from(
            ValidationError.builder()
                .vrn(ex.getVrn())
                .field(((FieldError) error).getField())
                .title(error.getDefaultMessage())
                .build()
            )
        ).collect(Collectors.toList());
    log.info("DtoValidationException occurred: {}", errorsList);
    return ResponseEntity.badRequest().body(ErrorsResponse.from(errorsList));
  }
}