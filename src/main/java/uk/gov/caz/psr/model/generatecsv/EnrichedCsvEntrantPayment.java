package uk.gov.caz.psr.model.generatecsv;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that enriches {@link CsvEntrantPayment} with data coming from Accounts API and VCCS API.
 */
@Value
@Builder
public class EnrichedCsvEntrantPayment {

  /**
   * Id of the payment.
   */
  UUID paymentId;

  /**
   * Date of the payment.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate dateOfPayment;

  /**
   * Name of the payer.
   */
  String paymentMadeBy;

  /**
   * Name of CleanAirZone.
   */
  String cazName;

  /**
   * Vehicle registration number.
   */
  String vrn;

  /**
   * Date of the entry.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate dateOfEntry;

  /**
   * Charge amount.
   */
  Integer charge;

  /**
   * Payment reference number.
   */
  String paymentReference;

  /**
   * Payment provider ID (GOV.UK.PAY).
   */
  String paymentProviderId;

  /**
   * Quantity of the payed entries to CAZ.
   */
  Integer entriesCount;

  /**
   * Total amount paid.
   */
  Integer totalPaid;

  /**
   * Status of EntrantPayment.
   */
  String status;

  /**
   * Date received from local authority.
   */
  LocalDate dateReceivedFromLa;

  /**
   * Case Reference number.
   */
  String caseReference;
}
