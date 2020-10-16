package uk.gov.caz.psr.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.caz.psr.controller.exception.PaymentInfoVrnValidationException;
import uk.gov.caz.psr.model.PaymentInfoRequestAttributes;
import uk.gov.caz.psr.model.info.EntrantPaymentMatchInfo;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;
import uk.gov.caz.psr.repository.jpa.EntrantPaymentMatchInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoSpecification;

@ExtendWith(MockitoExtension.class)
class ChargeSettlementPaymentInfoServiceTest {

  @Mock
  private EntrantPaymentMatchInfoRepository entrantPaymentMatchInfoRepository;

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;

  @Spy
  private List<PaymentInfoSpecification> specifications = new ArrayList<>();

  @InjectMocks
  private ChargeSettlementPaymentInfoService paymentInfoService;

  @Test
  public void shouldReturnListOfPaymentInfo() {
    // given
    PaymentInfoRequestAttributes input = PaymentInfoRequestAttributes.builder().build();
    when(entrantPaymentMatchInfoRepository.findAll(Mockito.any(Specification.class)))
        .thenReturn(emptyList());

    //when
    List<EntrantPaymentMatchInfo> any = paymentInfoService
        .findPaymentInfo(input, UUID.randomUUID());

    //then
    assertThat(any).isEqualTo(emptyList());
  }

  @Test
  public void shouldThrowPaymentInfoVrnValidationExceptionWhenVrnIsMissing() {
    // given
    PaymentInfoRequestAttributes input = PaymentInfoRequestAttributes.builder()
        .vrn("CAS123")
        .build();
    when(entrantPaymentRepository.countByVrnAndCaz(any(), anyString())).thenReturn(0);

    // when
    Throwable throwable = catchThrowable(
        () -> paymentInfoService.findPaymentInfo(input, UUID.randomUUID()));

    // then
    assertThat(throwable)
        .isInstanceOf(PaymentInfoVrnValidationException.class)
        .hasMessage("vrn cannot be found");
  }
}
