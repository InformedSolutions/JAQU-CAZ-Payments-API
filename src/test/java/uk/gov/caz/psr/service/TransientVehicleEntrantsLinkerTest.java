package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.model.Payment;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.repository.VehicleEntrantRepository;
import uk.gov.caz.psr.util.TestObjectFactory;
import uk.gov.caz.psr.util.TestObjectFactory.Payments;

@ExtendWith(MockitoExtension.class)
public class TransientVehicleEntrantsLinkerTest {

  @Mock
  private VehicleEntrantRepository vehicleEntrantRepository;

  @InjectMocks
  private TransientVehicleEntrantsLinker transientVehicleEntrantsLinker;

  @Test
  public void shouldThrowNullPointerExceptionWhenPaymentIsNull() {
    // given
    Payment payment = null;

    // when
    Throwable throwable = catchThrowable(
        () -> transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(payment));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Payment cannot be null");
  }

  @Test
  public void shouldReturnProvidedPaymentWhenStatusNotSuccess() {
    // given
    Payment payment = Payments.existing();

    // when
    Payment result = transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(payment);

    // then
    assertThat(result).isEqualTo(payment);
    verify(vehicleEntrantRepository, never()).findBy(any(), any(), any());
  }

  @Test
  public void shouldReturnProvidedPaymentWhenStatusSuccessButNoEntrantsFound() {
    // given
    Payment payment = createSuccessPayment();
    mockNotFoundVehicleEntrant();

    // when
    Payment result = transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(payment);

    // then
    assertThat(result).isEqualTo(payment);
    assertThat(result.getVehicleEntrantPayments().get(1).getVehicleEntrantId()).isNull();
    verify(vehicleEntrantRepository, times(payment.getVehicleEntrantPayments().size()))
        .findBy(any(), any(), any());
  }

  @Test
  public void shouldReturnUpdatedPaymentWhenStatusSuccessAndEntrantsFound() {
    // given
    Payment payment = createSuccessPayment();
    mockFoundVehicleEntrant();

    // when
    Payment result = transientVehicleEntrantsLinker.associateExistingVehicleEntrantsWith(payment);

    // then
    assertThat(result).isNotEqualTo(payment);
    assertThat(result.getVehicleEntrantPayments().get(1).getVehicleEntrantId()).isNotNull();
    verify(vehicleEntrantRepository, times(payment.getVehicleEntrantPayments().size()))
        .findBy(any(), any(), any());
  }

  private Payment createSuccessPayment() {
    return Payments.existing().toBuilder()
        .externalPaymentStatus(ExternalPaymentStatus.SUCCESS)
        .authorisedTimestamp(LocalDateTime.now())
        .build();
  }

  private Optional<VehicleEntrant> createVehicleEntrant() {
    return Optional.of(TestObjectFactory.VehicleEntrants.anyWithId());
  }

  private void mockNotFoundVehicleEntrant() {
    given(vehicleEntrantRepository.findBy(any(), any(), any())).willReturn(Optional.empty());
  }

  private void mockFoundVehicleEntrant() {
    given(vehicleEntrantRepository.findBy(any(), any(), any())).willReturn(
        createVehicleEntrant()
    );
  }
}
