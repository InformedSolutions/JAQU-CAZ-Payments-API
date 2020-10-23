
package uk.gov.caz.psr.controller;

import static uk.gov.caz.psr.dto.PaymentInfoErrorResponse.maxDateRangeErrorResponseWithField;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uk.gov.caz.GlobalExceptionHandler;
import uk.gov.caz.psr.controller.exception.PaymentInfoDtoValidationException;
import uk.gov.caz.psr.controller.exception.PaymentInfoPaymentMadeDateValidationException;
import uk.gov.caz.psr.controller.exception.PaymentInfoVrnValidationException;
import uk.gov.caz.psr.controller.exception.PaymentStatusDtoValidationException;
import uk.gov.caz.psr.dto.GenericErrorResponse;
import uk.gov.caz.psr.dto.PaymentInfoErrorResponse;
import uk.gov.caz.psr.dto.PaymentInfoErrorsResponse;
import uk.gov.caz.psr.dto.PaymentStatusErrorResponse;
import uk.gov.caz.psr.dto.PaymentStatusErrorsResponse;
import uk.gov.caz.psr.dto.validation.PaymentInfoMaxDateRangeValidationException;
import uk.gov.caz.psr.model.ValidationError;
import uk.gov.caz.psr.model.ValidationError.ValidationErrorBuilder;
import uk.gov.caz.psr.repository.exception.NotUniqueVehicleEntrantPaymentFoundException;
import uk.gov.caz.psr.service.exception.PaymentDoesNotExistException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionController extends GlobalExceptionHandler {

  private static final Locale LOCALE = Locale.ENGLISH;
  private static final String EMPTY = "must not be null";

  private final MessageSource messageSource;


  /**
   * Method to handle Exception when a unique VehicleEntrantPayment was not found and failed with
   * {@link NotUniqueVehicleEntrantPaymentFoundException}.
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
   * Method to handle {@link PaymentDoesNotExistException} when no VehicleEntrantPayment was found.
   *
   * @param e Exception object
   */
  @ExceptionHandler(PaymentDoesNotExistException.class)
  ResponseEntity<PaymentStatusErrorsResponse> handleNoVehicleEntrantPaymentFoundException(
      PaymentDoesNotExistException e) {
    log.info("PaymentDoesNotExistException occurred", e);
    return ResponseEntity.badRequest()
        .body(PaymentStatusErrorsResponse.entrantNotFoundErrorResponse(e));
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
   * PaymentInfoDtoValidationException}.
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
   * Method to handle Exception when no VRN exists.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(PaymentInfoVrnValidationException.class)
  public ResponseEntity<PaymentInfoErrorsResponse> handlePaymentInfoNonExistentVrnExceptions(
      PaymentInfoVrnValidationException ex) {
    log.info("PaymentInfoVrnValidationException occurred: {}", ex);
    return ResponseEntity.badRequest().body(
        PaymentInfoErrorsResponse.singleValidationErrorResponse("vrn", ex.getMessage()));
  }

  /**
   * Method to handle Exception when max date range exceeded.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(PaymentInfoMaxDateRangeValidationException.class)
  public ResponseEntity<List<PaymentInfoErrorResponse>> handleMaxDateRangeExceededExceptions(
      PaymentInfoMaxDateRangeValidationException ex) {
    log.info("PaymentInfoMaxDateRangeValidationException occurred: {}", ex);
    List<PaymentInfoErrorResponse> errorsList = Lists
        .newArrayList(maxDateRangeErrorResponseWithField("fromDatePaidFor"),
            maxDateRangeErrorResponseWithField("toDatePaidFor"));

    return ResponseEntity.badRequest().body(errorsList);
  }

  /**
   * Method to handle Exception when no VRN exists.
   *
   * @param ex Exception object.
   */
  @ExceptionHandler(PaymentInfoPaymentMadeDateValidationException.class)
  public ResponseEntity<PaymentInfoErrorsResponse> handlePaymentInfoPaymentMadeDateWithConjunction(
      PaymentInfoPaymentMadeDateValidationException ex) {
    log.info("PaymentInfoPaymentMadeDateValidationException occurred: {}", ex);
    return ResponseEntity.badRequest().body(
        PaymentInfoErrorsResponse
            .singleValidationErrorResponse("paymentMadeDate", ex.getMessage()));
  }

  /**
   * Exception handler that handles exceptions thrown when an obligatory header is missing.
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<GenericErrorResponse> handleException(
      MissingRequestHeaderException e) {
    log.warn("Missing header: ", e);
    return ResponseEntity.badRequest()
        .body(createMissingHeaderErrorResponse(e));
  }

  /**
   * Exception handler that handles exceptions thrown when a type mismatch error occurs.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<GenericErrorResponse> handleException(
      MethodArgumentTypeMismatchException e) {
    log.warn("Argument type mismatch exception: ", e);
    return ResponseEntity.badRequest()
        .body(createTypeMismatchErrorResponse(e));
  }

  /**
   * Exception handler that returns 400 error on invalid input format.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity handleException(ConstraintViolationException exception) {
    log.warn("ConstraintViolationException occurred: ", exception);
    return ResponseEntity.badRequest().build();
  }

  /**
   * Exception handler that returns 400 error on invalid argument exception.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity handleException(IllegalArgumentException exception) {
    log.warn("IllegalArgumentException occurred: ", exception);
    return ResponseEntity.badRequest().body(exception.getMessage());
  }

  /**
   * Creates an instance of {@link GenericErrorResponse} based on {@link
   * MissingRequestHeaderException}.
   */
  private GenericErrorResponse createTypeMismatchErrorResponse(
      MethodArgumentTypeMismatchException e) {
    return GenericErrorResponse.builder()
        .message("Wrong format of '" + e.getName() + "'")
        .build();
  }

  /**
   * Creates an instance of {@link GenericErrorResponse} based on {@link
   * MissingRequestHeaderException}.
   */
  private GenericErrorResponse createMissingHeaderErrorResponse(
      MissingRequestHeaderException e) {
    return GenericErrorResponse.builder()
        .message("Missing request header '" + e.getHeaderName() + "'")
        .build();
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
    String field = null;
    String detail = messageSource.getMessage(error, LOCALE);
    if (error instanceof FieldError) {
      String[] fieldArray = ((FieldError) error).getField().split("\\.");
      field = fieldArray.length > 1 ? fieldArray[1] : fieldArray[0];
      detail = detail.contains(EMPTY) ? field + " " + detail : detail;
    }
    return ValidationError.builder()
        .detail(detail)
        .field(field)
        .title(messageSource.getMessage(validationCode, null, LOCALE));
  }
}
