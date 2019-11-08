package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.psr.model.VehicleEntrance;

@ExtendWith(MockitoExtension.class)
public class VehicleEntranceRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private VehicleEntranceRepository vehicleEntranceRepository;

  @Nested
  class FindBy {

    @Test
    public void shouldThrowNullPointerExceptionWhenDateOfEntranceIsNull() {
      // given
      LocalDate dateOfEntrance = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntranceRepository.findBy(dateOfEntrance, UUID
              .randomUUID(), "VRN123"));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("dayOfEntrance cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
      // given
      UUID cleanZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntranceRepository.findBy(LocalDate.now(), cleanZoneId, "VRN123"));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("cleanZoneId cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenVrnIsEmpty() {
      // given
      String vrn = "";

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntranceRepository.findBy(LocalDate.now(), UUID.randomUUID(), vrn));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be null or empty");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenVrnIsNull() {
      // given
      String vrn = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntranceRepository.findBy(LocalDate.now(), UUID.randomUUID(), vrn));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be null or empty");
    }
  }

  @Nested
  class InsertIfNotExists {

    @Test
    public void shouldThrowNullPointerExceptionWhenVehicleEntranceIsNull() {
      // given
      VehicleEntrance vehicleEntrance = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntranceRepository.insertIfNotExists(vehicleEntrance));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle Entrance cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenVehicleEntranceHasId() {
      VehicleEntrance vehicleEntrance = VehicleEntrance.builder()
          .id(UUID.randomUUID())
          .build();

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntranceRepository.insertIfNotExists(vehicleEntrance));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle Entrance cannot have ID");
    }
  }
}
