package uk.gov.caz.psr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Call;
import uk.gov.caz.psr.dto.whitelist.WhitelistedVehicleResponseDto;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

@ExtendWith(MockitoExtension.class)
class WhitelistRepositoryTest {

  @Mock
  private Call<WhitelistedVehicleResponseDto> response;

  private WhitelistRepository whitelistRepository = new WhitelistRepository() {
    @Override
    public Call<WhitelistedVehicleResponseDto> getWhitelistVehicleDetails(String vrn) {
      return response;
    }
  };

  @Test
  public void shouldThrowExternalServiceCallExceptionUponIOException() throws IOException {
    when(response.execute()).thenThrow(new IOException());

    Throwable throwable = catchThrowable(() ->
        whitelistRepository.getWhitelistVehicleDetailsSync("vrn"));

    assertThat(throwable).isInstanceOf(ExternalServiceCallException.class);
  }
}