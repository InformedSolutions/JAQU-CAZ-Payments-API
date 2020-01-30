package uk.gov.caz.psr.dto;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.caz.psr.controller.exception.InvalidRequestPayloadException;

@Value
@Builder
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
          .put(travelDateNotNull(), "'travelDate' in all transactions cannot be null")
          .put(tariffCodeNotEmpty(), "'tariffCode' in all transactions cannot be null or empty")
          .put(containsNoDuplicatedEntrants(), "Request cannot have duplicated travel date(s) ")
          .build();

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.clean-zone-id}")
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.return-url}")
  String returnUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.transactions}")
  List<Transaction> transactions;

  /**
   * An inner class that represents a vehicle entrant for a given {@code cleanAirZoneId}.
   */
  @Value
  @Builder(toBuilder = true)
  public static class Transaction {

    @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.vrn}")
    String vrn;

    @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.charge}")
    Integer charge;

    @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.travel-date}")
    LocalDate travelDate;

    @ApiModelProperty(value = "${swagger.model.descriptions.payments-initiate.tariff-code}")
    String tariffCode;
  }

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
    return request -> {
      Map<String, List<LocalDate>> travelDatesByVrn = groupByVrn(request);
      for (Entry<String, List<LocalDate>> travelDatesForVrn : travelDatesByVrn.entrySet()) {
        if (travelDatesContainDuplicate(travelDatesForVrn)) {
          String vrn = travelDatesForVrn.getKey();
          log.warn("Duplicated travel date(s) detected for VRN '{}'", vrn);
          return false;
        }
      }
      return true;
    };
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
   * Returns a lambda that verifies if 'vrn's length does not exceed imposed limits.
   */
  private static Function<InitiatePaymentRequest, Boolean> vrnsMaxLength() {
    return request -> allTransactionsMatch(request,
        transaction -> transaction.getVrn().length() >= 1 && transaction.getVrn().length() <= 15);
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
   * Verifies if all transactions match {@code transactionPredicate}.
   */
  private static boolean allTransactionsMatch(InitiatePaymentRequest request,
      Predicate<Transaction> transactionPredicate) {
    return request.getTransactions().stream().allMatch(transactionPredicate);
  }

  /**
   * Converts {@code travelDates} to a set of dates.
   */
  private static HashSet<LocalDate> toSet(List<LocalDate> travelDates) {
    return new HashSet<>(travelDates);
  }

  /**
   * Groups transactions by vrn and then each transactions is mapped to its travel date.
   */
  private static Map<String, List<LocalDate>> groupByVrn(InitiatePaymentRequest input) {
    return input
        .getTransactions()
        .stream()
        .collect(groupingBy(Transaction::getVrn, mapping(Transaction::getTravelDate, toList())));
  }
}
