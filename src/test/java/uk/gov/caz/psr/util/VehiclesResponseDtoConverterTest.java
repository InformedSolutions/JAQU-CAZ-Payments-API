package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges.VehicleCharge;
import uk.gov.caz.psr.dto.VehicleRetrievalResponseDto;

@ExtendWith(MockitoExtension.class)
class VehiclesResponseDtoConverterTest {

  private static final UUID ANY_CAZ_ID = UUID.randomUUID();

  @InjectMocks
  private VehiclesResponseDtoConverter vehiclesResponseDtoConverter;

  @Test
  public void shouldThrowNullPointerExceptionWhenPassedValueIsNull() {
    // given
    VehiclesResponseDto response = null;
    String pageNumber = "1";
    String perPage = "10";

    // when
    Throwable throwable = catchThrowable(
        () -> vehiclesResponseDtoConverter
            .toVehicleRetrievalResponseDto(response, pageNumber, perPage));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("vehiclesResponse cannot be null");
  }

  @Test
  public void shouldConvertPassedResponse() {
    // given
    VehiclesResponseDto response = buildVehiclesResponse();
    String pageNumber = "1";
    String perPage = "10";

    // when
    VehicleRetrievalResponseDto vehicleRetrievalResponseDto = vehiclesResponseDtoConverter
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

  private VehiclesResponseDto buildVehiclesResponse() {
    return VehiclesResponseDto.builder()
        .pageCount(1)
        .totalVehiclesCount(2)
        .vehicles(Arrays.asList(
            VehicleWithCharges.builder()
                .exempt(Boolean.TRUE)
                .retrofitted(Boolean.FALSE)
                .vehicleType("Car")
                .vrn("TEST_VRN")
                .cachedCharges(Arrays.asList(
                    VehicleCharge.builder()
                        .charge(BigDecimal.valueOf(100))
                        .tariffCode("tariffCode")
                        .cazId(ANY_CAZ_ID)
                        .build()))
                .build()))
        .build();
  }
}