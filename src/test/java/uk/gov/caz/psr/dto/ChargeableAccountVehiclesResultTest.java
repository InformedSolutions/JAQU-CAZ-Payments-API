package uk.gov.caz.psr.dto;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;

public class ChargeableAccountVehiclesResultTest {
  
  @Test
  public void testThrowsExceptionWhenDtoIsBuiltIfCleanAirZoneIdUnrecognised() {
    ComplianceOutcomeDto outcome = ComplianceOutcomeDto.builder()
        .cleanAirZoneId(UUID.randomUUID())
        .build();
    ComplianceResultsDto result = ComplianceResultsDto.builder()
        .complianceOutcomes(Collections.singletonList(outcome))
        .build();
    assertThrows(CleanAirZoneNotFoundException.class, () -> ChargeableAccountVehiclesResult
        .buildVrnWithTariffAndEntrancesPaidFrom(result, UUID.randomUUID()));
  }

}
