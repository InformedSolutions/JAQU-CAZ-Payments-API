package uk.gov.caz.psr.dto.directdebit;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.dto.Transaction;
import uk.gov.caz.psr.util.DuplicatedEntrantsInTransactionsValidator;

@Value
@Builder(toBuilder = true)
@Slf4j
public class CreateDirectDebitPaymentRequest {

  private static final EmailValidator emailValidator = new EmailValidator();
  private static final Map<Function<CreateDirectDebitPaymentRequest, Boolean>, String> validators =
      ImmutableMap.<Function<CreateDirectDebitPaymentRequest, Boolean>, String>builder()
          .put(cleanAirZoneIdNotNull(), "'cleanAirZoneId' cannot be null")
          .put(accountIdNotNull(), "'accountId' cannot be null")
          .put(mandateIdNotNullOrEmpty(), "'mandateId' cannot be null or empty")
          .put(transactionsNotEmpty(), "'transactions' cannot be null or empty")
          .put(vrnsNotEmpty(), "'vrn' in all transactions cannot be null or empty")
          .put(vrnsMaxLength(), "'vrn' length in all transactions must be from 1 to 15")
          .put(chargesNotNull(), "'charge' in all transactions cannot be null")
          .put(positiveCharge(), "'charge' in all transactions must be positive")
          .put(userIdShouldBeUuidIfPresent(), "'userId' must be a valid UUID")
          .put(travelDateNotNull(), "'travelDate' in all transactions cannot be null")
          .put(tariffCodeNotEmpty(), "'tariffCode' in all transactions cannot be null or empty")
          .put(containsNoDuplicatedEntrants(), "Request cannot have duplicated travel date(s)")
          .put(emailNotNullOrEmpty(), "'userEmail' cannot be null or empty")
          .put(emailIsValid(), "'userEmail' is not valid.")
          .build();

  @ApiModelProperty(
      value = "${swagger.model.descriptions.create-direct-debit-payment.clean-zone-id}")
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.create-direct-debit-payment.account-id}")
  String accountId;

  @ApiModelProperty(value = "${swagger.model.descriptions.create-direct-debit-payment.mandate-id}")
  String mandateId;

  @ApiModelProperty(value = "${swagger.model.descriptions.create-direct-debit-payment.user-id}")
  String userId;

  @ApiModelProperty(value = "${swagger.model.descriptions.create-direct-debit-payment.user-email}")
  @ToString.Exclude
  String userEmail;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.create-direct-debit-payment.transactions}")
  List<Transaction> transactions;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Verifies if {@code request} does not contain duplicated vehicle entrants.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> containsNoDuplicatedEntrants() {
    return request -> DuplicatedEntrantsInTransactionsValidator
        .containsNoDuplicatedEntrants(request.getTransactions());
  }

  /**
   * Returns a lambda that verifies if 'clean air zone id' is not null.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> cleanAirZoneIdNotNull() {
    return request -> Objects.nonNull(request.getCleanAirZoneId());
  }

  /**
   * Returns a lambda that verifies if 'account id' is not null.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> accountIdNotNull() {
    return request -> Objects.nonNull(request.getCleanAirZoneId());
  }

  /**
   * Returns a lambda that verifies if 'mandate id' is not null or empty.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> mandateIdNotNullOrEmpty() {
    return request -> !Strings.isNullOrEmpty(request.getMandateId());
  }

  /**
   * Returns a lambda that verifies if 'user email' is not null or empty.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> emailNotNullOrEmpty() {
    return request -> !Strings.isNullOrEmpty(request.getUserEmail());
  }

  /**
   * Returns a lambda that verifies if 'email' is valid Email Address.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> emailIsValid() {
    return request -> emailValidator.isValid(request.getUserEmail(), null);
  }


  /**
   * Returns a lambda that verifies if 'transactions' are not empty.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> transactionsNotEmpty() {
    return request -> !CollectionUtils.isEmpty(request.getTransactions());
  }

  /**
   * Returns a lambda that verifies if 'tariff code' is not null and not empty.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> tariffCodeNotEmpty() {
    return request -> allTransactionsMatch(request,
        transaction -> StringUtils.hasText(transaction.getTariffCode()));
  }

  /**
   * Returns a lambda that verifies if 'travel data' is not null.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> travelDateNotNull() {
    return request -> allTransactionsMatch(request,
        transaction -> Objects.nonNull(transaction.getTravelDate()));
  }


  /**
   * Returns a lambda that verifies if 'charge' is positive.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> positiveCharge() {
    return request -> allTransactionsMatch(request, transaction -> transaction.getCharge() > 0);
  }

  /**
   * Returns a lambda that verifies if 'charge' is not null.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> chargesNotNull() {
    return request -> allTransactionsMatch(request,
        transaction -> Objects.nonNull(transaction.getCharge()));
  }

  /**
   * Returns a lambda that verifies if 'vrn's length does not exceed imposed limits.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> vrnsMaxLength() {
    return request -> allTransactionsMatch(request,
        transaction -> transaction.getVrn().length() <= 15);
  }

  /**
   * Returns a lambda that verifies if 'vrn' is not null and not empty.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> vrnsNotEmpty() {
    return request -> allTransactionsMatch(request,
        transaction -> StringUtils.hasText(transaction.getVrn()));
  }

  /**
   * Verifies if all transactions match {@code transactionPredicate}.
   */
  private static boolean allTransactionsMatch(CreateDirectDebitPaymentRequest request,
      Predicate<Transaction> transactionPredicate) {
    return request.getTransactions().stream().allMatch(transactionPredicate);
  }

  /**
   * Returns a lambda that verifies if 'user id' is a valid value, unless it is not set.
   */
  private static Function<CreateDirectDebitPaymentRequest, Boolean> userIdShouldBeUuidIfPresent() {
    return request -> {
      String userId = request.getUserId();
      // the field is optional - return true when it is missing
      if (!StringUtils.hasText(userId)) {
        return true;
      }
      return isValidUuid(userId);
    };
  }

  private static boolean isValidUuid(String userId) {
    try {
      // see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8159339
      return UUID.fromString(userId).toString().equals(userId);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
