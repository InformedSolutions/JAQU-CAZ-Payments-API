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
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.util.TestObjectFactory;

@ExtendWith(MockitoExtension.class)
public class VehicleEntrantRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private VehicleEntrantRepository vehicleEntrantRepository;

  @Nested
  class FindBy {

    @Test
    public void shouldThrowNullPointerExceptionWhenCazEntryDateIsNull() {
      // given
      LocalDate cazEntryDate = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantRepository.findBy(cazEntryDate, UUID
              .randomUUID(), "VRN123"));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("cazEntryDate cannot be null");
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenCleanZoneIdIsNull() {
      // given
      UUID cleanZoneId = null;

      // when
      Throwable throwable = catchThrowable(
          () -> vehicleEntrantRepository.findBy(LocalDate.now(), cleanZoneId, "VRN123"));

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
          () -> vehicleEntrantRepository.findBy(LocalDate.now(), UUID.randomUUID(), vrn));

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
          () -> vehicleEntrantRepository.findBy(LocalDate.now(), UUID.randomUUID(), vrn));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("VRN cannot be null or empty");
    }
  }

  @Nested
  class InsertIfNotExists {

    @Test
    public void shouldThrowNullPointerExceptionWhenVehicleEntrantIsNull() {
      // given
      VehicleEntrant vehicleEntrant = null;

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantRepository.insertIfNotExists(vehicleEntrant));

      // then
      assertThat(throwable).isInstanceOf(NullPointerException.class)
          .hasMessage("Vehicle Entrant cannot be null");
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenVehicleEntrantHasId() {
      // given
      UUID id = UUID.fromString("e3b6292a-260a-4679-aeb5-7e43a05486cd");
      VehicleEntrant vehicleEntrant = TestObjectFactory.VehicleEntrants.sampleEntrantWithId(id);

      // when
      Throwable throwable = catchThrowable(() -> vehicleEntrantRepository.insertIfNotExists(vehicleEntrant));

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vehicle Entrant cannot have ID");
    }
  }
}
