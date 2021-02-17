package uk.gov.caz.psr.service.paymentinfo.byoperator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentModificationStatus;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.byoperator.PaymentInfoByOperator;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.jpa.EntrantPaymentMatchInfoRepository;
import uk.gov.caz.psr.service.CleanAirZoneService;

/**
 * Service class that retrieves paginated information from T_PAYMENT and
 * T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT tables about payments made by the given operator.
 */
@Service
@AllArgsConstructor
class PaginatedPaymentsInfoByOperatorDetailsFetcher {

  private final EntrantPaymentMatchInfoRepository entrantPaymentMatchInfoRepository;
  private final PaymentDetailRepository paymentDetailRepository;
  private final CleanAirZoneService cleanAirZoneService;

  /**
   * Retrieves paginated information from T_PAYMENT and T_CLEAN_AIR_ZONE_ENTRANT_PAYMENT tables
   * about the passed payments.
   */
  public List<PaymentInfoByOperator> fetchDetailsFor(List<PaymentInfo> paymentInfos) {
    if (paymentInfos.isEmpty()) {
      return Collections.emptyList();
    }

    Map<UUID, CleanAirZoneDto> cleanAirZonesById = fetchCleanAirZonesById();
    Map<UUID, List<EntrantPaymentMatchInfo>> matchingEntrantInfosByPaymentId =
        findMatchingInfoByPaymentId(paymentInfos);
    Map<UUID, List<PaymentModificationStatus>> matchingPaymentModificationStatuses =
        findPaymentModifiedStatusesByPaymentId(paymentInfos);
    return paymentInfos.stream()
        .map(paymentInfo -> toPaymentInfoByOperator(paymentInfo, cleanAirZonesById,
            matchingEntrantInfosByPaymentId, matchingPaymentModificationStatuses))
        .collect(Collectors.toList());
  }

  /**
   * Converts {@code paymentInfo} to an instance of {@link PaymentInfoByOperator}.
   */
  private PaymentInfoByOperator toPaymentInfoByOperator(
      PaymentInfo paymentInfo,
      Map<UUID, CleanAirZoneDto> cleanAirZoneById,
      Map<UUID, List<EntrantPaymentMatchInfo>> matchingEntrantInfos,
      Map<UUID, List<PaymentModificationStatus>> matchingPaymentModificationStatuses) {
    List<EntrantPaymentMatchInfo> entrantPaymentMatchInfos = matchingEntrantInfos
        .get(paymentInfo.getId());
    return PaymentInfoByOperator.builder()
        .cazName(getCazNameFromAnyMatchingEntrant(cleanAirZoneById, entrantPaymentMatchInfos))
        .paymentId(paymentInfo.getId())
        .paymentProviderStatus(paymentInfo.getExternalPaymentStatus().toString())
        .paymentReference(paymentInfo.getReferenceNumber())
        .paymentTimestamp(paymentInfo.getInsertTimestamp())
        .totalPaid(paymentInfo.getTotalPaid())
        .isChargedback(
            paymentHadModifiedStatus(paymentInfo.getId(), matchingPaymentModificationStatuses,
                InternalPaymentStatus.CHARGEBACK))
        .isRefunded(
            paymentHadModifiedStatus(paymentInfo.getId(), matchingPaymentModificationStatuses,
                InternalPaymentStatus.REFUNDED))
        .vrns(entrantPaymentMatchInfos.stream()
            .map(EntrantPaymentMatchInfo::getEntrantPaymentInfo)
            .map(EntrantPaymentInfo::getVrn)
            .collect(Collectors.toSet())
        )
        .build();
  }

  /**
   * Returns the matching CAZ name for any entrant payment match (they all are from the same CAZ).
   */
  private String getCazNameFromAnyMatchingEntrant(Map<UUID, CleanAirZoneDto> cleanAirZoneById,
      List<EntrantPaymentMatchInfo> entrantPaymentMatchInfos) {
    EntrantPaymentInfo anyMatchingEntrantInfo = getAnyMatchingEntrantInfo(entrantPaymentMatchInfos);
    CleanAirZoneDto cleanAirZoneData = getCleanAirZoneDataByCazId(cleanAirZoneById,
        anyMatchingEntrantInfo);
    return cleanAirZoneData.getName();
  }

