package uk.gov.caz.psr.service.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import uk.gov.caz.psr.dto.SendEmailRequest;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;

/**
 * An abstract class acting as a base class for subclasses that create {@link SendEmailRequest}s
 * provided {@link #isApplicableFor(uk.gov.caz.psr.model.Payment)} returns {@code true}.
 */
@AllArgsConstructor
public abstract class CustomPaymentReceiptEmailCreator {

  public static final String DATE_FORMAT = "dd MMMM YYYY";

  private final CurrencyFormatter currencyFormatter;
  private final CleanAirZoneService cleanAirZoneNameGetterService;
  private final ObjectMapper objectMapper;
  private final String templateId;

  /**
   * Predicate that returns {@code true} if this class should create {@link SendEmailRequest} for a
   * given {@code payment}. This method should be called before invoking {@link
   * #createSendEmailRequest(Payment)}.
   *
   * @return true if if this class should create {@link SendEmailRequest} for a given payment, false
   *     otherwise.
   */
  public abstract boolean isApplicableFor(Payment payment);

  /**
   * Creates {@link SendEmailRequest} based on the passed {@code payment} and {@code templateId}
   * (via constructor).
   */
  public final SendEmailRequest createSendEmailRequest(Payment payment) {
    return SendEmailRequest.builder()
        .emailAddress(payment.getEmailAddress())
        .personalisation(toJson(createPersonalisationPayload(payment)))
        .templateId(templateId)
        .build();
  }

  /**
   * To be overloaded in subclasses. Creates a map containing variables used in the email.
   */
  abstract Map<String, Object> createPersonalisationPayload(Payment payment);

  //--- Helper methods to be used in subclasses

  /**
   * Converts pennies ({@code amountInPennies}) to pounds.
   */
  final double toPounds(int amountInPennies) {
    return currencyFormatter.parsePennies(amountInPennies);
  }

  /**
   * Converts pennies to its string representation in pounds. The result does NOT include the pound
   * sign (£).
   */
  final String toFormattedPounds(int amountInPennies) {
    double amountInPounds = toPounds(amountInPennies);
    return String.format(Locale.UK, "%.2f", amountInPounds);
  }

  /**
   * Retrieves the name of a clean air zone against which the payment has been made. Please note
   * that this involves an I/O operation.
   */
  final String getCazName(Payment payment) {
    UUID cleanAirZoneId = getCleanAirZoneId(payment);
    return cleanAirZoneNameGetterService.fetch(cleanAirZoneId);
  }

  /**
   * Converts the {@code date} to its string representation used in an email.
   */
  final String formatDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.UK));
  }

  // --- private methods

  /**
   * Converts the passed object {@code o} to a JSON string.
   */
  @SneakyThrows
  private String toJson(Object o) {
    return objectMapper.writeValueAsString(o);
  }

  /**
   * Retrieves the Clean Air Zone ID for the given {@link Payment}.
   *
   * @param payment an instance of a {@link Payment} object
   * @return a {@link UUID} representing a Clean Air Zone.
   */
  private UUID getCleanAirZoneId(Payment payment) {
    Preconditions.checkArgument(!payment.getEntrantPayments().isEmpty(),
        "Vehicle entrant payments should not be empty");
    return payment.getEntrantPayments().iterator().next().getCleanAirZoneId();
  }
}
