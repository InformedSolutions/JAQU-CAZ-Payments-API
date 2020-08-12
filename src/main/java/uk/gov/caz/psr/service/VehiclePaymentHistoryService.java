package uk.gov.caz.psr.service;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
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

  /**
   * Returns page of entrant items for a vehicle.
   * @param vrn vehicle searched
   * @param pageNumber number of page requested
   * @param pageSize size of page to be returned
   * @return page of items
   */
  public Page<EntrantPaymentEnriched> paymentHistoryForVehicle(String vrn, int pageNumber,
      int pageSize) {
    Map<UUID, String> cleanAirZoneNameMap = vehicleComplianceRetrievalService
        .getCleanAirZoneIdToCleanAirZoneNameMap();
    Sort sort = Sort.by("entrantPaymentInfo.travelDate", "paymentInfo.submittedTimestamp");
    Page<EntrantPaymentMatchInfo> all = entrantPaymentMatchInfoRepository
        .findByEntrantPaymentInfo_vrn(vrn, PageRequest.of(pageNumber, pageSize, sort));
    Page<EntrantPaymentEnriched> enrichedPage = all.map(
        matchInfo -> EntrantPaymentEnriched.fromMatchInfo(matchInfo, cleanAirZoneNameMap));
    return enrichedPage;
  }

}
