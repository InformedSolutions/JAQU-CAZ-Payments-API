package uk.gov.caz.psr;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.psr.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.psr.controller.ChargeSettlementController;
import uk.gov.caz.psr.util.PaymentInfoAssertion;

@FullyRunningServerIntegrationTest
class ChargeSettlementPaymentInfoV1TestIT extends ChargeSettlementPaymentInfoTest {

  @Override
  protected String getPaymentInfoPath() {
    return ChargeSettlementController.PAYMENT_INFO_PATH_V1;
  }

  @Nested
  class PaymentInfoV1 {

    @Nested
    class WhenRequestedWithVrn {

      @Nested
      class WhichExists {

        @Nested
        class WhenRequestedWithDatesRange {

          @Nested
          class WhichIsCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoV1TestIT#coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn")
            public void shouldReturnDataForRequestedDaysRange(String fromDatePaidForAsString,
                String toDatePaidForAsString, int expectedLineItemsCount, boolean expectedFailedPayments) {
              String vrn = "ND84VSX";
              PaymentInfoAssertion.whenRequested()
                  .withParam("vrn", vrn)
                  .withParam("fromDatePaidFor", fromDatePaidForAsString)
                  .withParam("toDatePaidFor", toDatePaidForAsString)
                  .then()
                  .headerContainsCorrelationId()
                  .responseHasOkStatus()
                  .doesNotContainNotPaidEntries()
                  .containsMarkedAsFailedPayments(expectedFailedPayments)
                  .containsReferenceNumbers()
                  .totalLineItemsCountIsEqualTo(expectedLineItemsCount);

              verifyResultsWereFetchedByOneDatabaseQuery();
            }
          }

          @Nested
          class WhichIsNotCovered {

            @ParameterizedTest
            @MethodSource("uk.gov.caz.psr.ChargeSettlementPaymentInfoV1TestIT#uncoveredTravelDatesRangeProviderForVrn")
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
    class WhenRequestedForOnlyDatesRange {

      @Nested
      class AndEmptyDateRange {

        @Test
        public void shouldReturnEmptyArray() {

          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", "1998-05-01")
              .withParam("toDatePaidFor", "2003-01-02")
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .containsEmptyResults();

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }

      @Nested
      class AndCoveredDateRange {

        @Test
        public void shouldReturnDataForRequestedDaysRange() {
          PaymentInfoAssertion.whenRequested()
              .withParam("fromDatePaidFor", "1993-04-18")
              .withParam("toDatePaidFor", "2019-11-02")
              .then()
              .headerContainsCorrelationId()
              .responseHasOkStatus()
              .doesNotContainNotPaidEntries()
              .containsReferenceNumbers()
              .totalLineItemsCountIsEqualTo(4);

          verifyResultsWereFetchedByOneDatabaseQuery();
        }
      }
    }
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> coveredTravelDatesRangeWithExpectedLineItemsCountProviderForVrn() {
    return Stream.of(
        Arguments.of("1993-04-18", "2019-11-02", 2, false),
        Arguments.of("2019-11-03", "2025-02-19", 4, true),
        Arguments.of("2019-11-03", "2031-09-22", 4, true)
    );
  }

  // data for VRN ND84VSX
  public static Stream<Arguments> uncoveredTravelDatesRangeProviderForVrn() {
    return Stream.of(
        Arguments.of("1998-05-01", "2003-01-02"),
        Arguments.of("2019-11-11", "2025-02-19"),
        Arguments.of("2019-11-11", "2031-09-22")
    );
  }
}