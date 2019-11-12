package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.psr.annotation.IntegrationTest;
import uk.gov.caz.psr.model.VehicleEntrant;
import uk.gov.caz.psr.util.TestObjectFactory.VehicleEntrants;

@IntegrationTest
class VehicleEntrantRepositoryTestIT {

  @Autowired
  private VehicleEntrantRepository repository;

  @Nested
  class Insert {

    @Nested
    class WhenDuplicatedRowIsInserted {

      @Test
      public void shouldNotThrowException() {
        // given
        VehicleEntrant vehicleEntrant = VehicleEntrants.anyWithoutId();

        // when
        Throwable throwable = catchThrowable(() -> {
          repository.insertIfNotExists(vehicleEntrant);
          repository.insertIfNotExists(vehicleEntrant);
        });

        // then
        assertThat(throwable).isNull();
      }
    }
  }
}