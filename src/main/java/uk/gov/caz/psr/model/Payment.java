package uk.gov.caz.psr.model;

import com.google.common.base.Preconditions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * An entity which represents a row stored in the database in {@code PAYMENT} table.
 */
@Value
@Builder(toBuilder = true)
@Slf4j
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
   * A list of {@link VehicleEntrantPayment} instances associated with this object.
   */
  @NonNull
  List<VehicleEntrantPayment> vehicleEntrantPayments;

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
   * Gets the payment status provided that statuses of all entrant payments are the same (they can
   * differ if, for example, a local authority changes the status of one vehicle entrant payment).
   * If they are not, {@link IllegalStateException} is thrown.
   *
   * @return {@link PaymentStatus} for the payment if statuses of all entrant payments are the same.
   * @throws IllegalStateException if there is at least one vehicle entrant payment's status
   *     which is different from the others.
   */
  public PaymentStatus getStatus() {
    Preconditions.checkState(vehicleEntrantPaymentsHaveSameStatus(vehicleEntrantPayments),
        "Vehicle entrant payments are empty or do not have one common status");
    return vehicleEntrantPayments.iterator().next().getStatus();
  }

  /**
   * Predicate which checks whether all vehicle entrant payments have the same status.
   *
   * @param vehicleEntrantPayments A list of {@link VehicleEntrantPayment}.
   * @return true if {@code vehicleEntrantPayments} is not empty and all vehicle entrant payments
   *     have the same status.
   */
  private static boolean vehicleEntrantPaymentsHaveSameStatus(
      List<VehicleEntrantPayment> vehicleEntrantPayments) {
    if (vehicleEntrantPayments == null || vehicleEntrantPayments.isEmpty()) {
      return false;
    }
    PaymentStatus status = vehicleEntrantPayments.iterator().next().getStatus();
    return vehicleEntrantPayments.stream()
        .map(VehicleEntrantPayment::getStatus)
        .allMatch(localStatus -> localStatus == status);
  }

  /**
   * An overridden lombok's builder.
   */
  public static class PaymentBuilder {

    private PaymentStatus status;

    /**
     * Sets the passed {@code status} for all vehicle entrant payments provided the have the same
     * status set. Additionally, if {@code status} is equal to {@link PaymentStatus#SUCCESS}, {@link
     * Payment#authorisedTimestamp} is set to the current timestamp ({@link LocalDateTime#now()}).
     *
     * @param status A {@link PaymentStatus} which is to be set for all {@link
     *     VehicleEntrantPayment} of this payment.
     * @return {@link PaymentBuilder}.
     * @throws IllegalStateException if there is at least one vehicle entrant payment's status
     *     which is different from the others.
     */
    public PaymentBuilder status(PaymentStatus status) {
      Preconditions.checkState(vehicleEntrantPaymentsHaveSameStatus(vehicleEntrantPayments),
          "Vehicle entrant payments are empty or do not have one common status");
      if (status == PaymentStatus.SUCCESS) {
        authorisedTimestamp(LocalDateTime.now());
        log.info("Setting status to '{}', setting authorised timestamp to '{}' for payment '{}'",
            PaymentStatus.SUCCESS, authorisedTimestamp, id);
      }
      this.status = status;
      return this;
    }

    /**
     * Build the payment object.
     *
     * @return An instance of {@link Payment}.
     */
    public Payment build() {
      if (status != null) {
        vehicleEntrantPayments = vehicleEntrantPayments.stream()
            .map(this::copyWithCurrentStatus)
            .collect(Collectors.toList());
      }
      return new Payment(id, externalId, paymentMethod, totalPaid,
          vehicleEntrantPayments, submittedTimestamp, authorisedTimestamp, nextUrl);
    }

    /**
     * Creates a new instance of {@link VehicleEntrantPayment} with all attributes from the passed
     * {@code vehicleEntrantPayment} object and the current value of {@link PaymentBuilder#status}.
     *
     * @param vehicleEntrantPayment An instance of {@link VehicleEntrantPayment} which will be
     *     copied with a new value for {@code status}.
     * @return A new instance of {@link VehicleEntrantPayment}.
     */
    private VehicleEntrantPayment copyWithCurrentStatus(
        VehicleEntrantPayment vehicleEntrantPayment) {
      return vehicleEntrantPayment.toBuilder()
          .status(status)
          .build();
    }
  }
}
