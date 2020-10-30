package uk.gov.caz.psr.dto;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.psr.util.DuplicatedEntrantsInTransactionsValidator;

@Value
@Builder(toBuilder = true)
@Slf4j
public class InitiatePaymentRequest {

  private static final Map<Function<InitiatePaymentRequest, Boolean>, String> validators =
      ImmutableMap.<Function<InitiatePaymentRequest, Boolean>, String>builder()
          .put(cleanAirZoneIdNotNull(), "'cleanAirZoneId' cannot be null")
          .put(returnUrlNotEmpty(), "'returnUrl' cannot be null or empty")
          .put(transactionsNotEmpty(), "'transactions' cannot be null or empty")
          .put(vrnsNotEmpty(), "'vrn' in all transactions cannot be null or empty")
          .put(vrnsMaxLength(), "'vrn' length in all transactions must be from 1 to 15")
          .put(chargesNotNull(), "'charge' in all transactions cannot be null")
          .put(positiveCharge(), "'charge' in all transactions must be positive")
          .put(userIdShouldBeUuidIfPresent(), "'userId' must be a valid UUID")
          .put(telephonePaymentNotNull(), "'telephonePayment' cannot be null")
          .put(operatorIdNullIfTelephonePaymentFalse(), "'operatorId' cannot be set when "
              + "'telephonePayment' is false")
          .put(operatorIdShouldBeUuidIfPresent(), "'operatorId' must be a valid UUID")
          .put(travelDateNotNull(), "'travelDate' in all transactions cannot be null")
          .put(tariffCodeNotEmpty(), "'tariffCode' in all transactions cannot be null or empty")
          .put(containsNoDuplicatedEntrants(), "Request cannot have duplicated travel date(s)")
          .build();

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.clean-zone-id}")
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.return-url}")
  String returnUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.user-id}")
  String userId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.telephone-payment}")
  Boolean telephonePayment;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.operator-id}")
  String operatorId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.transactions}")
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
  private static Function<InitiatePaymentRequest, Boolean> containsNoDuplicatedEntrants() {
    return request -> DuplicatedEntrantsInTransactionsValidator
        .containsNoDuplicatedEntrants(request.getTransactions());
  }

  /**
   * Returns a lambda that verifies if 'tariff code' is not null and not empty.
   */
  private static Function<InitiatePaymentRequest, Boolean> tariffCodeNotEmpty() {
    return request -> allTransactionsMatch(request,
        transaction -> StringUtils.hasText(transaction.getTariffCode()));
  }

  /**
   * Returns a lambda that verifies if 'travel data' is not null.
   */
  private static Function<InitiatePaymentRequest, Boolean> travelDateNotNull() {
    return request -> allTransactionsMatch(request,
        transaction -> Objects.nonNull(transaction.getTravelDate()));
  }

  /**
   * Returns a lambda that verifies if 'charge' is positive.
   */
  private static Function<InitiatePaymentRequest, Boolean> positiveCharge() {
    return request -> allTransactionsMatch(request, transaction -> transaction.getCharge() > 0);
  }

  /**
   * Returns a lambda that verifies if 'charge' is not null.
   */
  private static Function<InitiatePaymentRequest, Boolean> chargesNotNull() {
    return request -> allTransactionsMatch(request,
        transaction -> Objects.nonNull(transaction.getCharge()));
  }

  /**
   * Returns a lambda that verifies if 'telephonePayment' is not null.
   */
  private static Function<InitiatePaymentRequest, Boolean> telephonePaymentNotNull() {
    return request -> Objects.nonNull(request.getTelephonePayment());
  }

  /**
   * Returns a lambda that verifies that 'operatorId' is null when 'telephonePayment' is false.
   */
  private static Function<InitiatePaymentRequest, Boolean> operatorIdNullIfTelephonePaymentFalse() {
    return request -> request.getTelephonePayment() || Objects.isNull(request.getOperatorId());
  }

  /**
   * Returns a lambda that verifies if 'vrn's length does not exceed imposed limits.
   */
  private static Function<InitiatePaymentRequest, Boolean> vrnsMaxLength() {
    return request -> allTransactionsMatch(request,
        transaction -> transaction.getVrn().length() <= 15);
  }

  /**
   * Returns a lambda that verifies if 'vrn' is not null and not empty.
   */
  private static Function<InitiatePaymentRequest, Boolean> vrnsNotEmpty() {
    return request -> allTransactionsMatch(request,
        transaction -> StringUtils.hasText(transaction.getVrn()));
  }

  /**
   * Returns a lambda that verifies if 'transactions' are not empty.
   */
  private static Function<InitiatePaymentRequest, Boolean> transactionsNotEmpty() {
    return request -> !CollectionUtils.isEmpty(request.getTransactions());
  }

  /**
   * Returns a lambda that verifies if 'url' is not null and not empty.
   */
  private static Function<InitiatePaymentRequest, Boolean> returnUrlNotEmpty() {
    return request -> StringUtils.hasText(request.getReturnUrl());
  }

  /**
   * Returns a lambda that verifies if 'clean air zone id' is not null.
   */
  private static Function<InitiatePaymentRequest, Boolean> cleanAirZoneIdNotNull() {
    return request -> Objects.nonNull(request.getCleanAirZoneId());
  }

  /**
   * Returns a lambda that verifies if 'user id' is a valid value, unless it is not set.
   */
  private static Function<InitiatePaymentRequest, Boolean> userIdShouldBeUuidIfPresent() {
    return request -> {
      String userId = request.getUserId();
      // the field is optional - return true when it is missing
      if (!StringUtils.hasText(userId)) {
        return true;
      }
      return isValidUuid(userId);
    };
  }

  /**
   * Returns a lambda that verifies if 'operator id' is a valid value, unless it is not set.
   */
  private static Function<InitiatePaymentRequest, Boolean> operatorIdShouldBeUuidIfPresent() {
    return request -> Optional.ofNullable(request.getOperatorId())
        .filter(StringUtils::hasText)
        .map(InitiatePaymentRequest::isValidUuid)
        .orElse(Boolean.TRUE);
  }

  private static boolean isValidUuid(String userId) {
    try {
      // see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8159339
      return UUID.fromString(userId).toString().equals(userId);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Verifies if all transactions match {@code transactionPredicate}.
   */
  private static boolean allTransactionsMatch(InitiatePaymentRequest request,
      Predicate<Transaction> transactionPredicate) {
    return request.getTransactions().stream().allMatch(transactionPredicate);
  }
}
