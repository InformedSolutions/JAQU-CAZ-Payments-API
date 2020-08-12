package uk.gov.caz.psr.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.psr.util.TestObjectFactory.Payments.preparePayment;
import static uk.gov.caz.psr.util.TestObjectFactory.Payments.preparePaymentWithTwoEntrantPayments;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse;
import uk.gov.caz.psr.model.ExternalPaymentStatus;
import uk.gov.caz.psr.repository.VccsRepository;

@ExtendWith(MockitoExtension.class)
class ReferencesHistoryConverterTest {

  @Mock
  private CurrencyFormatter currencyFormatter;

  @Mock
  private VccsRepository vccsRepository;

  @InjectMocks
  private ReferencesHistoryConverter converter;

  @Test
  public void shouldConvertPaymentToPaymentDetailsResponse() {
    // given
    given(currencyFormatter.parsePenniesToBigDecimal(anyInt()))
        .willAnswer(answer -> BigDecimal.valueOf(40));
    given(vccsRepository.findCleanAirZonesSync()).willReturn(sampleCleanAirZonesResponse());

    // when
    ReferencesHistoryResponse referencesHistoryResponse = converter
        .toReferencesHistoryResponse(preparePayment(UUID.randomUUID()));

    // then
    assertThat(referencesHistoryResponse).isNotNull();
    assertThat(referencesHistoryResponse.getPaymentReference()).isEqualTo(1500L);
    assertThat(referencesHistoryResponse.getPaymentProviderId()).isEqualTo("123");
    assertThat(referencesHistoryResponse.isTelephonePayment()).isFalse();
    assertThat(referencesHistoryResponse.getTotalPaid()).isEqualTo(BigDecimal.valueOf(40));
    assertThat(referencesHistoryResponse.getOperatorId()).isNotNull();
    assertThat(referencesHistoryResponse.getPaymentProviderStatus())
        .isEqualTo(ExternalPaymentStatus.CREATED);
    assertThat(referencesHistoryResponse.getLineItems()).hasSize(1);
    assertThat(referencesHistoryResponse.getLineItems().get(0).getChargePaid())
        .isEqualTo(BigDecimal.valueOf(40));
    assertThat(referencesHistoryResponse.getLineItems().get(0).getVrn())
        .isEqualTo("CAS310");
  }

  @Test
  public void shouldThrowExceptionWhenThereIsNotUniqueCazInEntrantPayments() {
    // when
    Throwable result = catchThrowable(() -> converter
        .toReferencesHistoryResponse(preparePaymentWithTwoEntrantPayments(UUID.randomUUID())));

    // then
    assertThat(result)
        .hasMessage("There is more than one CAZ in entrant payments. CAZ should be unique for the same payment.");
  }

  private Response<CleanAirZonesDto> sampleCleanAirZonesResponse() {
    List<CleanAirZoneDto> cazDtosList = sampleCleanAirZoneDtosList();
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(cazDtosList)
        .build();
    return Response.success(cleanAirZonesDto);
  }

  private List<CleanAirZoneDto> sampleCleanAirZoneDtosList() {
    return Arrays.asList(
        CleanAirZoneDto.builder()
            .cleanAirZoneId(UUID.fromString("f64fdc1b-70f6-4c87-bfce-3643b2e4c714"))
            .name("Birmingham")
            .build(),
        CleanAirZoneDto.builder()
            .cleanAirZoneId(UUID.fromString("5e554ef5-8513-4d98-8cd5-625dc6a77e80"))
            .name("Leeds")
            .build()
    );
  }
}