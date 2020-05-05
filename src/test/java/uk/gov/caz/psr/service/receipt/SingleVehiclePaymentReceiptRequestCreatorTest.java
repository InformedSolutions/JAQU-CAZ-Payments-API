package uk.gov.caz.psr.service.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.util.CurrencyFormatter;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
class SingleVehiclePaymentReceiptRequestCreatorTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private CleanAirZoneService cleanAirZoneNameGetterService;

  @Mock
  private ObjectMapper objectMapper;

  private SingleVehiclePaymentReceiptRequestCreator singleVehiclePaymentReceiptRequestCreator;

  @BeforeEach
  public void setUpReceiptRequestCreator() {
    singleVehiclePaymentReceiptRequestCreator = new SingleVehiclePaymentReceiptRequestCreator(
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
      Map<String, Object> result = singleVehiclePaymentReceiptRequestCreator
          .createPersonalisationPayload(payment);

      // then
      assertThat(result).containsEntry("caz", cazName);
      assertThat(result).containsEntry("external_id", payment.getExternalId());
      assertThat(result).containsEntry("reference", payment.getReferenceNumber().toString());
      assertThat(result).containsEntry("amount", "100.00");
      assertThat(result).containsEntry("vrn", vrn);
      assertThat(result).extracting("date")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly("01 January 2019");
    }

    private String mockGettingCazName() {
      String cazName = "caz-name";
      when(cleanAirZoneNameGetterService.fetch(any())).thenReturn(cazName);
      return cazName;
    }
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
    public void shouldReturnFalseIfUserIdIsNotNull() {
      // given
      Payment payment = paymentWithRandomUserId();

      // when
      boolean result = singleVehiclePaymentReceiptRequestCreator.isApplicableFor(payment);

      // then
      assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnTrueIfUserIdIsNull() {
      // given
      Payment payment = paymentWithNullUserId();

      // when
      boolean result = singleVehiclePaymentReceiptRequestCreator.isApplicableFor(payment);

      // then
      assertThat(result).isTrue();
    }

    private Payment paymentWithNullUserId() {
      return Payments.forRandomDays().toBuilder().userId(null).build();
    }
  }

}