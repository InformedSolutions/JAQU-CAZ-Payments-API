package uk.gov.caz.psr.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import uk.gov.caz.psr.dto.PaymentInfoRequest;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;
import uk.gov.caz.psr.repository.jpa.VehicleEntrantPaymentInfoRepository;
import uk.gov.caz.psr.service.paymentinfo.PaymentInfoSpecification;

@ExtendWith(MockitoExtension.class)
class ChargeSettlementPaymentInfoServiceTest {

  @Mock
  private VehicleEntrantPaymentInfoRepository vehicleEntrantPaymentInfoRepository;

  @Spy
  private List<PaymentInfoSpecification> specifications = new ArrayList<>();

  @InjectMocks
  private ChargeSettlementPaymentInfoService paymentInfoService;

  @Test
  void shouldReturnListOfPaymentInfo() {
    // given
    PaymentInfoRequest input = PaymentInfoRequest.builder().build();
    when(vehicleEntrantPaymentInfoRepository.findAll(Mockito.any(Specification.class))).thenReturn(emptyList());

    //when
    List<VehicleEntrantPaymentInfo> any = paymentInfoService.findPaymentInfo(input, UUID.randomUUID());

    //then
    assertThat(any).isEqualTo(emptyList());
  }
}