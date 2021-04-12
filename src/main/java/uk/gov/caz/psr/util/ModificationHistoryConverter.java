package uk.gov.caz.psr.util;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.experimental.UtilityClass;
import uk.gov.caz.psr.dto.ModificationHistoryDetails;
import uk.gov.caz.psr.model.PaymentModification;

/**
 * A utility class that converts a list of {@link PaymentModification} to a list of {@link
 * ModificationHistoryDetails}.
 */
@UtilityClass
public class ModificationHistoryConverter {

  private static final ZoneId GMT_ZONE_ID = ZoneId.of("GMT");
  private static final ZoneId UK_ZONE_ID = ZoneId.of("Europe/London");

  /**
   * Performs conversion of the received list of {@link PaymentModification} to a list of {@link
   * ModificationHistoryDetails}.
   */
  public static List<ModificationHistoryDetails> toModificationHistory(
      List<PaymentModification> paymentModifications) {
    return paymentModifications.stream()
        .map(paymentModification -> toModificationHistoryDetails(paymentModification))
        .collect(toList());
  }

  private static ModificationHistoryDetails toModificationHistoryDetails(
      PaymentModification paymentModification) {
    return ModificationHistoryDetails.builder()
        .amount(paymentModification.getAmount())
        .travelDate(paymentModification.getTravelDate())
        .vrn(paymentModification.getVrn())
        .caseReference(paymentModification.getCaseReference())
        .modificationTimestamp(
            LocalDateTime.from(paymentModification.getModificationTimestamp()).atZone(GMT_ZONE_ID)
                .withZoneSameInstant(UK_ZONE_ID).toLocalDateTime())
        .entrantPaymentStatus(paymentModification.getEntrantPaymentStatus())
        .build();
  }
}
