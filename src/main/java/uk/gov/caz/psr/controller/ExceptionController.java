package uk.gov.caz.psr.controller;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.caz.GlobalExceptionHandler;
import uk.gov.caz.psr.controller.exception.PaymentInfoDtoValidationException;
import uk.gov.caz.psr.controller.exception.PaymentStatusDtoValidationException;
import uk.gov.caz.psr.dto.PaymentInfoErrorResponse;
import uk.gov.caz.psr.dto.PaymentInfoErrorsResponse;
import uk.gov.caz.psr.dto.PaymentStatusErrorResponse;
import uk.gov.caz.psr.dto.PaymentStatusErrorsResponse;
import uk.gov.caz.psr.model.ValidationError;
import uk.gov.caz.psr.model.ValidationError.ValidationErrorBuilder;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;
import uk.gov.caz.psr.service.exception.MissingVehicleEntrantPaymentException;
import uk.gov.caz.psr.service.exception.TooManyPaidPaymentStatusesException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionController extends GlobalExceptionHandler {

  private static final Locale LOCALE = Locale.ENGLISH;

  private final MessageSource messageSource;

  /**
   * Method to handle Exception while VehicleEntrantPayment was not found and failed with {@link
   * MissingVehicleEntrantPaymentException}.
   *
   * @param e Exception object.
   */
  @ExceptionHandler(MissingVehicleEntrantPaymentException.class)
  ResponseEntity<PaymentStatusErrorsResponse> handleMissingVehicleEntrantPaymentException(
      MissingVehicleEntrantPaymentException e) {

    log.info("MissingVehicleEntrantPaymentException occurred", e);
    return ResponseEntity.badRequest()
        .body(PaymentStatusErrorsResponse.singleValidationErrorResponse(e.getVrn(),
            e.getMessage()));
  }

  /**
   * Method to handle Exception while VehicleEntrantPayment was not found and failed with {@link
   * NotUniqueVehicleEntrantPaymentFoundException}.
   *
   * @param e Exception object.
   */
  @ExceptionHandler(NotUniqueVehicleEntrantPaymentFoundException.class)
  ResponseEntity<PaymentStatusErrorsResponse> handleNotUniqueVehicleEntrantPaymentFoundException(
      NotUniqueVehicleEntrantPaymentFoundException e) {

    log.info("NotUniqueVehicleEntrantPaymentFoundException occurred", e);
    return ResponseEntity.badRequest()
        .body(PaymentStatusErrorsResponse.singleValidationErrorResponse(e.getVrn(),
            e.getMessage()));
  }

  /**
   * Method to handle Exception when multiple {@link uk.gov.caz.psr.model.PaymentStatus} were found
   * for cazId, vrn and cazEntryDate.
   *
   * @param exception Exception object.
   */
  @ExceptionHandler(TooManyPaidPaymentStatusesException.class)
  ResponseEntity<PaymentStatusErrorsResponse> handleTooManyPaidPaymentStatusesException(
      TooManyPaidPaymentStatusesException exception) {

    log.info("TooManyPaidPaymentStatusesException occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(PaymentStatusErrorsResponse.singleValidationErrorResponse(exception.getVrn(),
            exception.getMessage()));
  }

  /**
   * Method to handle Exception while validation of request DTO failed with {@link
   * PaymentStatusDtoValidationException}.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(PaymentStatusDtoValidationException.class)
  public ResponseEntity<PaymentStatusErrorsResponse> handlePaymentStatusValidationExceptions(
      PaymentStatusDtoValidationException ex) {
    List<PaymentStatusErrorResponse> errorsList = ex.getBindingResult().getAllErrors().stream()
        .map(error -> PaymentStatusErrorResponse.from(createValidationError(error,
            ex.getGenericValidationCode(), ex.getVrn()))
        ).collect(Collectors.toList());
    log.info("PaymentStatusDtoValidationException occurred: {}", errorsList);
    return ResponseEntity.badRequest().body(PaymentStatusErrorsResponse.from(errorsList));
  }

  /**
   * Method to handle Exception while validation of request DTO failed with {@link
   * PaymentStatusDtoValidationException}.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(PaymentInfoDtoValidationException.class)
  public ResponseEntity<PaymentInfoErrorsResponse> handlePaymentInfoValidationExceptions(
      PaymentInfoDtoValidationException ex) {
    List<PaymentInfoErrorResponse> errorsList = ex.getBindingResult().getAllErrors().stream()
        .map(error -> PaymentInfoErrorResponse.from(createValidationError(error,
            ex.getGenericValidationCode())))
        .collect(Collectors.toList());
    log.info("PaymentInfoDtoValidationException occurred: {}", errorsList);
    return ResponseEntity.badRequest().body(PaymentInfoErrorsResponse.from(errorsList));
  }

  /**
   * Creates {@link ValidationError} based on passed parameters.
   */
  private ValidationError createValidationError(ObjectError error, String validationCode,
      String vrn) {
    return createBaseValidationErrorBuilder(error, validationCode)
        .vrn(vrn)
        .build();
  }

  /**
   * Creates {@link ValidationError} based on passed parameters.
   */
  private ValidationError createValidationError(ObjectError error, String validationCode) {
    return createBaseValidationErrorBuilder(error, validationCode)
        .build();
  }

  /**
   * Creates {@link ValidationErrorBuilder} with {@code error} and {@code validationCode}.
   */
  private ValidationErrorBuilder createBaseValidationErrorBuilder(ObjectError error,
      String validationCode) {
    return ValidationError.builder()
        .detail(messageSource.getMessage(error, LOCALE))
        .title(messageSource.getMessage(validationCode, null, LOCALE));
  }
}
