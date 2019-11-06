package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VehicleEntranceRepositoryTest {

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
}