  /**
   * Returns the matching CAZ for the given {@code anyMatchingEntrantInfo}.
   */
  private CleanAirZoneDto getCleanAirZoneDataByCazId(Map<UUID, CleanAirZoneDto> cleanAirZoneById,
      EntrantPaymentInfo anyMatchingEntrantInfo) {
    return cleanAirZoneById.get(anyMatchingEntrantInfo.getCleanAirZoneId());
  }

  /**
   * Returns the first element of {@code entrantPaymentMatchInfos}.
   */
  private EntrantPaymentInfo getAnyMatchingEntrantInfo(
      List<EntrantPaymentMatchInfo> entrantPaymentMatchInfos) {
    return entrantPaymentMatchInfos.iterator().next().getEntrantPaymentInfo();
  }

  /**
   * Gets information about all Clean Air Zones from an external source.
   */
  private Map<UUID, CleanAirZoneDto> fetchCleanAirZonesById() {
    return cleanAirZoneService.fetchAll()
        .getBody()
        .getCleanAirZones()
        .stream()
        .collect(Collectors.toMap(CleanAirZoneDto::getCleanAirZoneId, Function.identity()));
  }

  /**
   * For the given payment IDs from {@code paymentInfos} finds all entrant payment matches alongside
   * with payment info and entrant payment info. The result is grouped by payment id.
   */
  private Map<UUID, List<EntrantPaymentMatchInfo>> findMatchingInfoByPaymentId(
      List<PaymentInfo> paymentInfos) {
    return entrantPaymentMatchInfoRepository.findAll(matchingEntrantPaymentsSpec(paymentInfos))
        .stream()
        .collect(Collectors.groupingBy(entrantMatch -> entrantMatch.getPaymentInfo().getId()));
  }

  /**
   * For the given payment IDs from {@code paymentInfos} finds all entrant payment matches alongside
   * with payment info and entrant payment info. The result is grouped by payment id.
   */
  private Map<UUID, List<PaymentModificationStatus>> findPaymentModifiedStatusesByPaymentId(
      List<PaymentInfo> paymentInfos) {
    Set<UUID> paymentIds = extractPaymentIdsFrom(paymentInfos);
    List<PaymentModificationStatus> paymentModificationStatuses = paymentDetailRepository
        .getPaymentStatusesForPaymentIds(paymentIds,
            EntrantPaymentUpdateActor.LA,
            Arrays.asList(InternalPaymentStatus.REFUNDED, InternalPaymentStatus.CHARGEBACK));
    return paymentModificationStatuses.stream()
        .collect(Collectors.groupingBy(PaymentModificationStatus::getPaymentId));
  }

  /**
   * Creates a specification that eagerly fetches entrant payment info and payment info for the
   * matched entrant payment match record.
   */
  private Specification<EntrantPaymentMatchInfo> matchingEntrantPaymentsSpec(
      List<PaymentInfo> paymentInfos) {
    Set<UUID> paymentInfosIds = extractPaymentIdsFrom(paymentInfos);
    return (root, criteriaQuery, criteriaBuilder) -> {
      root.fetch(EntrantPaymentMatchInfo_.entrantPaymentInfo);
      root.fetch(EntrantPaymentMatchInfo_.paymentInfo);
      return root.get("paymentInfo").get("id").in(paymentInfosIds);
    };
  }

  /**
   * Extracts IDs from the passed {@code paymentInfos} and maps them to a set.
   */
  private Set<UUID> extractPaymentIdsFrom(List<PaymentInfo> paymentInfos) {
    return paymentInfos.stream()
        .map(PaymentInfo::getId)
        .collect(Collectors.toSet());
  }

  private boolean paymentHadModifiedStatus(UUID paymentId,
      Map<UUID, List<PaymentModificationStatus>> matchingPaymentModificationStatuses,
      InternalPaymentStatus expectedModificationStatus) {

    if (matchingPaymentModificationStatuses.containsKey(paymentId)) {
      return matchingPaymentModificationStatuses.get(paymentId).stream()
          .anyMatch(paymentModificationStatus -> paymentModificationStatus.getPaymentStatus()
              .equals(expectedModificationStatus));
    } else {
      return false;
    }
  }
}
