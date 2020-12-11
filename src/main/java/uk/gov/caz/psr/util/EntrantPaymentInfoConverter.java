package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentMethod;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentInfoResponseV1;
import uk.gov.caz.psr.dto.PaymentInfoResponseV1.PaymentsInfo;
import uk.gov.caz.psr.dto.PaymentInfoResponseV1.SinglePaymentInfo;
import uk.gov.caz.psr.dto.PaymentInfoResponseV2;
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.PaymentInfo;

/**
 * A utility class that converts a collection of {@link EntrantPaymentInfo} into {@link
 * PaymentInfoResponseV1}.
 */
@Component
@AllArgsConstructor
public class EntrantPaymentInfoConverter {

  private final CurrencyFormatter currencyFormatter;

  /**
   * Converts the passed {@code entrantPaymentMatchInfos} into an instance of {@link
   * PaymentInfoResponseV1}.
   *
   * @param entrantPaymentMatchInfos A collection of {@link EntrantPaymentInfo}.
   * @return An instance of {@link PaymentInfoResponseV1}.
   */
  public PaymentInfoResponseV1 toPaymentInfoResponse(
      Collection<EntrantPaymentMatchInfo> entrantPaymentMatchInfos) {
    Preconditions.checkNotNull(entrantPaymentMatchInfos, "entrantPaymentMatchInfos cannot be null");

    List<PaymentsInfo> paymentsInfo = groupByVrnAndPayment(entrantPaymentMatchInfos)
        .entrySet()
        .stream()
        .map(vrnWithVehicleEntrantPayments -> toPaymentsInfo(
            vrnWithVehicleEntrantPayments.getKey(),
            vrnWithVehicleEntrantPayments.getValue())
        ).collect(toList());
    return new PaymentInfoResponseV1(paymentsInfo);
  }

  /**
   * Converts the passed {@code entrantPaymentMatchInfos} into an instance of {@link
   * PaymentInfoResponseV2}.
   *
   * @param entrantPaymentMatchInfos A collection of {@link EntrantPaymentInfo}.
   * @return An instance of {@link PaymentInfoResponseV2}.
   */
  public PaymentInfoResponseV2 toPaymentInfoResponseV2(
      Page<EntrantPaymentMatchInfo> entrantPaymentMatchInfos) {
    Preconditions.checkNotNull(entrantPaymentMatchInfos, "entrantPaymentMatchInfos cannot be null");

    List<PaymentsInfo> paymentsInfo = groupByVrnAndPayment(entrantPaymentMatchInfos.getContent())
        .entrySet()
        .stream()
        .map(vrnWithVehicleEntrantPayments -> toPaymentsInfo(
            vrnWithVehicleEntrantPayments.getKey(),
            vrnWithVehicleEntrantPayments.getValue()))
        .sorted(Comparator.comparing(PaymentsInfo::getVrn))
        .collect(toList());
    return new PaymentInfoResponseV2(paymentsInfo, entrantPaymentMatchInfos.getTotalPages());
  }

  /**
   * Creates an instance of {@link PaymentsInfo} from passed {@code vrn} and {@code
   * vrnWithVehicleEntrantPayments}.
   */
  private PaymentsInfo toPaymentsInfo(String vrn, Map<PaymentInfo,
      List<EntrantPaymentInfo>> vrnWithVehicleEntrantPayments) {
    return new PaymentsInfo(vrn, toSinglePaymentInfos(vrnWithVehicleEntrantPayments));
  }

  /**
   * Creates a list of {@link SinglePaymentInfo} from passed {@code vrnWithVehicleEntrantPayments}.
   */
  private List<SinglePaymentInfo> toSinglePaymentInfos(
      Map<PaymentInfo, List<EntrantPaymentInfo>> vrnWithVehicleEntrantPayments) {
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
      List<EntrantPaymentInfo> entrantPaymentInfoList) {
    return SinglePaymentInfo.builder()
        .cazPaymentReference(paymentInfo.getReferenceNumber())
        .paymentDate(toLocalDate(paymentInfo.getSubmittedTimestamp()))
        .paymentProviderId(paymentInfo.getExternalId())
        .totalPaid(currencyFormatter.parsePenniesToBigDecimal(paymentInfo.getTotalPaid()))
        .lineItems(toVehicleEntrantPayments(entrantPaymentInfoList))
        .paymentMethod(ChargeSettlementPaymentMethod.from(paymentInfo.getPaymentMethod()))
        .paymentMandateId(PaymentMethod.DIRECT_DEBIT.equals(paymentInfo.getPaymentMethod())
            ? paymentInfo.getPaymentProviderMandateId()
            : null)
        .telephonePayment(paymentInfo.isTelephonePayment())
        .build();
  }

  /**
   * Maps the provided {@code timestamp} to a date provided it is non null. Returns null otherwise.
   */
  private LocalDate toLocalDate(LocalDateTime timestamp) {
    return timestamp == null ? null
        : timestamp.atZone(ZoneId.of("GMT")).withZoneSameInstant(ZoneId.of("Europe/London"))
            .toLocalDate();
  }

  /**
   * Creates a list of {@link SinglePaymentInfo.VehicleEntrantPaymentInfo} from passed {@code
   * entrantPaymentInfoList}.
   */
  private List<SinglePaymentInfo.VehicleEntrantPaymentInfo> toVehicleEntrantPayments(
      List<EntrantPaymentInfo> entrantPaymentInfoList) {
    return entrantPaymentInfoList.stream()
        .map(this::toVehicleEntrantPaymentInfo)
        .collect(toList());
  }

  /**
   * Creates an instance of {@link SinglePaymentInfo.VehicleEntrantPaymentInfo} from passed {@code
   * entrantPaymentInfo}.
   */
  private SinglePaymentInfo.VehicleEntrantPaymentInfo toVehicleEntrantPaymentInfo(
      EntrantPaymentInfo entrantPaymentInfo) {
    return SinglePaymentInfo.VehicleEntrantPaymentInfo
        .builder()
        .travelDate(entrantPaymentInfo.getTravelDate())
        .caseReference(entrantPaymentInfo.getCaseReference())
        .chargePaid(currencyFormatter.parsePenniesToBigDecimal(
            entrantPaymentInfo.getChargePaid()))
        .paymentStatus(ChargeSettlementPaymentStatus
            .from(entrantPaymentInfo.getPaymentStatus()))
        .build();
  }

  /**
   * Groups the passed {@code entrantPaymentInfos} by vrn and payment info and return the result as
   * a map.
   */
  private Map<String, Map<PaymentInfo, List<EntrantPaymentInfo>>> groupByVrnAndPayment(
      Collection<EntrantPaymentMatchInfo> entrantPaymentInfos) {
    return entrantPaymentInfos
        .stream()
        .collect(
            groupingBy(e -> e.getEntrantPaymentInfo().getVrn(),
                groupingBy(EntrantPaymentMatchInfo::getPaymentInfo,
                    mapping(EntrantPaymentMatchInfo::getEntrantPaymentInfo, toList())
                )
            )
        );
  }
}
