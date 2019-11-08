package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.repository.VehicleEntrantRepository;
import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrants;

@ExtendWith(MockitoExtension.class)
class VehicleEntrantServiceTest {

  @Mock
  private VehicleEntrantRepository vehicleEntrantRepository;

  @Mock
  private FinalizeVehicleEntrantService finalizeVehicleEntrantService;

  @InjectMocks
  private VehicleEntrantService vehicleEntrantService;

  @Test
  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
    // given
    VehicleEntrant vehicleEntrant = null;

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleEntrantService.registerVehicleEntrant(vehicleEntrant));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Vehicle entrant cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenVehicleEntrantIdIsNotNull() {
    // given
    UUID id = UUID.fromString("1ada0539-7528-456e-95eb-f14025792889");
    VehicleEntrant vehicleEntrant = VehicleEntrants.sampleEntrantWithId(id);

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleEntrantService.registerVehicleEntrant(vehicleEntrant));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ID of the vehicle entrant must be null, actual: " + id);
  }

  @Test
  public void shouldCallRepositoryWhenVehicleEntrantIsValid() {
    // given
    VehicleEntrant vehicleEntrant = VehicleEntrants.SAMPLE_ENTRANT;

    // when
    vehicleEntrantService.registerVehicleEntrant(vehicleEntrant);

    // then
    verify(vehicleEntrantRepository).insertIfNotExists(vehicleEntrant);
  }
}