package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentInfoResponse.PaymentsInfo;
import uk.gov.caz.psr.dto.PaymentInfoResponse.SinglePaymentInfo;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;

/**
 * A utility class that converts a collection of {@link VehicleEntrantPaymentInfo} into {@link
 * PaymentInfoResponse}.
 */
@Component
@AllArgsConstructor
public class VehicleEntrantPaymentInfoConverter {

  private final CurrencyFormatter currencyFormatter;

  /**
   * Converts the passed {@code vehicleEntrantPaymentInfos} into an instance of {@link
   * PaymentInfoResponse}.
   *
   * @param vehicleEntrantPaymentInfos A collection of {@link VehicleEntrantPaymentInfo}.
   * @return An instance of {@link PaymentInfoResponse}.
   */
  public PaymentInfoResponse toPaymentInfoResponse(
      Collection<VehicleEntrantPaymentInfo> vehicleEntrantPaymentInfos) {
    Preconditions.checkNotNull(vehicleEntrantPaymentInfos,
        "vehicleEntrantPaymentInfos cannot be null");

    List<PaymentsInfo> paymentsInfo = groupByVrnAndPayment(vehicleEntrantPaymentInfos)
        .entrySet()
        .stream()
        .map(vrnWithVehicleEntrantPayments -> toPaymentsInfo(
            vrnWithVehicleEntrantPayments.getKey(),
            vrnWithVehicleEntrantPayments.getValue())
        ).collect(toList());
    return new PaymentInfoResponse(paymentsInfo);
  }

  /**
   * Creates an instance of {@link PaymentsInfo} from passed {@code vrn} and {@code
   * vrnWithVehicleEntrantPayments}.
   */
  private PaymentsInfo toPaymentsInfo(String vrn, Map<PaymentInfo,
      List<VehicleEntrantPaymentInfo>> vrnWithVehicleEntrantPayments) {
    return new PaymentsInfo(vrn, toSinglePaymentInfos(vrnWithVehicleEntrantPayments));
  }

  /**
   * Creates a list of {@link SinglePaymentInfo} from passed {@code vrnWithVehicleEntrantPayments}.
   */
  private List<SinglePaymentInfo> toSinglePaymentInfos(
      Map<PaymentInfo, List<VehicleEntrantPaymentInfo>> vrnWithVehicleEntrantPayments) {
    return vrnWithVehicleEntrantPayments
        .entrySet()
        .stream()
        .map(paymentWithVehicleEntrantPayments -> toSinglePaymentInfo(
            paymentWithVehicleEntrantPayments.getKey(),
            paymentWithVehicleEntrantPayments.getValue())
        ).collect(toList());
  }

  /**
   * Creates an instance of {@link SinglePaymentInfo} from passed {@code paymentInfo} and {@code
   * entrantPaymentInfoList}.
   */
  private SinglePaymentInfo toSinglePaymentInfo(PaymentInfo paymentInfo,
      List<VehicleEntrantPaymentInfo> entrantPaymentInfoList) {
    return SinglePaymentInfo.builder()
        .paymentDate(paymentInfo.getSubmittedTimestamp().toLocalDate())
        .paymentProviderId(paymentInfo.getExternalId())
        .totalPaid(currencyFormatter.parsePenniesToBigDecimal(paymentInfo.getTotalPaid()))
        .lineItems(toVehicleEntrantPayments(entrantPaymentInfoList))
        .build();
  }

  /**
   * Creates a list of {@link SinglePaymentInfo.VehicleEntrantPaymentInfo} from passed {@code
   * entrantPaymentInfoList}.
   */
  private List<SinglePaymentInfo.VehicleEntrantPaymentInfo> toVehicleEntrantPayments(
      List<VehicleEntrantPaymentInfo> entrantPaymentInfoList) {
    return entrantPaymentInfoList.stream()
        .map(this::toVehicleEntrantPaymentInfo)
        .collect(toList());
  }

  /**
   * Creates an instance of {@link SinglePaymentInfo.VehicleEntrantPaymentInfo} from passed {@code
   * vehicleEntrantPaymentInfo}.
   */
  private SinglePaymentInfo.VehicleEntrantPaymentInfo toVehicleEntrantPaymentInfo(
      VehicleEntrantPaymentInfo vehicleEntrantPaymentInfo) {
    return SinglePaymentInfo.VehicleEntrantPaymentInfo
        .builder()
        .travelDate(vehicleEntrantPaymentInfo.getTravelDate())
        .caseReference(vehicleEntrantPaymentInfo.getCaseReference())
        .chargePaid(currencyFormatter.parsePenniesToBigDecimal(
            vehicleEntrantPaymentInfo.getChargePaid()))
        .chargeSettlementPaymentStatus(ChargeSettlementPaymentStatus
            .from(vehicleEntrantPaymentInfo.getPaymentStatus()))
        .build();
  }

  /**
   * Groups the passed {@code vehicleEntrantPaymentInfos} by vrn and payment info and return the
   * result as a map.
   */
  private Map<String, Map<PaymentInfo, List<VehicleEntrantPaymentInfo>>> groupByVrnAndPayment(
      Collection<VehicleEntrantPaymentInfo> vehicleEntrantPaymentInfos) {
    return vehicleEntrantPaymentInfos
        .stream()
        .collect(
            groupingBy(VehicleEntrantPaymentInfo::getVrn,
                groupingBy(VehicleEntrantPaymentInfo::getPaymentInfo)
            )
        );
  }
}
