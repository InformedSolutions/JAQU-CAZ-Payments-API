package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CacheableResponseDto;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.repository.exception.CleanAirZoneNotFoundException;


@ExtendWith(MockitoExtension.class)
public class CleanAirZoneServiceTest {

  private static final UUID ANY_VALID_CAZ_ID = UUID.randomUUID();
  private static final String ANY_VALID_CAZ_NAME = "CAZ_NAME";
  private static final String ANY_VALID_OPERATOR_NAME = "OPERATOR_NAME";
  private static final URI ANY_URL = URI.create("www.test.uk");
  private static final String ANY_DIRECT_DEBIT_START_DATE_TEXT = "4 May 2021";

  @InjectMocks
  private CleanAirZoneService cleanAirZoneService;

  @Mock
  private VccsRepository vccsRepository;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedCleanAirZoneIdIsNull() {
    // given
    UUID cleanAirZoneId = null;

    // when
    Throwable throwable =
        catchThrowable(
            () -> cleanAirZoneService.fetch(cleanAirZoneId));

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
            () -> cleanAirZoneService.fetch(cleanAirZoneId));

    // then
    assertThat(throwable).isInstanceOf(CleanAirZoneNotFoundException.class);
  }

  @Test
  public void shouldReturnNameWhenCleanAirZoneFound() {
    // given
    mockRepositoryResultForCleanAirZones();

    // when
    String result = cleanAirZoneService.fetch(ANY_VALID_CAZ_ID);

    // then
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(ANY_VALID_CAZ_NAME);
  }

  @Test
  public void shouldReturnCleanAirZonesFromFetchAll() {
    // given
    mockRepositoryResultForCleanAirZones();

    // when
    CacheableResponseDto<CleanAirZonesDto> result = cleanAirZoneService.fetchAll();

    // then
    assertThat(result).isNotNull();
    assertThat(result.getBody().getCleanAirZones().get(0).getName()).isEqualTo(ANY_VALID_CAZ_NAME);
    assertThat(result.getBody().getCleanAirZones().get(0).getOperatorName())
        .isEqualTo(ANY_VALID_OPERATOR_NAME);
    assertThat(result.getBody().getCleanAirZones().get(0).getActiveChargeStartDateText())
        .isEqualTo("1 June 2021");
    assertThat(result.getBody().getCleanAirZones().get(0).getDisplayFrom()).isEqualTo("2021-01-01");
    assertThat(result.getBody().getCleanAirZones().get(0).getDisplayOrder()).isEqualTo(2);
    assertThat(result.getBody().getCleanAirZones().get(0).getPrivacyPolicyUrl()).isEqualTo(ANY_URL);
    assertThat(result.getBody().getCleanAirZones().get(0).getDirectDebitStartDateText())
        .isEqualTo(ANY_DIRECT_DEBIT_START_DATE_TEXT);
  }

  private void mockRepositoryResultForCleanAirZones() {
    given(vccsRepository.findCleanAirZonesSync()).willReturn(cleanAirZonesResponse());
  }

  private Response<CleanAirZonesDto> cleanAirZonesResponse() {
    return Response.success(CleanAirZonesDto.builder()
        .cleanAirZones(Collections.singletonList(
            CleanAirZoneDto.builder()
                .cleanAirZoneId(ANY_VALID_CAZ_ID)
                .name(ANY_VALID_CAZ_NAME)
                .operatorName(ANY_VALID_OPERATOR_NAME)
                .activeChargeStartDate("2018-10-18")
                .activeChargeStartDateText("1 June 2021")
                .displayFrom("2021-01-01")
                .displayOrder(2)
                .privacyPolicyUrl(ANY_URL)
                .directDebitStartDateText(ANY_DIRECT_DEBIT_START_DATE_TEXT)
                .build()))
        .build());
  }
}
