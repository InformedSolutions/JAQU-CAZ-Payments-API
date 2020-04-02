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
import org.jetbrains.annotations.NotNull;
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
import uk.gov.caz.psr.model.PaymentMethod;
import uk.gov.caz.psr.model.info.EntrantPaymentInfo;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.model.info.PaymentInfo;

@ExtendWith(MockitoExtension.class)
class EntrantPaymentInfoConverterTest {

  private static final String ANY_VRN = "YC31QBL";
  private static final String ANY_CASE_REFERENCE = "case-reference";
  private static final LocalDateTime ANY_SUBMITTED_TIMESTAMP = LocalDateTime.of(2019, 10, 10, 19, 20);
  private static final String ANY_EXTERNAL_ID = "external-id";
  private static final int ANY_TOTAL_PAID = 74;
  private static final LocalDate ANY_TRAVEL_DATE = LocalDate.parse("2019-10-10");

  @Mock
  private CurrencyFormatter currencyFormatter;

  @InjectMocks
  private EntrantPaymentInfoConverter converter;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedValueIsNull() {
    // given
    Collection<EntrantPaymentMatchInfo> input = null;

    // when
    Throwable throwable = catchThrowable(() -> converter.toPaymentInfoResponse(input));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("entrantPaymentMatchInfos cannot be null");
  }

  @Test
  public void shouldConvertPassedCollectionWhenSubmittedTimestampIsNull() {
    // given
    Collection<EntrantPaymentMatchInfo> input = singleEntrantPaymentMatchInfoWithNullTimestamp();
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(74));

    // when
    PaymentInfoResponse paymentInfoResponse = converter.toPaymentInfoResponse(input);

    // then
    assertThat(paymentInfoResponse).isNotNull();
    assertThat(paymentInfoResponse.getResults()).hasSize(1);
    PaymentsInfo paymentsInfo = paymentInfoResponse.getResults().iterator().next();
    assertThat(paymentsInfo.getPayments()).hasSize(1);
    SinglePaymentInfo payment = paymentsInfo.getPayments().iterator().next();
    assertThat(payment.getPaymentDate()).isNull();
  }

  @Test
  public void shouldConvertPassedCollection() {
    // given
    Collection<EntrantPaymentMatchInfo> input = singleEntrantPaymentInfo(PaymentMethod.DIRECT_DEBIT);
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
    assertThat(payment.getPaymentMandateId()).isEqualTo(ANY_EXTERNAL_ID);
    assertThat(payment.isTelephonePayment()).isFalse();
    assertThat(payment.getTotalPaid()).isEqualTo(BigDecimal.valueOf(ANY_TOTAL_PAID));
    assertThat(payment.getLineItems()).hasSize(1);
    SinglePaymentInfo.VehicleEntrantPaymentInfo lineItem = payment.getLineItems().iterator().next();
    assertThat(lineItem.getCaseReference()).isEqualTo(ANY_CASE_REFERENCE);
    assertThat(lineItem.getChargePaid()).isEqualTo(BigDecimal.valueOf(ANY_TOTAL_PAID));
    assertThat(lineItem.getTravelDate()).isEqualTo(ANY_TRAVEL_DATE);
    assertThat(lineItem.getPaymentStatus()).isEqualTo(ChargeSettlementPaymentStatus.REFUNDED);
  }

  @Test
  public void shouldNotIncludePaymentMandateIdIfPaymentMethodIsNotDirectDebit() {
    // given
    Collection<EntrantPaymentMatchInfo> input = singleEntrantPaymentInfo(PaymentMethod.CREDIT_DEBIT_CARD);
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(74));

    // when
    PaymentInfoResponse paymentInfoResponse = converter.toPaymentInfoResponse(input);

    // then
    assertThat(paymentInfoResponse).isNotNull();
    assertThat(paymentInfoResponse.getResults()).hasSize(1);
    PaymentsInfo paymentsInfo = paymentInfoResponse.getResults().iterator().next();
    assertThat(paymentsInfo.getPayments()).hasSize(1);
    SinglePaymentInfo payment = paymentsInfo.getPayments().iterator().next();
    assertThat(payment.getPaymentMandateId()).isNull();
  }

  private Collection<EntrantPaymentMatchInfo> singleEntrantPaymentInfo(PaymentMethod directDebit) {
    EntrantPaymentMatchInfo result = buildEntrantPaymentMatchInfo(directDebit);
    return Collections.singletonList(result);
  }

  private Collection<EntrantPaymentMatchInfo> singleEntrantPaymentMatchInfoWithNullTimestamp() {
    EntrantPaymentMatchInfo result = buildEntrantPaymentMatchInfoWithNullSubmittedTimestamp();
    return Collections.singletonList(result);
  }

  private EntrantPaymentMatchInfo buildEntrantPaymentMatchInfo(PaymentMethod directDebit) {
    EntrantPaymentMatchInfo result = new EntrantPaymentMatchInfo();
    result.setId(UUID.fromString("7e2bf5c2-3cfc-11ea-b5aa-f7f8fb54cc82"));
    result.setLatest(true);
    result.setEntrantPaymentInfo(buildEntrantPaymentInfo());
    result.setPaymentInfo(buildPaymentInfo(directDebit));
    return result;
  }

  @NotNull
  private EntrantPaymentInfo buildEntrantPaymentInfo() {
    EntrantPaymentInfo entrantPaymentInfo = new EntrantPaymentInfo();
    entrantPaymentInfo.setId(UUID.fromString("0af851ef-870e-4c2e-b9aa-f84c1db35f24"));
    entrantPaymentInfo.setVrn(ANY_VRN);
    entrantPaymentInfo.setChargePaid(ANY_TOTAL_PAID);
    entrantPaymentInfo.setCaseReference(ANY_CASE_REFERENCE);
    entrantPaymentInfo.setPaymentStatus(InternalPaymentStatus.REFUNDED);
    entrantPaymentInfo.setTravelDate(ANY_TRAVEL_DATE);
    entrantPaymentInfo.setCleanAirZoneId(UUID.fromString("938cac88-1103-11ea-a1a6-33ad4299653d"));
    return entrantPaymentInfo;
  }

  private EntrantPaymentMatchInfo buildEntrantPaymentMatchInfoWithNullSubmittedTimestamp() {
    EntrantPaymentMatchInfo result = buildEntrantPaymentMatchInfo(PaymentMethod.DIRECT_DEBIT);
    result.setPaymentInfo(buildPaymentInfoWith(null, PaymentMethod.DIRECT_DEBIT));
    return result;
  }

  private PaymentInfo buildPaymentInfo(PaymentMethod paymentMethod) {
    return buildPaymentInfoWith(ANY_SUBMITTED_TIMESTAMP, paymentMethod);
  }

  private PaymentInfo buildPaymentInfoWith(LocalDateTime timestamp, PaymentMethod paymentMethod) {
    PaymentInfo paymentInfo = new PaymentInfo();
    paymentInfo.setId(UUID.fromString("996c6c95-960d-4dfd-98a2-6effa9a1cbda"));
    paymentInfo.setExternalId(ANY_EXTERNAL_ID);
    paymentInfo.setTotalPaid(EntrantPaymentInfoConverterTest.ANY_TOTAL_PAID);
    paymentInfo.setExternalPaymentStatus(ExternalPaymentStatus.SUCCESS);
    paymentInfo.setPaymentMethod(paymentMethod);
    paymentInfo.setSubmittedTimestamp(timestamp);
    paymentInfo.setPaymentProviderMandateId(ANY_EXTERNAL_ID);
    return paymentInfo;
  }
}