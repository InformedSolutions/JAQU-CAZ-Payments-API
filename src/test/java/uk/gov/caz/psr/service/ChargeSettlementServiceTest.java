package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
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
  void shouldReturnEmptyElementWhenRepositoryReturnsEmptyCollection() {
    //given
    mockEmptyCollection();

    //when
    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    //then
    assertThat(paymentStatus).isEqualTo(Optional.empty());
  }

  @Test
  void shouldReturnPaidPaymentStatusWhenPaidEntrantPaymentExists() {
    // given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.PAID);

    // when
    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    // then
    assertThat(paymentStatus).map(PaymentStatus::getStatus).contains(InternalPaymentStatus.PAID);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsNoPaidOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.NOT_PAID);

    //when
    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    assertThat(paymentStatus).map(PaymentStatus::getStatus)
        .contains(InternalPaymentStatus.NOT_PAID);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsRefundedOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.REFUNDED);

    //when
    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);

    assertThat(paymentStatus).map(PaymentStatus::getStatus)
        .contains(InternalPaymentStatus.REFUNDED);
  }

  @Test
  void shouldReturnTheExistingPaymentStatusWhenThereIsChargebackOne() {
    //given
    mockSingleEntrantPaymentForStatus(InternalPaymentStatus.CHARGEBACK);

    //when
    Optional<PaymentStatus> paymentStatus = chargeSettlementService
        .findChargeSettlement(ANY_CAZ_ID, ANY_VRN, ANY_DATE);
    
    assertThat(paymentStatus).map(PaymentStatus::getStatus)
        .contains(InternalPaymentStatus.CHARGEBACK);
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
