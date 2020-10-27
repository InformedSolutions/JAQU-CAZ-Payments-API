package uk.gov.caz.psr;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.ChargeSettlementController;
import uk.gov.caz.psr.util.PaymentInfoAssertion;

@FullyRunningServerIntegrationTest
class ChargeSettlementPaymentInfoV2TestIT extends ChargeSettlementPaymentInfoTest {

  @Override
  public String getPaymentInfoPath() {
    return ChargeSettlementController.PAYMENT_INFO_PATH_V2;
  }

  @Nested
  class PaymentInfoV2 {

    @Nested
    class WhenRequestedWithVrn {

      @Nested
      class WhichExists {

        @Nested
        class WhenRequestedWithDatesRange {

          @Nested
          class WhichIsCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoV2TestIT#coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn")
            public void shouldReturnErrorsWithQueryDateRangeExceededForRequestedDaysRange(
                String fromDatePaidForAsString,
                String toDatePaidForAsString) {
              String vrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", vrn)
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasBadRequestStatus()
                  .containsErrors(2)
                  .andContainsErrorWith(0, 400, "fromDatePaidFor",
                      "The requested dates exceed the maximum permitted range",
                      "Query date range exceeded")
                  .andContainsErrorWith(1, 400, "toDatePaidFor",
                      "The requested dates exceed the maximum permitted range",
                      "Query date range exceeded");
            }
          }

          @Nested
          class WhichIsNotCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoV2TestIT#uncoveredTravelDatesRangeProviderForVrn")
            public void shouldReturnErrorsWithQueryDateRangeExceededForRequestedDaysRange(
                String fromDatePaidForAsString,
                String toDatePaidForAsString) {
              String vrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", vrn)
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasBadRequestStatus()
                  .containsErrors(2).andContainsErrorWith(0, 400, "fromDatePaidFor",
                  "The requested dates exceed the maximum permitted range",
                  "Query date range exceeded")
                  .andContainsErrorWith(1, 400, "toDatePaidFor",
                      "The requested dates exceed the maximum permitted range",
                      "Query date range exceeded");
            }
          }
        }
      }
    }

    @Nested
    class WhenRequestedForOnlyDatesRange {

      @Nested
      class AndEmptyDateRange {

        @Nested
        class AndCoveredDateRange {

          @Test
          public void shouldReturnErrorsForRequestedDaysRange() {
            PaymentInfoAssertion.whenRequested()
                .withParam("fromDatePaidFor", "1993-04-18")
                .withParam("toDatePaidFor", "2019-11-02")
                .then()
                .headerContainsCorrelationId()
                .responseHasBadRequestStatus()
                .containsErrors(2).andContainsErrorWith(0, 400, "fromDatePaidFor",
                "The requested dates exceed the maximum permitted range",
                "Query date range exceeded")
                .andContainsErrorWith(1, 400, "toDatePaidFor",
                    "The requested dates exceed the maximum permitted range",
                    "Query date range exceeded");
          }

          @Test
          public void shouldReturnSortedDataWithPagesForRequestedDaysRange() {
            PaymentInfoAssertion.whenRequested()
                .withParam("fromDatePaidFor", "2019-11-01")
                .withParam("toDatePaidFor", "2019-11-02")
                .then()
                .headerContainsCorrelationId()
                .responseHasOkStatus()
                .doesNotContainNotPaidEntries()
                .containsReferenceNumbers()
                .containsPages(1)
                .hasFirstResultWith("AB11CDE")
                .hasSecondResultWith("ND84VSX")
                .totalLineItemsCountIsEqualTo(4);

            verifyResultsWereFetchedByOneDatabaseQuery();
          }
        }
      }
    }

    @Nested
    class WhenRequestedWithPageNumber {

      @ParameterizedTest
      @ValueSource(ints = {-99, -5, -2, 1000, 1111})
      public void shouldReturnErrorsForRequestedDaysRangeWithPage(int page) {
        PaymentInfoAssertion.whenRequested()
            .withParam("fromDatePaidFor", "2019-11-01")
            .withParam("toDatePaidFor", "2019-11-02")
            .withParam("page", String.valueOf(page))
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .containsErrors(1)
            .andContainsErrorWith(0, 400, "page",
                "Invalid page number",
                "Parameter validation error");
      }

      @Test
      public void shouldReturnSortedDataWithPagesForRequestedDaysRange() {
        PaymentInfoAssertion.whenRequested()
            .withParam("fromDatePaidFor", "2019-11-01")
            .withParam("toDatePaidFor", "2019-11-02")
            .withParam("page", "0")
            .then()
            .headerContainsCorrelationId()
            .responseHasOkStatus()
            .doesNotContainNotPaidEntries()
            .containsReferenceNumbers()
            .containsPages(1)
            .hasFirstResultWith("AB11CDE")
            .hasSecondResultWith("ND84VSX")
            .totalLineItemsCountIsEqualTo(4);

        verifyResultsWereFetchedByOneDatabaseQuery();
      }

      @Test
      public void shouldReturnErrorForIncorrectPage() {
        PaymentInfoAssertion.whenRequested()
            .withParam("fromDatePaidFor", "2019-11-01")
            .withParam("toDatePaidFor", "2019-11-02")
            .withParam("page", "1")
            .then()
            .headerContainsCorrelationId()
            .responseHasBadRequestStatus()
            .containsErrors(1)
            .andContainsErrorWith(0, 400, "page",
                "Invalid page number",
                "Parameter validation error");
      }
    }
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn() {
    return Stream.of(
        Arguments.of("1993-04-18", "2019-11-02", 2),
        Arguments.of("2019-11-03", "2025-02-19", 3),
        Arguments.of("2019-11-03", "2031-09-22", 3)
    );
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> uncoveredTravelDatesRangeProviderForVrn() {
    return Stream.of(
        Arguments.of("1998-05-01", "2003-01-02"),
        Arguments.of("2019-11-07", "2025-02-19"),
        Arguments.of("2019-11-07", "2031-09-22")
    );
  }
}