package uk.gov.caz.psr.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByDatesRequest;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorRequest;
import uk.gov.caz.psr.dto.historicalinfo.PaymentsInfoByOperatorResponse;
import uk.gov.caz.psr.model.info.byoperator.PaymentInfoByOperator;
import uk.gov.caz.psr.service.paymentinfo.bydates.PaymentsInfoByDatesService;
import uk.gov.caz.psr.service.paymentinfo.byoperator.PaymentsInfoByOperatorService;
import uk.gov.caz.psr.util.PaymentInfoByOperatorIdConverter;

@RestController
@AllArgsConstructor
@Slf4j
public class PaymentsInfoByOperatorController implements PaymentsInfoByOperatorControllerApiSpec {

  public static final String OPERATOR_HISTORY_PATH = "/v1/payments/operators-history/{operatorId}";
  public static final String OPERATORS_HISTORY_PATH = "/v1/payments/operators-history";

  private final PaymentsInfoByOperatorService byOperatorService;
  private final PaymentsInfoByDatesService byOperatorsService;

  @Override
  public ResponseEntity<PaymentsInfoByOperatorResponse> getPaymentsByOperatorId(String operatorId,
      PaymentsInfoByOperatorRequest request) {
    request.validateWith(operatorId);

    Page<PaymentInfoByOperator> paymentsByOperatorId = byOperatorService.getPaymentsByOperatorId(
        UUID.fromString(operatorId),
        request.getPageSize(),
        request.getPageNumber()
    );
    logPageInformation(operatorId, paymentsByOperatorId);

    return ResponseEntity.ok(PaymentInfoByOperatorIdConverter.from(paymentsByOperatorId,
        request.getPageSize()));
  }

  @Override
  public ResponseEntity<PaymentsInfoByOperatorResponse> getPaymentsByDates(
      PaymentsInfoByDatesRequest request) {
    request.validate();

    Page<PaymentInfoByOperator> paymentsByDates = byOperatorsService.getPaymentsByDates(
        request.getPageSize(),
        request.getPageNumber(),
        request.getLocalStartDate(),
        request.getLocalEndDate()
    );
    logPageInformationByDates(paymentsByDates);

    return ResponseEntity.ok(PaymentInfoByOperatorIdConverter.from(paymentsByDates,
        request.getPageSize()));
  }

  /**
   * Logs basic information about the retrieved page.
   */
  private void logPageInformation(String operatorId, Page<PaymentInfoByOperator> page) {
    log.info("Successfully got paginated info about payments made by operator '{}': page no {} "
            + "with {} items out of {}", operatorId, page.getNumber(), page.getNumberOfElements(),
        page.getTotalElements());
  }

  /**
   * Logs basic information about the retrieved page.
   */
  private void logPageInformationByDates(Page<PaymentInfoByOperator> page) {
    log.info("Successfully got paginated info about payments made by dates: page no {} "
            + "with {} items out of {}", page.getNumber(), page.getNumberOfElements(),
        page.getTotalElements());
  }
}
