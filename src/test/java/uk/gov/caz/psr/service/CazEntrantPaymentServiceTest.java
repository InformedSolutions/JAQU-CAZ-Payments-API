package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.psr.dto.CazEntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.CazEntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.CazEntrantPaymentRepository;

@ExtendWith(MockitoExtension.class)
class CazEntrantPaymentServiceTest {

  @Mock
  private CazEntrantPaymentRepository cazEntrantPaymentRepository;

  @InjectMocks
  private CazEntrantPaymentService cazEntrantPaymentService;

  private final static String ANY_VRN = "CAS123";
  private final static String ANY_UUID = "6bea485b-7fa6-4b78-9703-b721c98f4b15";
  private final static String ANY_TIMESTAMP = "2020-01-13T12:21:38.234";

  @Test
  public void shouldReturnEmptyCollectionWhenProvideEmptyList() {
    // given
    List<VehicleEntrantDto> cazEntrantPaymentDtos = new ArrayList<>();

    // when
    List<CazEntrantPaymentDto> response = cazEntrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldCallRepositoryAndReturnCollectionWhenRecordsExistAndAreCaptured() {
    // given
    VehicleEntrantDto dto = buildVehicleEntrantDto();
    List<VehicleEntrantDto> cazEntrantPaymentDtos = Arrays.asList(dto);
    mockNonEmptyCapturedCazEntryPaymentRepositoryResponse();

    // when
    List<CazEntrantPaymentDto> response = cazEntrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isNotEmpty();
    verify(cazEntrantPaymentRepository).findOneByVrnAndCazEntryDate(
        UUID.fromString(ANY_UUID),
        ANY_VRN,
        LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
    );
    verify(cazEntrantPaymentRepository, never()).update(buildCazEntrantPayment(true));
  }

  @Test
  public void shouldCallRepositoryAndUpdateRecordsAndReturnCollectionWhenRecordsExistAndAreNotCaptured() {
    // given
    VehicleEntrantDto dto = buildVehicleEntrantDto();
    List<VehicleEntrantDto> cazEntrantPaymentDtos = Arrays.asList(dto);
    mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse();

    // when
    List<CazEntrantPaymentDto> response = cazEntrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isNotEmpty();
    verify(cazEntrantPaymentRepository).findOneByVrnAndCazEntryDate(
        UUID.fromString(ANY_UUID),
        ANY_VRN,
        LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
    );
    verify(cazEntrantPaymentRepository).update(buildCazEntrantPayment(true));
  }

  private VehicleEntrantDto buildVehicleEntrantDto() {
    return VehicleEntrantDto
        .builder()
        .vrn(ANY_VRN)
        .cazEntryTimestamp(LocalDateTime.parse(ANY_TIMESTAMP))
        .cleanZoneId(UUID.fromString(ANY_UUID))
        .build();
  }

  private void mockNonEmptyCapturedCazEntryPaymentRepositoryResponse() {
    when(cazEntrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(true)));
  }

  private void mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse() {
    when(cazEntrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(false)));
  }

  private CazEntrantPayment buildCazEntrantPayment(boolean vehicleEntrantCaptured) {
    return CazEntrantPayment.builder()
        .cleanAirZoneId(UUID.fromString(ANY_UUID))
        .internalPaymentStatus(InternalPaymentStatus.PAID)
        .vrn(ANY_VRN)
        .vehicleEntrantCaptured(vehicleEntrantCaptured)
        .tariffCode("any-tariff-code")
        .charge(50)
        .travelDate(LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate())
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .build();
  }
}