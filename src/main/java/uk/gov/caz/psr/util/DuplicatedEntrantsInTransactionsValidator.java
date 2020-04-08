package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.Transaction;

@Component
@Slf4j
public class DuplicatedEntrantsInTransactionsValidator {

  /**
   * Verifies if {@code request} does not contain duplicated vehicle entrants.
   */
  public static boolean containsNoDuplicatedEntrants(List<Transaction> transactions) {
    Map<String, List<LocalDate>> travelDatesByVrn = groupByVrn(transactions);
    for (Entry<String, List<LocalDate>> travelDatesForVrn : travelDatesByVrn.entrySet()) {
      if (travelDatesContainDuplicate(travelDatesForVrn)) {
        String vrn = travelDatesForVrn.getKey();
        log.warn("Duplicated travel date(s) detected for VRN '{}'", vrn);
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if {@code vrnWithTravelDates} contains at least one duplicated travel date.
   */
  private static boolean travelDatesContainDuplicate(
      Entry<String, List<LocalDate>> vrnWithTravelDates) {
    List<LocalDate> travelDatesWithPossibleDuplicates = vrnWithTravelDates.getValue();
    Set<LocalDate> uniqueTravelDates = toSet(travelDatesWithPossibleDuplicates);
    return travelDatesWithPossibleDuplicates.size() > uniqueTravelDates.size();
  }

  /**
   * Groups transactions by vrn and then each transactions is mapped to its travel date.
   */
  private static Map<String, List<LocalDate>> groupByVrn(List<Transaction> transactions) {
    return transactions
        .stream()
        .collect(groupingBy(Transaction::getVrn, mapping(Transaction::getTravelDate, toList())));
  }

  /**
   * Converts {@code travelDates} to a set of dates.
   */
  private static HashSet<LocalDate> toSet(List<LocalDate> travelDates) {
    return new HashSet<>(travelDates);
  }
}
