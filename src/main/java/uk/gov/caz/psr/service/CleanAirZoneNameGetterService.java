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
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;

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
   * @throws CleanAirZoneNotFoundException if cleanAirZone was not found in VCCS
   */
  public String fetch(UUID cleanAirZoneId) {
    Preconditions.checkNotNull(cleanAirZoneId, "cleanAirZoneId cannot be null");

    try {
      log.debug("Get all cleanAirZones name: start");
      Response<CleanAirZonesResponse> cleanAirZonesResponse = vccsRepository
          .findCleanAirZonesSync();
      CleanAirZonesResponse response = cleanAirZonesResponse.body();
      return findCleanAirZoneName(response, cleanAirZoneId);
    } finally {
      log.debug("Get all cleanAirZones name: finish");
    }
  }

  /**
   * Method which finds name of CleanAirZone with provided ID from VCCS response.
   *
   * @param response Response from VCCS
   * @param cleanAirZoneId id of Clean Air Zone which we want to find
   * @return Found CleanAirZone name.
   * @throws CleanAirZoneNotFoundException if Clean Air Zone was not found.
   */
  public String findCleanAirZoneName(CleanAirZonesResponse response, UUID cleanAirZoneId) {
    CleanAirZoneDto cleanAirZone = response.getCleanAirZones().stream()
        .filter(fetch -> fetch.getCleanAirZoneId().equals(cleanAirZoneId))
        .findFirst()
        .orElseThrow(() -> new CleanAirZoneNotFoundException(cleanAirZoneId));
    return cleanAirZone.getName();
  }
}
