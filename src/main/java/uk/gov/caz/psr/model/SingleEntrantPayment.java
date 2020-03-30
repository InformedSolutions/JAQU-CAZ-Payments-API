package uk.gov.caz.psr.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents a payment's parameters for a given CAZ entrant. Please note that this is
 * NOT a database entity.
 */
@Value
@Builder(toBuilder = true)
public class SingleEntrantPayment {

  String vrn;

  Integer charge;

  LocalDate travelDate;

  String tariffCode;
}
