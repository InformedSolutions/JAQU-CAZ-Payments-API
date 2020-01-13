
package uk.gov.caz.psr.service;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
public class InitiatePaymentService {

  private final VehicleEntrantPaymentChargeCalculator chargeCalculator;
  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final PaymentRepository paymentRepository;

  /**
   * Creates Payment in GOV.UK PAY Inserts Payment details into database.
   *
   * @param request A data which need to be used to create the payment.
   */
  @Transactional
  public Payment createPayment(InitiatePaymentRequest request) {
    Payment payment = buildPayment(request);
    Payment paymentWithInternalId = paymentRepository.insertWithExternalStatus(payment);
    Payment paymentWithExternalId = externalPaymentsRepository.create(paymentWithInternalId,
        request.getReturnUrl());
    paymentRepository.update(paymentWithExternalId);

    // TODO case when EntrantPayment exists
    return paymentWithExternalId;
  }

  /**
   * Builds Payment object based on request data.
   *
   * @param request A data which need to be used to create the payment.
   */

  private Payment buildPayment(InitiatePaymentRequest request) {
    int chargePerDay = chargeCalculator.calculateCharge(request.getAmount(),
        request.getDays().size());
    List<CazEntrantPayment> vehicleEntrantPayments = request.getDays()
        .stream()
        .map(day -> toEntrantPayment(day, request, chargePerDay))
        .collect(Collectors.toList());

    return Payment.builder()
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .totalPaid(request.getAmount())
        .cazEntrantPayments(vehicleEntrantPayments).build();
  }

  /**
   * Maps a data from {@link InitiatePaymentRequest} to an instance of
   * {@link CazEntrantPayment}.
   */
  private CazEntrantPayment toEntrantPayment(LocalDate travelDate,
      InitiatePaymentRequest request, int chargePerDay) {
    return CazEntrantPayment.builder()
        .vrn(normalizeVrn(request.getVrn()))
        .cleanAirZoneId(request.getCleanAirZoneId())
        .travelDate(travelDate)
        .charge(chargePerDay)
        .updateActor(EntrantPaymentUpdateActor.USER)
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .tariffCode(request.getTariffCode())
        .build();
  }
}
