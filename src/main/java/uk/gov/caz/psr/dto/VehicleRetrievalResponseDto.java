package uk.gov.caz.psr.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

@Builder
@Value
public class VehicleRetrievalResponseDto {
  
  int page;
  int pageCount;
  int perPage;
  long totalVrnsCount;
  List<ComplianceResultsDto> vehicles;

}
