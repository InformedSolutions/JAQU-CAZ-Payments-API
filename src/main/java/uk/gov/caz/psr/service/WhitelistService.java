package uk.gov.caz.psr.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CacheableResponseDto;
import uk.gov.caz.psr.dto.whitelist.WhitelistedVehicleResponseDto;
import uk.gov.caz.psr.repository.WhitelistRepository;

/**
 * Service that coordinates calls to the whitelist service by leveraging {@link
 * WhitelistRepository}.
 */
@Service
@AllArgsConstructor
public class WhitelistService {

  private final WhitelistRepository whitelistRepository;

  /**
   * Gets details of a whitelisted vehicle by its {@code vrn}.
   */
  public CacheableResponseDto<WhitelistedVehicleResponseDto> getWhitelistVehicle(String vrn) {
    Response<WhitelistedVehicleResponseDto> response = whitelistRepository
        .getWhitelistVehicleDetailsSync(vrn);
    return CacheableResponseDto.<WhitelistedVehicleResponseDto>builder()
        .code(response.code())
        .body(response.body())
        .build();
  }
}
