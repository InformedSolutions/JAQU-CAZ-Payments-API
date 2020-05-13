package uk.gov.caz.psr.model;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT} table.
 */
@Value
@Builder(toBuilder = true)
public class Payment {

  /**
   * The internal unique payment identifier.
   */
  UUID id;

  /**
   * The unique payment identifier from GOV UK Pay service.
   */
  String externalId;

  /**
   * The central reference number of the payment.
   */
  Long referenceNumber;

  /**
   * The method of payment.
   */
  @NonNull
  PaymentMethod paymentMethod;

  /**
   * The amount of money paid for the chosen days.
   */
  @NonNull
  Integer totalPaid;

  /**
   * A list of {@link EntrantPayment} instances associated with this object. It can be empty in
   * cases when we don't want to eagerly fetch all associated entities with this payment.
   */
  @NonNull
  List<EntrantPayment> entrantPayments;

  /**
   * Status of the payment as stored in GOV UK Pay.
   */
  ExternalPaymentStatus externalPaymentStatus;

  /**
   * A timestamp indicating the date/time on which a payment was submitted to GOV UK for
   * authorisation/processing.
   */
  LocalDateTime submittedTimestamp;

  /**
   * A timestamp indicating the date/time on which a payment was authorised by GOV.UK Pay, i.e. a
   * 'SUCCESS' response is returned by GOV UK Pay.
   */
  LocalDateTime authorisedTimestamp;

  /**
   * A URL which is returned from the GOV UK Pay service to continue the payment journey. A
   * transient field, not saved in the database.
   */
  String nextUrl;

  /**
   * The email address which is returned from the GOV UK Pay service to send a receipt of the
   * payment. A transient field, not saved in the database.
   */
  String emailAddress;

  /**
   * An identifier of the Clean Air Zone. A transient field, not saved in the database. This value
   * is non-null only when a new payment is initiated.
   */
  UUID cleanAirZoneId;

  /**
   * An identifier of the account/user which is making the payment. {@code null} for citizen
   * journeys.
   */
  UUID userId;

  /**
   * An Provider Mandate ID from Accounts API.
   */
  String paymentProviderMandateId;

  /**
   * An overridden lombok's builder.
   */
  public static class PaymentBuilder {

    /**
     * Builds the payment object.
     *
     * @return An instance of {@link Payment}.
     */
    public Payment build() {
      Preconditions.checkState(externalStatusMatchesExternalPaymentId(),
          "Illegal values of external payment status and ext id: (%s, %s)", externalPaymentStatus,
          externalId);
      Preconditions.checkState(
          (authorisedTimestamp == null) ^ (externalPaymentStatus == ExternalPaymentStatus.SUCCESS),
          "authorisedTimestamp is null and external payment status is not 'SUCCESS' or "
              + "authorisedTimestamp is not null and external payment status is 'SUCCESS'");

      return new Payment(id, externalId, referenceNumber, paymentMethod, totalPaid, entrantPayments,
          externalPaymentStatus, submittedTimestamp, authorisedTimestamp, nextUrl,
          emailAddress, cleanAirZoneId, userId, paymentProviderMandateId);
    }

    private boolean externalStatusMatchesExternalPaymentId() {
      if (externalId == null) {
        return externalPaymentStatus == null
            || externalPaymentStatus == ExternalPaymentStatus.INITIATED;
      }
      // if externalId != null, externalPaymentStatus MUST be set to a meaningful value
      return externalPaymentStatus != null;
    }
  }
}
