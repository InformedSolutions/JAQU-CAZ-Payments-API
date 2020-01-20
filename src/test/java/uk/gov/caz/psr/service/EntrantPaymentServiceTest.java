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
import uk.gov.caz.psr.dto.EntrantPaymentDto;
import uk.gov.caz.psr.dto.VehicleEntrantDto;
import uk.gov.caz.psr.model.EntrantPayment;
import uk.gov.caz.psr.model.EntrantPaymentUpdateActor;
import uk.gov.caz.psr.model.InternalPaymentStatus;
import uk.gov.caz.psr.repository.EntrantPaymentRepository;

@ExtendWith(MockitoExtension.class)
class EntrantPaymentServiceTest {

  @Mock
  private EntrantPaymentRepository entrantPaymentRepository;

  @InjectMocks
  private EntrantPaymentService entrantPaymentService;

  private final static String ANY_VRN = "CAS123";
  private final static String ANY_UUID = "6bea485b-7fa6-4b78-9703-b721c98f4b15";
  private final static String ANY_TIMESTAMP = "2020-01-13T12:21:38.234";

  @Test
  public void shouldReturnEmptyCollectionWhenProvideEmptyList() {
    // given
    List<VehicleEntrantDto> cazEntrantPaymentDtos = new ArrayList<>();

    // when
    List<EntrantPaymentDto> response = entrantPaymentService
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
    List<EntrantPaymentDto> response = entrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isNotEmpty();
    verify(entrantPaymentRepository).findOneByVrnAndCazEntryDate(
        UUID.fromString(ANY_UUID),
        ANY_VRN,
        LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
    );
    verify(entrantPaymentRepository, never()).update(buildCazEntrantPayment(true));
  }

  @Test
  public void shouldCallRepositoryAndUpdateRecordsAndReturnCollectionWhenRecordsExistAndAreNotCaptured() {
    // given
    VehicleEntrantDto dto = buildVehicleEntrantDto();
    List<VehicleEntrantDto> cazEntrantPaymentDtos = Arrays.asList(dto);
    mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse();

    // when
    List<EntrantPaymentDto> response = entrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isNotEmpty();
    verify(entrantPaymentRepository).findOneByVrnAndCazEntryDate(
        UUID.fromString(ANY_UUID),
        ANY_VRN,
        LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate()
    );
    verify(entrantPaymentRepository).update(buildCazEntrantPayment(true));
  }

  @Test
  public void shouldCallRepositoryAndReturnCollectionWhenRecordDoesNotExist() {
    // given
    VehicleEntrantDto dto = buildVehicleEntrantDto();
    List<VehicleEntrantDto> cazEntrantPaymentDtos = Arrays.asList(dto);
    mockEmptyCazEntryPaymentRepositoryResponse();

    // when
    List<EntrantPaymentDto> response = entrantPaymentService
        .bulkProcess(cazEntrantPaymentDtos);

    // then
    assertThat(response).isNotEmpty();
    verify(entrantPaymentRepository).insert(buildCazEntrantPaymentToInsert());
  }

  private void mockEmptyCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(entrantPaymentRepository.insert(buildCazEntrantPaymentToInsert())).thenReturn(
        buildCazEntrantPaymentToInsert().toBuilder()
            .cleanAirZoneEntrantPaymentId(UUID.fromString(ANY_UUID))
            .build()
    );
  }

  private void mockNonEmptyCapturedCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(true)));
  }

  private void mockNonEmptyNotCapturedCazEntryPaymentRepositoryResponse() {
    when(entrantPaymentRepository.findOneByVrnAndCazEntryDate(any(), any(), any()))
        .thenReturn(Optional.of(buildCazEntrantPayment(false)));
  }

  private VehicleEntrantDto buildVehicleEntrantDto() {
    return VehicleEntrantDto
        .builder()
        .vrn(ANY_VRN)
        .cazEntryTimestamp(LocalDateTime.parse(ANY_TIMESTAMP))
        .cleanZoneId(UUID.fromString(ANY_UUID))
        .build();
  }

  private EntrantPayment buildCazEntrantPayment(boolean vehicleEntrantCaptured) {
    return EntrantPayment.builder()
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

  private EntrantPayment buildCazEntrantPaymentToInsert() {
    return EntrantPayment.builder()
        .cleanAirZoneId(UUID.fromString(ANY_UUID))
        .internalPaymentStatus(InternalPaymentStatus.NOT_PAID)
        .vrn(ANY_VRN)
        .vehicleEntrantCaptured(true)
        .travelDate(LocalDateTime.parse(ANY_TIMESTAMP).toLocalDate())
        .updateActor(EntrantPaymentUpdateActor.VCCS_API)
        .build();
  }
}