package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentDetailsResponse;
import uk.gov.caz.psr.dto.PaymentDetailsResponse.VehicleEntrantPaymentDetails;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentModification;
import uk.gov.caz.psr.service.AccountService;

/**
 * A utility class that converts {@link Payment} into {@link PaymentDetailsResponse}.
 */
@Component
@AllArgsConstructor
public class PaymentDetailsConverter {

  private final CurrencyFormatter currencyFormatter;
  private final AccountService accountService;

  /**
   * Converts the passed {@link Payment} into an instance of {@link PaymentDetailsResponse}.
   *
   * @param payment {@link Payment}
   * @return An instance of {@link PaymentDetailsResponse}.
   */
  public PaymentDetailsResponse toPaymentDetailsResponse(Payment payment,
      List<PaymentModification> paymentModifications) {
    return PaymentDetailsResponse.builder()
        .centralPaymentReference(payment.getReferenceNumber())
        .paymentProviderId(payment.getExternalId())
        .paymentDate(toLocalDate(payment.getSubmittedTimestamp()))
        .telephonePayment(payment.isTelephonePayment())
        .payerName(accountService.getPayerName(payment.getUserId()))
        .totalPaid(currencyFormatter.parsePenniesToBigDecimal(payment.getTotalPaid()))
        .lineItems(toVehicleEntrantPaymentsDetails(payment.getEntrantPayments()))
        .modificationHistory(
            ModificationHistoryConverter.toModificationHistory(paymentModifications))
        .build();
  }

  private List<VehicleEntrantPaymentDetails> toVehicleEntrantPaymentsDetails(
      List<EntrantPayment> entrantPaymentInfoList) {
    return entrantPaymentInfoList.stream()
        .map(this::toVehicleEntrantPaymentDetails)
        .collect(toList());
  }

  private PaymentDetailsResponse.VehicleEntrantPaymentDetails toVehicleEntrantPaymentDetails(
      EntrantPayment entrantPayment) {
    return PaymentDetailsResponse.VehicleEntrantPaymentDetails.builder()
        .caseReference(entrantPayment.getCaseReference())
        .chargePaid(currencyFormatter.parsePenniesToBigDecimal(
            entrantPayment.getCharge()))
        .paymentStatus(ChargeSettlementPaymentStatus
            .from(entrantPayment.getInternalPaymentStatus()))
        .travelDate(entrantPayment.getTravelDate())
        .vrn(entrantPayment.getVrn())
        .build();
  }

  /**
   * Maps the provided {@code timestamp} to a date provided it is non null. Returns null otherwise.
   */
  private LocalDate toLocalDate(LocalDateTime timestamp) {
    return timestamp == null ? null : timestamp.toLocalDate();
  }
}