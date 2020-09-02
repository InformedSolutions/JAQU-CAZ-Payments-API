package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.psr.dto.AccountVehicleRetrievalResponse;
import uk.gov.caz.psr.dto.CachedCharge;
import uk.gov.caz.psr.dto.VehicleDetails;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;

@ExtendWith(MockitoExtension.class)
class AccountVehicleRetrievalConverterTest {

  private static final UUID ANY_CAZ_ID = UUID.randomUUID();

  @InjectMocks
  private AccountVehicleRetrievalConverter accountVehicleRetrievalConverter;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedValueIsNull() {
    // given
    AccountVehicleRetrievalResponse response = null;
    String pageNumber = "1";
    String perPage = "10";

    // when
    Throwable throwable = catchThrowable(
        () -> accountVehicleRetrievalConverter
            .toVehicleRetrievalResponseDto(response, pageNumber, perPage));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("accountVehicleRetrievalResponse cannot be null");
  }

  @Test
  public void shouldConvertPassedResponse() {
    // given
    AccountVehicleRetrievalResponse response = buildAccountVehicleRetrievalResponse();
    String pageNumber = "1";
    String perPage = "10";

    // when
    VehicleRetrievalResponseDto vehicleRetrievalResponseDto = accountVehicleRetrievalConverter
        .toVehicleRetrievalResponseDto(response, pageNumber, perPage);

    // then
    assertThat(vehicleRetrievalResponseDto).isNotNull();
    assertThat(vehicleRetrievalResponseDto.getVehicles()).hasSize(1);
    assertThat(vehicleRetrievalResponseDto.getPage()).isEqualTo(Integer.parseInt(pageNumber));
    assertThat(vehicleRetrievalResponseDto.getPerPage()).isEqualTo(Integer.parseInt(perPage));
    assertThat(vehicleRetrievalResponseDto.getPageCount()).isEqualTo(1);
    assertThat(vehicleRetrievalResponseDto.getTotalVrnsCount()).isEqualTo(2);

    ComplianceResultsDto singleResult = vehicleRetrievalResponseDto.getVehicles().iterator().next();
    assertThat(singleResult.getIsExempt()).isTrue();
    assertThat(singleResult.getIsRetrofitted()).isFalse();
    assertThat(singleResult.getRegistrationNumber()).isEqualTo("TEST_VRN");
    assertThat(singleResult.getVehicleType()).isEqualTo("Car");

    ComplianceOutcomeDto complianceOutcome = singleResult.getComplianceOutcomes().iterator().next();
    assertThat(complianceOutcome.getCharge()).isEqualTo(100);
    assertThat(complianceOutcome.getTariffCode()).isEqualTo("tariffCode");
    assertThat(complianceOutcome.getCleanAirZoneId()).isEqualTo(ANY_CAZ_ID);
  }

  private AccountVehicleRetrievalResponse buildAccountVehicleRetrievalResponse() {
    return AccountVehicleRetrievalResponse.builder()
        .pageCount(1)
        .totalVehiclesCount(2)
        .vehicles(Arrays.asList(
            VehicleDetails.builder()
                .isExempt(Boolean.TRUE)
                .isRetrofitted(Boolean.FALSE)
                .vehicleType("Car")
                .vrn("TEST_VRN")
                .cachedCharges(Arrays.asList(
                    CachedCharge.builder()
                        .charge(100)
                        .tariffCode("tariffCode")
                        .cazId(ANY_CAZ_ID)
                        .build()))
                .build()))
        .build();
  }
}