package uk.gov.caz.psr.service;

import com.google.common.base.Preconditions;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse.CleanAirZoneDto;
import uk.gov.caz.psr.repository.VccsRepository;

/**
 * Class responsible to call vccs for clean air zones and find a name of selected zone in the
 * response list.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CleanAirZoneNameGetterService {

  private final VccsRepository vccsRepository;

  /**
   * Gets name of clean air zone name from vccs.
   *
   * @param cleanAirZoneId id of clean air zone
   * @return {@link String} if the CleanAirZone exist
   * @throws NullPointerException if {@code cleanAirZoneId} is null
   */
  public String fetch(UUID cleanAirZoneId) {
    Preconditions.checkNotNull(cleanAirZoneId, "cleanAirZoneId cannot be null");

    try {
      log.info("Get all cleanAirZones name: start");
      Response<CleanAirZonesResponse> cleanAirZonesResponse = vccsRepository
          .findCleanAirZonesSync();
      CleanAirZonesResponse response = cleanAirZonesResponse.body();
      CleanAirZoneDto cleanAirZone = response.findCleanAirZone(cleanAirZoneId);
      return cleanAirZone.getName();
    } finally {
      log.info("Get all cleanAirZones name: finish");
    }
  }
}
