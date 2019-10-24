package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.model.VehicleEntrance;
import uk.gov.caz.psr.repository.VehicleEntranceRepository;

@ExtendWith(MockitoExtension.class)
class VehicleEntranceServiceTest {

  @Mock
  private VehicleEntranceRepository vehicleEntranceRepository;

  @InjectMocks
  private VehicleEntranceService vehicleEntranceService;

  @Test
  public void shouldThrowNullPointerExceptionWhenVehicleEntranceIsNull() {
    // given
    VehicleEntrance vehicleEntrance = null;

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleEntranceService.registerVehicleEntrance(vehicleEntrance));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("Vehicle entrance cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenVehicleEntranceIdIsNotNull() {
    // given
    UUID id = UUID.fromString("1ada0539-7528-456e-95eb-f14025792889");
    VehicleEntrance vehicleEntrance = VehicleEntrance.builder()
        .vrn("BW91HUN")
        .dateOfEntrance(LocalDate.now())
        .id(id)
        .cleanZoneId(UUID.randomUUID())
        .build();

    // when
    Throwable throwable = catchThrowable(
        () -> vehicleEntranceService.registerVehicleEntrance(vehicleEntrance));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ID of the vehicle entrance must be null, actual: " + id);
  }

  @Test
  public void shouldCallRepositoryWhenVehicleEntranceIsValid() {
    // given
    VehicleEntrance vehicleEntrance = VehicleEntrance.builder()
        .vrn("BW91HUN")
        .dateOfEntrance(LocalDate.now())
        .cleanZoneId(UUID.randomUUID())
        .build();

    // when
    vehicleEntranceService.registerVehicleEntrance(vehicleEntrance);

    // then
    verify(vehicleEntranceRepository).insertIfNotExists(vehicleEntrance);
  }
}