package uk.gov.caz.psr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.VehicleEntrantPayment;
import uk.gov.caz.psr.repository.ExternalPaymentsRepository;
import uk.gov.caz.psr.repository.PaymentRepository;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
@Slf4j
public class InitiatePaymentService {

  private final PaymentRepository paymentRepository;
  private final ExternalPaymentsRepository externalPaymentsRepository;
  private final VehicleEntrantPaymentChargeCalculator chargeCalculator;

  /**
   * Creates Payment in GOV.UK PAY Inserts Payment details into database
   *
   * @param request A data which need to be used to create the payment.
   */
  public Payment createPayment(InitiatePaymentRequest request) {
    Payment payment = buildPayment(request);
    Payment paymentWithInternalId = paymentRepository.insertExternal(payment);
    Payment paymentWithExternalId = externalPaymentsRepository.create(paymentWithInternalId,
        request.getReturnUrl());
    paymentRepository.update(paymentWithExternalId);
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
    List<VehicleEntrantPayment> vehicleEntrantPayments = request.getDays()
        .stream()
        .map(day -> toVehicleEntrantPayment(day, request, chargePerDay))
        .collect(Collectors.toList());

    return Payment.builder()
        .externalPaymentStatus(ExternalPaymentStatus.INITIATED)
        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD)
        .totalPaid(request.getAmount())
        .vehicleEntrantPayments(vehicleEntrantPayments)
        .build();
  }

  /**
   * Maps a data from {@link InitiatePaymentRequest} to an instance of {@link
   * VehicleEntrantPayment}.
   */
  private VehicleEntrantPayment toVehicleEntrantPayment(LocalDate travelDate,
      InitiatePaymentRequest request, int chargePerDay) {
    return VehicleEntrantPayment.builder()
        .vrn(request.getVrn())
        .cleanZoneId(request.getCleanAirZoneId())
        .travelDate(travelDate)
        .chargePaid(chargePerDay)
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .build();
  }
}
