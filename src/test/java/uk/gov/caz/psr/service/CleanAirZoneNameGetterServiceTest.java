package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.psr.dto.CleanAirZonesResponse;
import uk.gov.caz.psr.dto.CleanAirZonesResponse.CleanAirZoneDto;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;


@ExtendWith(MockitoExtension.class)
public class CleanAirZoneNameGetterServiceTest {

  private static final UUID ANY_VALID_CAZ_ID = UUID.randomUUID();
  private static final String ANY_VALID_CAZ_NAME = "CAZ_NAME";

  @InjectMocks
  private CleanAirZoneNameGetterService cleanAirZoneNameGetterService;

  @Mock
  private VccsRepository vccsRepository;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedCleanAirZoneIdIsNull() {
    // given
    UUID cleanAirZoneId = null;

    // when
    Throwable throwable =
        catchThrowable(
            () -> cleanAirZoneNameGetterService.fetch(cleanAirZoneId));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("cleanAirZoneId cannot be null");
  }

  @Test
  public void shouldThrowExceptionWhenCleanAirZoneNotFound() {
    // given
    UUID cleanAirZoneId = UUID.randomUUID();
    mockRepositoryResultForCleanAirZones();

    // when
    Throwable throwable =
        catchThrowable(
            () -> cleanAirZoneNameGetterService.fetch(cleanAirZoneId));

    // then
    assertThat(throwable).isInstanceOf(CleanAirZoneNotFoundException.class);
  }

  @Test
  public void shouldReturnNameWhenCleanAirZoneFound() {
    // given
    mockRepositoryResultForCleanAirZones();

    // when
    String result = cleanAirZoneNameGetterService.fetch(ANY_VALID_CAZ_ID);

    // then
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(ANY_VALID_CAZ_NAME);
  }

  private void mockRepositoryResultForCleanAirZones() {
    given(vccsRepository.findCleanAirZonesSync()).willReturn(cleanAirZonesResponse());
  }

  private Response<CleanAirZonesResponse> cleanAirZonesResponse() {
    return Response.success(CleanAirZonesResponse.builder()
        .cleanAirZones(Collections.singletonList(
            CleanAirZoneDto.builder()
                .cleanAirZoneId(ANY_VALID_CAZ_ID)
                .name(ANY_VALID_CAZ_NAME)
                .build()))
        .build());
  }
}
