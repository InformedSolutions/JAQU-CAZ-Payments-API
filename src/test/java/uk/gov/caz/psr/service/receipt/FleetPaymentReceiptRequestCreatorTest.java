package uk.gov.caz.psr.service.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;
import uk.gov.caz.psr.util.TestObjectFactory.EntrantPayments;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class FleetPaymentReceiptRequestCreatorTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private CleanAirZoneService cleanAirZoneNameGetterService;

  @Mock
  private ObjectMapper objectMapper;

  private FleetPaymentReceiptRequestCreator fleetPaymentReceiptRequestCreator;

  @BeforeEach
  public void setUpReceiptRequestCreator() {
    fleetPaymentReceiptRequestCreator = new FleetPaymentReceiptRequestCreator(
        currencyFormatter, cleanAirZoneNameGetterService, objectMapper, "some-template"
    );
  }

  @Nested
  class PersonalisationPayload {

    @Test
    public void shouldReturnCorrectPersonalisationPayload() {
      // given
      Payment payment = paymentWithTravelDates(LocalDate.of(2019, 1, 1));
      String vrn = payment.getEntrantPayments().get(0).getVrn();
      String cazName = mockGettingCazName();
      when(currencyFormatter.parsePennies(anyInt())).thenReturn(100.0);

      // when
      Map<String, Object> result = fleetPaymentReceiptRequestCreator
          .createPersonalisationPayload(payment);

      // then
      assertThat(result).containsEntry("caz", cazName);
      assertThat(result).containsEntry("external_id", payment.getExternalId());
      assertThat(result).containsEntry("reference", payment.getReferenceNumber().toString());
      assertThat(result).containsEntry("amount", "100.00");
      assertThat(result).extracting("charges")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly("01 January 2019 - " + vrn + " - £100.00");
    }

    @Test
    public void shouldReturnChargesSortedByTravelDate() {
      // given
      Payment payment = paymentWithTravelDates(
          LocalDate.of(2019, 2, 1),
          LocalDate.of(2018, 2, 1),
          LocalDate.of(2017, 2, 1)
      );
      mockGettingCazName();
      String vrn = payment.getEntrantPayments().get(0).getVrn();
      when(currencyFormatter.parsePennies(anyInt())).thenReturn(100.0);

      // when
      Map<String, Object> result = fleetPaymentReceiptRequestCreator
          .createPersonalisationPayload(payment);

      // then
      assertThat(result).extracting("charges")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly(
              "01 February 2017 - " + vrn + " - £100.00",
              "01 February 2018 - " + vrn + " - £100.00",
              "01 February 2019 - " + vrn + " - £100.00"
          );
    }

    @Test
    public void shouldReturnChargesSortedByTravelDateAndVrn() {
      // given
      Payment payment = paymentWithTravelDatesAndVrn(
          "ABC", LocalDate.of(2019, 2, 1),
          "CDE", LocalDate.of(2019, 2, 1),
          "ABC", LocalDate.of(2017, 2, 1)
      );
      mockGettingCazName();
      when(currencyFormatter.parsePennies(anyInt())).thenReturn(100.0);

      // when
      Map<String, Object> result = fleetPaymentReceiptRequestCreator
          .createPersonalisationPayload(payment);

      // then
      assertThat(result).extracting("charges")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly(
              "01 February 2017 - ABC - £100.00",
              "01 February 2019 - ABC - £100.00",
              "01 February 2019 - CDE - £100.00"
          );
    }

    private String mockGettingCazName() {
      String cazName = "caz-name";
      when(cleanAirZoneNameGetterService.fetch(any())).thenReturn(cazName);
      return cazName;
    }
  }

  private Payment paymentWithTravelDatesAndVrn(String vrn1, LocalDate travelDate1,
      String vrn2, LocalDate travelDate2, String vrn3, LocalDate travelDate3) {
    List<EntrantPayment> entrantPayments = Arrays.asList(
        EntrantPayments.forTravelDate(travelDate1, vrn1),
        EntrantPayments.forTravelDate(travelDate2, vrn2),
        EntrantPayments.forTravelDate(travelDate3, vrn3)
    );
    return paymentWithRandomUserId()
        .toBuilder()
        .entrantPayments(entrantPayments)
        .build();
  }

  private Payment paymentWithRandomUserId() {
    return paymentWithTravelDates(LocalDate.now());
  }

  private Payment paymentWithTravelDates(LocalDate ... paymentDates) {
    return Payments.forDays(Arrays.asList(paymentDates), UUID.randomUUID())
        .toBuilder()
        .userId(UUID.randomUUID())
        .externalId("ext-id")
        .referenceNumber(1005L)
        .build();
  }

  @Nested
  class Applicability {

    @Test
    public void shouldReturnTrueIfUserIdIsNotNull() {
      // given
      Payment payment = paymentWithRandomUserId();

      // when
      boolean result = fleetPaymentReceiptRequestCreator.isApplicableFor(payment);

      // then
      assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserIdIsNull() {
      // given
      Payment payment = paymentWithNullUserId();

      // when
      boolean result = fleetPaymentReceiptRequestCreator.isApplicableFor(payment);

      // then
      assertThat(result).isFalse();
    }

    private Payment paymentWithNullUserId() {
      return Payments.forRandomDays().toBuilder().userId(null).build();
    }
  }
}