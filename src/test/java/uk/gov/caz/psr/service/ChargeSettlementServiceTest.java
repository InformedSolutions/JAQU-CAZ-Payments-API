package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.model.PaymentStatus;
import uk.gov.caz.psr.repository.PaymentStatusRepository;
import uk.gov.caz.psr.util.TestObjectFactory.PaymentStatusFactory;

@ExtendWith(MockitoExtension.class)
class ChargeSettlementServiceTest {

  public static final String ANY_VRN = "CAS123";
  public static final UUID ANY_CAZ_ID = UUID.randomUUID();
  public static final LocalDate ANY_DATE = LocalDate.now();

  @Mock
  private PaymentStatusRepository paymentStatusRepository;

  @InjectMocks
  private ChargeSettlementService chargeSettlementService;

  @Test
  void shouldReturnNotPaidWhenRepositoryReturnsEmptyCollection() {
    //given
    mockEmptyCollection();

    //when
    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    //then
    assertThat(paymentStatus.getStatus()).isEqualTo(InternalPaymentStatus.NOT_PAID);
  }

  @Test
  void shouldReturnPaidPaymentStatusWhenPaidEntrantPaymentExists() {
    // given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.PAID);

    // when
    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    // then
    assertThat(paymentStatus.getStatus()).isEqualTo(InternalPaymentStatus.PAID);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsNoPaidOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.NOT_PAID);

    //when
    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    assertThat(paymentStatus.getStatus()).isEqualTo(InternalPaymentStatus.NOT_PAID);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsRefundedOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.REFUNDED);

    //when
    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    assertThat(paymentStatus.getStatus()).isEqualTo(InternalPaymentStatus.REFUNDED);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsChargebackOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.CHARGEBACK);

    //when
    PaymentStatus paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    assertThat(paymentStatus.getStatus()).isEqualTo(InternalPaymentStatus.CHARGEBACK);
  }

  private void mockEmptyCollection() {
    when(paymentStatusRepository.findByCazIdAndVrnAndEntryDate(ANY_CAZ_ID, ANY_VRN, ANY_DATE))
        .thenReturn(Collections.emptyList());
  }

  private void mockSingleEntrantPaymentForStatus(InternalPaymentStatus status) {
    when(paymentStatusRepository.findByCazIdAndVrnAndEntryDate(ANY_CAZ_ID, ANY_VRN, ANY_DATE))
        .thenReturn(
            Arrays.asList(PaymentStatusFactory.anyWithStatus(status)));
  }
}
