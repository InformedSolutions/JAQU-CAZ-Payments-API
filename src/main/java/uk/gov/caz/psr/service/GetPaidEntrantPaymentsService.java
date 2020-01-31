package uk.gov.caz.psr.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;

/**
 * Class responsible for fetching PAID EntrantPayments for provided VRNs in provided CAZ ID and
 * returning it.
 */
@Service
@AllArgsConstructor
public class GetPaidEntrantPaymentsService {

  private final EntrantPaymentRepository entrantPaymentRepository;

  /**
   * Method receives a collection of VRNs, date range and CleanAirZone ID for which it is supposed
   * to fetch collection of PAID {@link EntrantPayment}, and then return information which VRNs in
   * provided CAZ has already paid for the entrance.
   *
   * @param vrns provided list of VRNs.
   * @param startDate first day in date range.
   * @param endDate last day in date range.
   * @param cleanAirZoneId CAZ in which the payments are checked.
   * @return Map with VRN as a key and list of {@link EntrantPayment} as value.
   */
  public Map<String,List<EntrantPayment>> getResults(List<String> vrns, LocalDate startDate,
      LocalDate endDate, UUID cleanAirZoneId) {
    Map<String, List<EntrantPayment>> results = new HashMap<>();

    for (String vrn : vrns) {
      List<EntrantPayment> paidEntrantPaymentsForVrn = entrantPaymentRepository
          .findAllPaidByVrnAndDateRangeAndCazId(vrn, startDate, endDate, cleanAirZoneId);

      if (!paidEntrantPaymentsForVrn.isEmpty()) {
        results.put(vrn, paidEntrantPaymentsForVrn);
      }
    }

    return results;
  }
}
