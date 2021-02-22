package uk.gov.caz.psr;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import java.time.LocalDate;
import java.util.stream.Stream;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import uk.gov.caz.psr.util.PaymentInfoAssertion;

public abstract class ChargeSettlementPaymentInfoTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @LocalServerPort
  int randomServerPort;

  private Statistics statistics;

  @Nested
  class PaymentInfoCommon {

    @Nested
    class WhenRequestForOnlyPaymentMadeDate {

      @Nested
      class AndThereAreMatchingLineItems {

        @Nested
        class AndIsUkWinterTime {

          @Test
          public void shouldReturnDataForTheProvidedDay() {
            LocalDate paymentMadeDate = LocalDate.parse("2020-11-02");

            PaymentInfoAssertion.whenRequested()
                .withParam("paymentMadeDate", paymentMadeDate.toString())
                .then()
                .headerContainsCorrelationId()
                .responseHasOkStatus()
                .totalLineItemsCountIsEqualTo(2)
                .containsExactlyLineItemsWithTravelDates("2020-11-02", "2020-11-03")
                .containsExactlyPaymentsFromDate("2020-11-02");

            verifyResultsWereFetchedByOneDatabaseQuery();
          }
        }

        @Nested
        class AndIsUkSummerTime {
          @Test
          public void shouldReturnDataForTheProvidedDay() {
            LocalDate paymentMadeDate = LocalDate.parse("2020-09-02");

            PaymentInfoAssertion.whenRequested()
                .withParam("paymentMadeDate", paymentMadeDate.toString())
                .then()
                .headerContainsCorrelationId()
                .responseHasOkStatus()
                .totalLineItemsCountIsEqualTo(2)
                .containsExactlyLineItemsWithTravelDates("2020-09-01", "2020-09-02")
                .containsExactlyPaymentsFromDate("2020-09-02");

            verifyResultsWereFetchedByOneDatabaseQuery();
          }
        }
      }

      @Nested
      class AndThereAreNoMatchingLineItems {

        @ParameterizedTest
        @ValueSource(strings = {"2019-11-01", "2019-11-10"})
        public void shouldReturnEmptyArray(String paymentMadeDateParam) {
          LocalDate paymentMadeDate = LocalDate.parse(paymentMadeDateParam);

          PaymentInfoAssertion.whenRequested()
              .withParam("paymentMadeDate", paymentMadeDate.toString())
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }
    }

    @Nested
    class WhenRequestedForOnlyToDatePaidFor {

      @Nested
      class AndThereAreMatchingLineItems {

        @ParameterizedTest
        @ValueSource(strings = {"2019-11-02", "2019-11-03"})
        public void shouldReturnDataForPrecedingDay(String toDatePaidForAsString) {
          LocalDate toDatePaidFor = LocalDate.parse(toDatePaidForAsString);
          LocalDate expectedPaidEntrantDate = toDatePaidFor.minusDays(1);

          PaymentInfoAssertion.whenRequested()
              .withParam("toDatePaidFor", toDatePaidFor.toString())
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .containsExactlyLineItemsWithTravelDates(expectedPaidEntrantDate.toString())
              .totalLineItemsCountIsEqualTo(2);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }

      @Nested
      class AndThereAreNoMatchingLineItems {

        @ParameterizedTest
        @ValueSource(strings = {"2019-11-01", "2019-11-10"})
        public void shouldReturnEmptyArray(String toDatePaidForAsString) {
          LocalDate toDatePaidFor = LocalDate.parse(toDatePaidForAsString);

          PaymentInfoAssertion.whenRequested()
              .withParam("toDatePaidFor", toDatePaidFor.toString())
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }
    }

    @Nested
    class WhenRequestedForOnlyFromDatePaidFor {

      @Nested
      class AndThereAreMatchingLineItems {

        @ParameterizedTest
        @ValueSource(strings = {"2019-11-01", "2019-11-02"})
        public void shouldReturnDataForRequestedDay(String fromDatePaidForAsString) {
          LocalDate fromDatePaidFor = LocalDate.parse(fromDatePaidForAsString);

          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", fromDatePaidFor.toString())
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .containsExactlyLineItemsWithTravelDates(fromDatePaidForAsString)
              .totalLineItemsCountIsEqualTo(2);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }

      @Nested
      class AndThereAreNoMatchingLineItems {

        @ParameterizedTest
        @ValueSource(strings = {"2019-10-31", "2019-11-09", "2019-11-15"})
        public void shouldReturnEmptyArray(String fromDatePaidForAsString) {
          LocalDate fromDatePaidFor = LocalDate.parse(fromDatePaidForAsString);

          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", fromDatePaidFor.toString())
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }
    }

    @Nested
    class WhenRequestedForOnlyDatesRange {

      @Nested
      class AndEmptyDateRange {

        @ParameterizedTest
        @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#uncoveredTravelDatesRangeProvider")
        public void shouldReturnEmptyArray(String fromDatePaidForAsString,
            String toDatePaidForAsString) {

          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", fromDatePaidForAsString)
              .withParam("toDatePaidFor", toDatePaidForAsString)
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }

      @Nested
      class AndCoveredDateRange {

        @ParameterizedTest
        @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#coveredTravelDatesRangeWithExpectedLineItemsCountProvider")
        public void shouldReturnDataForRequestedDaysRange(String fromDatePaidForAsString,
            String toDatePaidForAsString, int expectedLineItemsCount) {
          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", fromDatePaidForAsString)
              .withParam("toDatePaidFor", toDatePaidForAsString)
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .totalLineItemsCountIsEqualTo(expectedLineItemsCount);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }
    }

    @Nested
    class WhenRequestedWithVrn {

      @Nested
      class WhichDoesNotExist {

        @ParameterizedTest
        @ValueSource(strings = {"XB11CDE", "YB11CDE", "ZB11CDE"})
        public void shouldReturnBadRequestResponse(String nonExistingVrn) {
          PaymentInfoAssertion.whenRequested()
              .withParam("vrn", nonExistingVrn)
              .then()
              .headerContainsCorrelationId()
              .responseHasBadRequestStatus()
              .responseHasErrorField("vrn");
        }

        @Nested
        class AndCoveredDateRange {

          @ParameterizedTest
          @ValueSource(strings = {"XB11CDE", "YB11CDE", "ZB11CDE"})
          public void shouldReturnEmptyArray(String nonExistingVrn) {
            String fromDatePaidForAsString = "2019-11-01";
            String toDatePaidForAsString = "2019-11-02";
            PaymentInfoAssertion.whenRequested()
                .withParam("fromDatePaidFor", fromDatePaidForAsString)
                .withParam("toDatePaidFor", toDatePaidForAsString)
                .withParam("vrn", nonExistingVrn)
                .then()
                .headerContainsCorrelationId()
                .responseHasBadRequestStatus()
                .responseHasErrorField("vrn");
          }
        }

        @Nested
        class AndCoveredFromDatePaidFor {

          @ParameterizedTest
          @ValueSource(strings = {"XB11CDE", "YB11CDE", "ZB11CDE"})
          public void shouldReturnEmptyArray(String nonExistingVrn) {
            String fromDatePaidForAsString = "2019-11-01";
            PaymentInfoAssertion.whenRequested()
                .withParam("fromDatePaidFor", fromDatePaidForAsString)
                .withParam("vrn", nonExistingVrn)
                .then()
                .headerContainsCorrelationId()
                .responseHasBadRequestStatus()
                .responseHasErrorField("vrn");
          }
        }

        @Nested
        class AndCoveredToDatePaidFor {

          @ParameterizedTest
          @ValueSource(strings = {"XB11CDE", "YB11CDE", "ZB11CDE"})
          public void shouldReturnEmptyArray(String nonExistingVrn) {
            String toDatePaidForAsString = "2019-11-02";
            PaymentInfoAssertion.whenRequested()
                .withParam("toDatePaidFor", toDatePaidForAsString)
                .withParam("vrn", nonExistingVrn)
                .then()
                .headerContainsCorrelationId()
                .responseHasBadRequestStatus()
                .responseHasErrorField("vrn");
          }
        }
      }

      @Nested
      class WhichExists {

        @ParameterizedTest
        @ValueSource(strings = {
            "AB11CDE", // existing
            "Ab11CdE", "ab11cDe", // with changed capitalisation
            "AB 11CDE", "AB 1 1 CD E", " AB 11CDE ", // with whitespaces
            "AB 11cD e " // with whitespaces and changed capitalisation
        })
        public void shouldReturnDataForRequestedVrnRegardlessOfCapitalisationAndWhitespaces(
            String vrn) {
          PaymentInfoAssertion.whenRequested()
              .withParam("vrn", vrn)
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsExactlyVrns(vrn)
              .containsExactlyLineItemsWithTravelDates("2019-11-01", "2019-11-02")
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .totalLineItemsCountIsEqualTo(2);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }

        @Nested
        class WhenRequestedWithOnlyToDatePaidFor {

          @Nested
          class AndThereAreMatchingLineItems {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#existingToPaidDatesAndVrnProvider")
            public void shouldReturnDataForPrecedingDay(String toDatePaidForAsString, String vrn) {
              LocalDate toDatePaidFor = LocalDate.parse(toDatePaidForAsString);
              LocalDate expectedPaidEntrantDate = toDatePaidFor.minusDays(1);

              PaymentInfoAssertion.whenRequested()
                  .withParam("toDatePaidFor", toDatePaidFor.toString())
                  .withParam("vrn", vrn)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .doesNotContainNotPaidEntries()
                  .containsReferenceNumbers()
                  .containsExactlyVrns(vrn)
                  .containsExactlyLineItemsWithTravelDates(expectedPaidEntrantDate.toString())
                  .totalLineItemsCountIsEqualTo(1);

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class AndThereAreNoMatchingLineItems {

            @ParameterizedTest
            @ValueSource(strings = {"2019-11-01", "2019-11-10"})
            public void shouldReturnEmptyArray(String toDatePaidForAsString) {
              String vrn = "ND84VSX";
              LocalDate toDatePaidFor = LocalDate.parse(toDatePaidForAsString);

              PaymentInfoAssertion.whenRequested()
                  .withParam("toDatePaidFor", toDatePaidFor.toString())
                  .withParam("vrn", vrn)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }
        }

        @Nested
        class WhenRequestedWithOnlyFromDatePaidFor {

          @Nested
          class AndThereAreMatchingLineItems {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#existingFromPaidDatesAndVrnProvider")
            public void shouldReturnDataForRequestedDay(String fromDatePaidForAsString,
                String vrn) {
              PaymentInfoAssertion.whenRequested()
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("vrn", vrn)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .doesNotContainNotPaidEntries()
                  .containsReferenceNumbers()
                  .containsExactlyVrns(vrn)
                  .containsExactlyLineItemsWithTravelDates(fromDatePaidForAsString)
                  .totalLineItemsCountIsEqualTo(1);

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class AndThereAreNoMatchingLineItems {

            @ParameterizedTest
            @ValueSource(strings = {"2019-10-31", "2019-11-09"})
            public void shouldReturnEmptyArray(String fromDatePaidForAsString) {
              String vrn = "ND84VSX";

              PaymentInfoAssertion.whenRequested()
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("vrn", vrn)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }
        }

        @Nested
        class WhenRequestedWithDatesRange {

          @Nested
          class WhichIsCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn")
            public void shouldReturnDataForRequestedDaysRange(String fromDatePaidForAsString,
                String toDatePaidForAsString, int expectedLineItemsCount) {
              String vrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", vrn)
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .doesNotContainNotPaidEntries()
                  .containsReferenceNumbers()
                  .totalLineItemsCountIsEqualTo(expectedLineItemsCount);

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class WhichIsNotCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoTest#uncoveredTravelDatesRangeProviderForVrn")
            public void shouldReturnEmptyArray(String fromDatePaidForAsString,
                String toDatePaidForAsString) {
              String vrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", vrn)
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }
        }
      }
    }

    @Nested
    class WhenRequestedWithPaymentProviderId {

      @Nested
      class WhichDoesNotExist {

        @ParameterizedTest
        @ValueSource(strings = {"abcde", "fghi", "jklmn"})
        public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
          PaymentInfoAssertion.whenRequested()
              .withParam("paymentProviderId", nonExistingPaymentProviderId)
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }

        @Nested
        class AndExistingVrn {

          @ParameterizedTest
          @ValueSource(strings = {"abcde", "fghi", "jklmn"})
          public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
            String existingVrn = "ND84VSX";
            PaymentInfoAssertion.whenRequested()
                .withParam("vrn", existingVrn)
                .withParam("paymentProviderId", nonExistingPaymentProviderId)
                .then()
                .headerContainsCorrelationId()
                .responseHasOkStatus()
                .containsEmptyResults();

            verifyResultsWereFetchedByOneDatabaseQuery();
          }

          @Nested
          class AndCoveredDateRange {

            @ParameterizedTest
            @ValueSource(strings = {"abcde", "fghi", "jklmn"})
            public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
              String existingVrn = "ND84VSX";
              String fromDatePaidForAsString = "2019-11-01";
              String toDatePaidForAsString = "2019-11-02";
              PaymentInfoAssertion.whenRequested()
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .withParam("vrn", existingVrn)
                  .withParam("paymentProviderId", nonExistingPaymentProviderId)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class AndCoveredFromDatePaidFor {

            @ParameterizedTest
            @ValueSource(strings = {"abcde", "fghi", "jklmn"})
            public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
              String existingVrn = "ND84VSX";
              String fromDatePaidForAsString = "2019-11-01";
              PaymentInfoAssertion.whenRequested()
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("vrn", existingVrn)
                  .withParam("paymentProviderId", nonExistingPaymentProviderId)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class AndCoveredToDatePaidFor {

            @ParameterizedTest
            @ValueSource(strings = {"abcde", "fghi", "jklmn"})
            public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
              String existingVrn = "ND84VSX";
              String toDatePaidForAsString = "2019-11-02";
              PaymentInfoAssertion.whenRequested()
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .withParam("vrn", existingVrn)
                  .withParam("paymentProviderId", nonExistingPaymentProviderId)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }
        }

        @Nested
        class AndNonExistingVrn {

          @ParameterizedTest
          @ValueSource(strings = {"abcde", "fghi", "jklmn"})
          public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
            PaymentInfoAssertion.whenRequested()
                .withParam("vrn", "AB11CDE")
                .withParam("paymentProviderId", nonExistingPaymentProviderId)
                .then()
                .headerContainsCorrelationId()
                .responseHasOkStatus()
                .containsEmptyResults();

            verifyResultsWereFetchedByOneDatabaseQuery();
          }

          @Nested
          class AndCoveredDateRange {

            @ParameterizedTest
            @ValueSource(strings = {"abcde", "fghi", "jklmn"})
            public void shouldReturnEmptyArray(String nonExistingPaymentProviderId) {
              String vrn = "ND84VSX";
              String fromDatePaidForAsString = "2019-11-01";
              String toDatePaidForAsString = "2019-11-02";
              PaymentInfoAssertion.whenRequested()
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .withParam("vrn", vrn)
                  .withParam("paymentProviderId", nonExistingPaymentProviderId)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }
        }
      }

      @Nested
      class WhichExists {

        @Test
        public void shouldReturnDataForRequestedPaymentProviderId() {
          String paymentProviderId = "ext-payment-id-3";
          PaymentInfoAssertion.whenRequested()
              .withParam("paymentProviderId", paymentProviderId)
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .containsOnePaymentWithProviderIdEqualTo(paymentProviderId)
              .containsExactlyVrns("AB11CDE")
              .containsExactlyLineItemsWithTravelDates("2019-11-01", "2019-11-02")
              .totalLineItemsCountIsEqualTo(2);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }

        @Nested
        class AndExistingVrn {

          @Nested
          class WhichMatchesPaymentProviderId {

            @Test
            public void shouldReturnDataForRequestedPaymentProviderIdAndVrn() {
              String paymentProviderId = "ext-payment-id-1";
              String matchingVrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("paymentProviderId", paymentProviderId)
                  .withParam("vrn", matchingVrn)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .doesNotContainNotPaidEntries()
                  .containsReferenceNumbers()
                  .containsExactlyVrns(matchingVrn)
                  .containsOnePaymentWithProviderIdEqualTo(paymentProviderId)
                  .containsExactlyLineItemsWithTravelDates("2019-11-01", "2019-11-02", "2019-11-03",
                      "2019-11-04", "2019-11-05")
                  .totalLineItemsCountIsEqualTo(5);

              verifyResultsWereFetchedByOneDatabaseQuery();
            }

            @Nested
            class AndCoveredDateRange {

              @Test
              public void shouldReturnDataForRequestedPaymentProviderIdVrnAndDateRange() {
                String paymentProviderId = "ext-payment-id-1";
                String matchingVrn = "ND84VSX";
                String fromDatePaidForAsString = "2019-11-03";
                String toDatePaidForAsString = "2019-11-05";
                PaymentInfoAssertion.whenRequested()
                    .withParam("fromDatePaidFor", fromDatePaidForAsString)
                    .withParam("toDatePaidFor", toDatePaidForAsString)
                    .withParam("vrn", matchingVrn)
                    .withParam("paymentProviderId", paymentProviderId)
                    .then()
                    .responseHasOkStatus()
                    .headerContainsCorrelationId()
                    .doesNotContainNotPaidEntries()
                    .containsReferenceNumbers()
                    .containsOnePaymentWithProviderIdEqualTo(paymentProviderId)
                    .containsExactlyVrns(matchingVrn)
                    .containsExactlyLineItemsWithTravelDates("2019-11-03", "2019-11-04",
                        "2019-11-05");

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }

            @Nested
            class AndCoveredFromDatePaidFor {

              @Test
              public void shouldReturnDataForRequestedPaymentProviderIdVrnAndFromDatePaidFor() {
                String paymentProviderId = "ext-payment-id-1";
                String matchingVrn = "ND84VSX";
                String fromDatePaidForAsString = "2019-11-03";
                PaymentInfoAssertion.whenRequested()
                    .withParam("fromDatePaidFor", fromDatePaidForAsString)
                    .withParam("vrn", matchingVrn)
                    .withParam("paymentProviderId", paymentProviderId)
                    .then()
                    .headerContainsCorrelationId()
                    .responseHasOkStatus()
                    .doesNotContainNotPaidEntries()
                    .containsReferenceNumbers()
                    .containsOnePaymentWithProviderIdEqualTo(paymentProviderId)
                    .containsExactlyVrns(matchingVrn)
                    .containsExactlyLineItemsWithTravelDates("2019-11-03");

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }

            @Nested
            class AndCoveredToDatePaidFor {

              @Test
              public void shouldReturnDataForRequestedPaymentProviderIdVrnAndToDatePaidFor() {
                String paymentProviderId = "ext-payment-id-1";
                String matchingVrn = "ND84VSX";
                String toDatePaidForAsString = "2019-11-03";
                PaymentInfoAssertion.whenRequested()
                    .withParam("toDatePaidFor", toDatePaidForAsString)
                    .withParam("vrn", matchingVrn)
                    .withParam("paymentProviderId", paymentProviderId)
                    .then()
                    .headerContainsCorrelationId()
                    .responseHasOkStatus()
                    .doesNotContainNotPaidEntries()
                    .containsReferenceNumbers()
                    .containsOnePaymentWithProviderIdEqualTo(paymentProviderId)
                    .containsExactlyVrns(matchingVrn)
                    .containsExactlyLineItemsWithTravelDates("2019-11-02");

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }
          }

          @Nested
          class WhichDoesNotMatchPaymentProviderId {

            @Test
            public void shouldReturnEmptyArray() {
              String existingPaymentProviderId = "ext-payment-id-3";
              String notMatchingVrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", notMatchingVrn)
                  .withParam("paymentProviderId", existingPaymentProviderId)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .containsEmptyResults();

              verifyResultsWereFetchedByOneDatabaseQuery();
            }

            @Nested
            class AndCoveredDateRange {

              @Test
              public void shouldReturnEmptyArray() {
                String fromDatePaidForAsString = "2019-11-03";
                String toDatePaidForAsString = "2019-11-05";
                String existingPaymentProviderId = "ext-payment-id-3";
                String notMatchingVrn = "ND84VSX";
                PaymentInfoAssertion.whenRequested()
                    .withParam("fromDatePaidFor", fromDatePaidForAsString)
                    .withParam("toDatePaidFor", toDatePaidForAsString)
                    .withParam("vrn", notMatchingVrn)
                    .withParam("paymentProviderId", existingPaymentProviderId)
                    .then()
                    .headerContainsCorrelationId()
                    .responseHasOkStatus()
                    .containsEmptyResults();

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }

            @Nested
            class AndCoveredFromDatePaidFor {

              @Test
              public void shouldReturnEmptyArray() {
                String fromDatePaidForAsString = "2019-11-03";
                String existingPaymentProviderId = "ext-payment-id-3";
                String notMatchingVrn = "ND84VSX";
                PaymentInfoAssertion.whenRequested()
                    .withParam("fromDatePaidFor", fromDatePaidForAsString)
                    .withParam("vrn", notMatchingVrn)
                    .withParam("paymentProviderId", existingPaymentProviderId)
                    .then()
                    .headerContainsCorrelationId()
                    .responseHasOkStatus()
                    .containsEmptyResults();

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }

            @Nested
            class AndCoveredToDatePaidFor {

              @Test
              public void shouldReturnEmptyArray() {
                String toDatePaidForAsString = "2019-11-05";
                String existingPaymentProviderId = "ext-payment-id-3";
                String notMatchingVrn = "ND84VSX";
                PaymentInfoAssertion.whenRequested()
                    .withParam("toDatePaidFor", toDatePaidForAsString)
                    .withParam("vrn", notMatchingVrn)
                    .withParam("paymentProviderId", existingPaymentProviderId)
                    .then()
                    .headerContainsCorrelationId()
                    .responseHasOkStatus()
                    .containsEmptyResults();

                verifyResultsWereFetchedByOneDatabaseQuery();
              }
            }
          }
        }
      }
    }

    @Nested
    class WhenInvalidRequest {

      @Test
      public void shouldReturn400ResponseWhenNoParametersGiven() {
        PaymentInfoAssertion.whenRequested()
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus();
      }

      @Test
      public void shouldReturn400ResponseWhenVrnIsLongerThan15Characters() {
        PaymentInfoAssertion.whenRequested()
            .withParam("vrn", "AB12CD34EF56GH78")
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .responseHasErrorField("vrn");
      }

      @ParameterizedTest
      @ValueSource(strings = {"2020-01-20-", "20/01/2020"})
      public void shouldReturn400ResponseWhenFromDatePaidForIsInvalid(String fromDatePaidFor) {
        PaymentInfoAssertion.whenRequested()
            .withParam("fromDatePaidFor", fromDatePaidFor)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .responseHasErrorField("fromDatePaidFor");
      }

      @ParameterizedTest
      @ValueSource(strings = {"2020-01-20-", "20/01/2020"})
      public void shouldReturn400ResponseWhenToDatePaidForIsInvalid(String toDatePaidFor) {
        PaymentInfoAssertion.whenRequested()
            .withParam("toDatePaidFor", toDatePaidFor)
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .responseHasErrorField("toDatePaidFor");
      }
    }
  }

  @BeforeEach
  public void initHibernateStats() {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
  }

  protected void verifyResultsWereFetchedByOneDatabaseQuery() {
    long queryExecutionCount = statistics.getQueryExecutionCount();
    assertThat(queryExecutionCount).isOne();
  }

  protected void verifyResultsWereFetchedByTwoDatabaseQueries() {
    long queryExecutionCount = statistics.getQueryExecutionCount();
    assertThat(queryExecutionCount).isEqualTo(2);
  }

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = getPaymentInfoPath();
  }

  @BeforeEach
  public void insertTestData() {
    // we cannot use SQL annotations on this class, see:
    // https://github.com/spring-projects/spring-framework/issues/19930
    clearDatabase();
    executeSqlFrom("data/sql/charge-settlement/payment-info/test-data.sql");
  }

  @AfterEach
  public void clearDatabase() {
    executeSqlFrom("data/sql/clear-all-payments.sql");
  }

  abstract String getPaymentInfoPath();

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  public static Stream<Arguments> uncoveredTravelDatesRangeProvider() {
    return Stream.of(
        Arguments.of("2019-05-01", "2019-05-01"),
        Arguments.of("2019-11-09", "2019-11-09"),
        Arguments.of("2019-11-09", "2019-11-10"),
        Arguments.of("2019-10-30", "2019-10-31")
    );
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn() {
    return Stream.of(
        Arguments.of("2019-11-01", "2019-11-02", 2),
        Arguments.of("2019-10-31", "2019-11-02", 2),
        Arguments.of("2019-11-02", "2019-11-06", 4),
        Arguments.of("2019-11-03", "2019-11-07", 3),
        Arguments.of("2019-11-03", "2019-11-08", 3)
    );
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> uncoveredTravelDatesRangeProviderForVrn() {
    return Stream.of(
        Arguments.of("2019-05-01", "2019-05-01"),
        Arguments.of("2019-11-09", "2019-11-09"),
        Arguments.of("2019-11-09", "2019-11-10"),
        Arguments.of("2019-10-30", "2019-10-31")
    );
  }

  public static Stream<Arguments> coveredTravelDatesRangeWithExpectedLineItemsCountProvider() {
    return Stream.of(
        Arguments.of("2019-11-01", "2019-11-02", 4),
        Arguments.of("2019-10-31", "2019-11-02", 4),
        Arguments.of("2019-11-02", "2019-11-06", 5),
        Arguments.of("2019-11-03", "2019-11-07", 3)

    );
  }

  public static Stream<Arguments> existingToPaidDatesAndVrnProvider() {
    return Stream.of(
        Arguments.of("2019-11-02", "ND84VSX"),
        Arguments.of("2019-11-02", "AB11CDE"),
        Arguments.of("2019-11-03", "ND84VSX"),
        Arguments.of("2019-11-03", "AB11CDE"),
        Arguments.of("2019-11-04", "ND84VSX"),
        Arguments.of("2019-11-05", "ND84VSX")
    );
  }

  public static Stream<Arguments> existingFromPaidDatesAndVrnProvider() {
    return Stream.of(
        Arguments.of("2019-11-01", "ND84VSX"),
        Arguments.of("2019-11-01", "AB11CDE"),
        Arguments.of("2019-11-02", "ND84VSX"),
        Arguments.of("2019-11-02", "AB11CDE"),
        Arguments.of("2019-11-03", "ND84VSX"),
        Arguments.of("2019-11-04", "ND84VSX"),
        Arguments.of("2019-11-05", "ND84VSX")
    );
  }
}
