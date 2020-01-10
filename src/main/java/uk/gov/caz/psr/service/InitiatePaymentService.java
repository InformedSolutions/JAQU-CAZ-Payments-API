
package uk.gov.caz.psr.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.dto.InitiatePaymentRequest;
import uk.gov.caz.psr.model.Payment;

/**
 * A service which is responsible creating Payment in GOV.UK PAY
 */
@Service
@AllArgsConstructor
public class InitiatePaymentService {

  //  private final VehicleEntrantPaymentChargeCalculator chargeCalculator;
  //  private final ExternalPaymentsRepository externalPaymentsRepository;
  //  private final PaymentRepository paymentRepository;

  /**
   * Creates Payment in GOV.UK PAY Inserts Payment details into database
   *
   * @param request A data which need to be used to create the payment.
   */

  public Payment createPayment(InitiatePaymentRequest request) {
    //    TODO: Fix with the payment updates CAZ-1716
    //    Payment payment = buildPayment(request);
    //    Payment paymentWithInternalId = paymentRepository.insertWithExternalStatus(payment);
    //    Payment paymentWithExternalId =
    //        externalPaymentsRepository.create(paymentWithInternalId, request.getReturnUrl());
    //    paymentRepository.update(paymentWithExternalId);
    //    return paymentWithExternalId;
    return Payment.builder().totalPaid(request.getAmount()).build();
  }

  //  /**
  //   * Builds Payment object based on request data.
  //   *
  //   * @param request A data which need to be used to create the payment.
  //   */
  //
  //  private Payment buildPayment(InitiatePaymentRequest request) {
  //    int chargePerDay =
  //        chargeCalculator.calculateCharge(request.getAmount(), request.getDays().size());
  //    List<VehicleEntrantPayment> vehicleEntrantPayments =
  //        request.getDays().stream()
  //            .map(day -> toVehicleEntrantPayment(day, request, chargePerDay))
  //            .collect(Collectors.toList());
  //
  //    return Payment.builder().externalPaymentStatus(ExternalPaymentStatus.INITIATED)
  //        .paymentMethod(PaymentMethod.CREDIT_DEBIT_CARD).totalPaid(request.getAmount())
  //        .vehicleEntrantPayments(vehicleEntrantPayments).build();
  //  }
  //
  //  /**
  //   * Maps a data from {@link InitiatePaymentRequest} to an instance of
  //   * {@link VehicleEntrantPayment}.
  //   */
  //  private VehicleEntrantPayment toVehicleEntrantPayment(LocalDate travelDate,
  //      InitiatePaymentRequest request, int chargePerDay) {
  //    return VehicleEntrantPayment.builder()
  //        .vrn(normalizeVrn(request.getVrn()))
  //        .cleanZoneId(request.getCleanAirZoneId())
  //        .travelDate(travelDate)
  //        .chargePaid(chargePerDay)
  //        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
  //        .build();
  //  }
}
