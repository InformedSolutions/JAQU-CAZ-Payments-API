package uk.gov.caz.psr.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;
import uk.gov.caz.psr.model.PaymentStatusAuditData;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo_;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.repository.jpa.EntrantPaymentMatchInfoRepository;


/**
 * Service returning payment history for VRN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehiclePaymentHistoryService {

  private final EntrantPaymentMatchInfoRepository entrantPaymentMatchInfoRepository;

  private final VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

  private final PaymentDetailRepository paymentDetailRepository;

  /**
   * Returns page of entrant items for a vehicle.
   *
   * @param vrn vehicle searched
   * @param pageNumber number of page requested
   * @param pageSize size of page to be returned
   * @return page of items
   */
  public Page<EntrantPaymentEnriched> paymentHistoryForVehicle(String vrn, int pageNumber,
      int pageSize) {
    Map<UUID, String> cleanAirZoneNameMap = vehicleComplianceRetrievalService
        .getCleanAirZoneIdToCleanAirZoneNameMap();

    Sort sort = Sort.by(Direction.DESC,
        "entrantPaymentInfo.travelDate", "paymentInfo.submittedTimestamp");

    Page<EntrantPaymentMatchInfo> pageWithOrderedIds = entrantPaymentMatchInfoRepository.findAll(
        createByVrnSpec(vrn),
        PageRequest.of(pageNumber, pageSize, sort)
    );

    Map<UUID, EntrantPaymentMatchInfo> entrantPaymentMatchById = getEntrantPaymentMatchInfoById(
        pageWithOrderedIds, sort);

    List<PaymentStatusAuditData> auditData = fetchPaymentStatusesFromAuditDataForVrn(vrn,
        entrantPaymentMatchById);

    Page<EntrantPaymentEnriched> enrichedPage = pageWithOrderedIds
        .map(EntrantPaymentMatchInfo::getId)
        .map(entrantPaymentMatchById::get)
        .map(matchInfo -> EntrantPaymentEnriched
            .fromMatchInfo(matchInfo, cleanAirZoneNameMap, auditData));
    return enrichedPage;
  }

  private List<PaymentStatusAuditData> fetchPaymentStatusesFromAuditDataForVrn(String vrn,
      Map<UUID, EntrantPaymentMatchInfo> entrantPaymentMatchById) {

    Set<UUID> cazIds = entrantPaymentMatchById.values().stream().map(
        entrantPaymentMatchInfo -> entrantPaymentMatchInfo.getEntrantPaymentInfo()
            .getCleanAirZoneId()).collect(Collectors.toSet());
    Set<LocalDate> travelDates = entrantPaymentMatchById.values().stream().map(
        entrantPaymentMatchInfo -> entrantPaymentMatchInfo.getEntrantPaymentInfo().getTravelDate())
        .collect(Collectors.toSet());
    Set<UUID> paymentIds = entrantPaymentMatchById.values().stream()
        .map(entrantPaymentMatchInfo -> entrantPaymentMatchInfo.getPaymentInfo().getId())
        .collect(Collectors.toSet());

    return paymentDetailRepository.getPaymentStatuses(vrn, cazIds, travelDates, paymentIds);
  }

  private Map<UUID, EntrantPaymentMatchInfo> getEntrantPaymentMatchInfoById(
      Page<EntrantPaymentMatchInfo> all, Sort sort) {
    if (all.isEmpty()) {
      return Collections.emptyMap();
    }
    return entrantPaymentMatchInfoRepository.findAll(findAllByIds(all), sort)
        .stream()
        .collect(Collectors.toMap(EntrantPaymentMatchInfo::getId, Function.identity()));
  }

  private Specification<EntrantPaymentMatchInfo> findAllByIds(Page<EntrantPaymentMatchInfo> all) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      root.fetch(EntrantPaymentMatchInfo_.entrantPaymentInfo);
      root.fetch(EntrantPaymentMatchInfo_.paymentInfo);
      return root.get("id").in(extractIds(all));
    };
  }

  private List<UUID> extractIds(Page<EntrantPaymentMatchInfo> all) {
    return all.getContent()
        .stream()
        .map(EntrantPaymentMatchInfo::getId)
        .collect(Collectors.toList());
  }

  private Specification<EntrantPaymentMatchInfo> createByVrnSpec(String vrn) {
    return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(
        root.get(EntrantPaymentMatchInfo_.entrantPaymentInfo).get("vrn"),
        vrn
    );
  }
}
