package uk.gov.caz.psr.util;

import org.springframework.stereotype.Component;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentStatusUpdate;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;

/**
 * Utility class that creates new instance of {@link EntrantPayment} using attributes provided in
 * {@link EntrantPaymentStatusUpdate} and default attributes which are associated with update-by-LA
 * context.
 */
@Component
public class EntrantPaymentStatusUpdateConverter {

  /**
   * Creates new instance of {@link EntrantPayment} based on attributes provided
   * in {@link EntrantPaymentStatusUpdate}.
   *
   * @param statusUpdate from which initial attributes are taken
   * @return {@link EntrantPayment}
   */
  public EntrantPayment convert(EntrantPaymentStatusUpdate statusUpdate) {
    return EntrantPayment.builder()
        .vrn(statusUpdate.getVrn())
        .cleanAirZoneId(statusUpdate.getCleanAirZoneId())
        .travelDate(statusUpdate.getDateOfCazEntry())
        .vehicleEntrantCaptured(false)
        .updateActor(EntrantPaymentUpdateActor.LA)
        .internalPaymentStatus(statusUpdate.getPaymentStatus())
        .caseReference(statusUpdate.getCaseReference())
        .charge(0)
        .build();
  }
}
