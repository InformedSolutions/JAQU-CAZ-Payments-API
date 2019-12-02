package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.ChargeSettlementPaymentStatus;
import uk.gov.caz.psr.dto.PaymentInfoResponse;
import uk.gov.caz.psr.dto.PaymentInfoResponse.PaymentsInfo;
import uk.gov.caz.psr.dto.PaymentInfoResponse.SinglePaymentInfo;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantPaymentInfoConverterTest {

  private static final String ANY_VRN = "YC31QBL";
  private static final String ANY_CASE_REFERENCE = "case-reference";
  private static final LocalDateTime ANY_SUBMITTED_TIMESTAMP = LocalDateTime.of(2019, 10, 10, 19, 20);
  private static final String ANY_EXTERNAL_ID = "external-id";
  private static final int ANY_TOTAL_PAID = 74;
  private static final LocalDate ANY_TRAVEL_DATE = LocalDate.parse("2019-10-10");

  @Mock
  private CurrencyFormatter currencyFormatter;

  @InjectMocks
  private VehicleEntrantPaymentInfoConverter converter;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedValueIsNull() {
    // given
    Collection<VehicleEntrantPaymentInfo> input = null;

    // when
    Throwable throwable = catchThrowable(() -> converter.toPaymentInfoResponse(input));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("vehicleEntrantPaymentInfos cannot be null");
  }

  @Test
  public void shouldConvertPassedCollection() {
    // given
    Collection<VehicleEntrantPaymentInfo> input = singleVehicleEntrantPaymentInfo();
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(74));

    // when
    PaymentInfoResponse paymentInfoResponse = converter.toPaymentInfoResponse(input);

    // then
    assertThat(paymentInfoResponse).isNotNull();
    assertThat(paymentInfoResponse.getResults()).hasSize(1);
    PaymentsInfo paymentsInfo = paymentInfoResponse.getResults().iterator().next();
    assertThat(paymentsInfo.getVrn()).isEqualTo(ANY_VRN);
    assertThat(paymentsInfo.getPayments()).hasSize(1);
    SinglePaymentInfo payment = paymentsInfo.getPayments().iterator().next();
    assertThat(payment.getPaymentDate()).isEqualTo(ANY_SUBMITTED_TIMESTAMP.toLocalDate());
    assertThat(payment.getPaymentProviderId()).isEqualTo(ANY_EXTERNAL_ID);
    assertThat(payment.getTotalPaid()).isEqualTo(BigDecimal.valueOf(ANY_TOTAL_PAID));
    assertThat(payment.getLineItems()).hasSize(1);
    SinglePaymentInfo.VehicleEntrantPaymentInfo lineItem = payment.getLineItems().iterator().next();
    assertThat(lineItem.getCaseReference()).isEqualTo(ANY_CASE_REFERENCE);
    assertThat(lineItem.getChargePaid()).isEqualTo(BigDecimal.valueOf(ANY_TOTAL_PAID));
    assertThat(lineItem.getTravelDate()).isEqualTo(ANY_TRAVEL_DATE);
    assertThat(lineItem.getChargeSettlementPaymentStatus()).isEqualTo(ChargeSettlementPaymentStatus.REFUNDED);
  }

  private Collection<VehicleEntrantPaymentInfo> singleVehicleEntrantPaymentInfo() {
    VehicleEntrantPaymentInfo result = new VehicleEntrantPaymentInfo();
    result.setId(UUID.fromString("0af851ef-870e-4c2e-b9aa-f84c1db35f24"));
    result.setVrn(ANY_VRN);
    result.setChargePaid(ANY_TOTAL_PAID);
    result.setCaseReference(ANY_CASE_REFERENCE);
    result.setPaymentStatus(InternalPaymentStatus.REFUNDED);
    result.setTravelDate(ANY_TRAVEL_DATE);
    result.setCleanAirZoneId(UUID.fromString("938cac88-1103-11ea-a1a6-33ad4299653d"));
    result.setPaymentInfo(buildPaymentInfo());
    return Collections.singletonList(result);
  }

  private PaymentInfo buildPaymentInfo() {
    PaymentInfo paymentInfo = new PaymentInfo();
    paymentInfo.setId(UUID.fromString("996c6c95-960d-4dfd-98a2-6effa9a1cbda"));
    paymentInfo.setExternalId(ANY_EXTERNAL_ID);
    paymentInfo.setTotalPaid(VehicleEntrantPaymentInfoConverterTest.ANY_TOTAL_PAID);
    paymentInfo.setExternalPaymentStatus(ExternalPaymentStatus.SUCCESS);
    paymentInfo.setSubmittedTimestamp(ANY_SUBMITTED_TIMESTAMP);
    return paymentInfo;
  }
}