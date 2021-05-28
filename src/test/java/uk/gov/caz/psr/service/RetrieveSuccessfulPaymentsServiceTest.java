package uk.gov.caz.psr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.accounts.AccountUserResponse;
import uk.gov.caz.psr.dto.accounts.AccountUsersResponse;
import uk.gov.caz.psr.model.EnrichedPaymentSummary;
import uk.gov.caz.psr.model.PaginationData;
import uk.gov.caz.psr.model.PaymentSummary;
import uk.gov.caz.psr.model.PaymentToCleanAirZoneMapping;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.PaymentSummaryRepository;
import uk.gov.caz.psr.repository.PaymentToCleanAirZoneMappingRepository;
import uk.gov.caz.psr.repository.audit.PaymentDetailRepository;
import uk.gov.caz.psr.util.CurrencyFormatter;

@ExtendWith(MockitoExtension.class)
class RetrieveSuccessfulPaymentsServiceTest {

  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private VehicleComplianceRetrievalService vehicleComplianceRetrievalService;

  @Mock
  private PaymentToCleanAirZoneMappingRepository paymentToCleanAirZoneMappingRepository;

  @Mock
  private PaymentSummaryRepository paymentSummaryRepository;

  @Mock
  private PaymentDetailRepository paymentDetailRepository;

  @Mock
  private CurrencyFormatter currencyFormatter;

  @InjectMocks
  private RetrieveSuccessfulPaymentsService retrieveSuccessfulPaymentsService;

  private final UUID ANY_ACCOUNT_ID = UUID.fromString("cce6da7e-c69f-11ea-bb00-5f164033a9da");

  private final UUID OWNER_USER_ID = UUID.fromString("ab3e9f4b-4076-4154-b6dd-97c5d4800b47");
  private final UUID ANY_USER_ID = UUID.fromString("88732cca-a5c7-4ad6-a60d-7edede935915");
  private final UUID REMOVED_USER_ID = UUID.fromString("3f319922-71d2-432c-9757-8e5f060c2447");

  private final int ANY_PAGE_NUMBER = 0;
  private final int ANY_PAGE_SIZE = 10;

  private final int ANY_PAYMENTS_COUNT_RESULT = 2;

  @Nested
  class RetrieveForSingleUser {

    @Test
    public void shouldIntegrateDataFromVariousSources() {
      prepareAllMocks();

      retrieveSuccessfulPaymentsService
          .retrieveForAccount(ANY_ACCOUNT_ID, ANY_PAGE_NUMBER, ANY_PAGE_SIZE);

      verify(accountsRepository).getAllUsersSync(any());
      verify(paymentToCleanAirZoneMappingRepository).getPaymentToCleanAirZoneMapping(any());
      verify(paymentSummaryRepository)
          .getPaginatedPaymentSummaryForUserIds(any(), anyInt(), anyInt());
      verify(vehicleComplianceRetrievalService).getCleanAirZoneIdToCleanAirZoneNameMap();
      verify(paymentSummaryRepository).getTotalPaymentsCountForUserIds(any());
      verify(paymentDetailRepository).getPaymentStatusesForPaymentIds(anySet(), any(), anyList());
    }

    @Test
    public void shouldReturnEnrichedPaymentSummariesAndPaginationDataForSingleUser() {
      prepareAllMocks();

      Pair<PaginationData, List<EnrichedPaymentSummary>> result = retrieveSuccessfulPaymentsService
          .retrieveForSingleUser(ANY_ACCOUNT_ID, OWNER_USER_ID, ANY_PAGE_NUMBER, ANY_PAGE_SIZE);

      PaginationData paginationData = result.getFirst();
      List<EnrichedPaymentSummary> enrichedPaymentSummaries = result.getSecond();

      assertThat(paginationData).isNotNull();
      assertThat(enrichedPaymentSummaries).isNotEmpty();
    }
  }

  @Nested
  class RetrieveForAccount {

    @Test
    public void shouldIntegrateDataFromVariousSources() {
      prepareAllMocks();

      retrieveSuccessfulPaymentsService
          .retrieveForAccount(ANY_ACCOUNT_ID, ANY_PAGE_NUMBER, ANY_PAGE_SIZE);

      verify(accountsRepository).getAllUsersSync(any());
      verify(paymentToCleanAirZoneMappingRepository).getPaymentToCleanAirZoneMapping(any());
      verify(paymentSummaryRepository)
          .getPaginatedPaymentSummaryForUserIds(any(), anyInt(), anyInt());
      verify(vehicleComplianceRetrievalService).getCleanAirZoneIdToCleanAirZoneNameMap();
      verify(paymentSummaryRepository).getTotalPaymentsCountForUserIds(any());
      verify(paymentDetailRepository).getPaymentStatusesForPaymentIds(anySet(), any(), anyList());
    }

    @Test
    public void shouldReturnEnrichedPaymentSummariesAndPaginationDataForAllUsers() {
      prepareAllMocks();

      Pair<PaginationData, List<EnrichedPaymentSummary>> result = retrieveSuccessfulPaymentsService
          .retrieveForAccount(ANY_ACCOUNT_ID, ANY_PAGE_NUMBER, ANY_PAGE_SIZE);

      PaginationData paginationData = result.getFirst();
      List<EnrichedPaymentSummary> enrichedPaymentSummaries = result.getSecond();

      assertThat(paginationData).isNotNull();
      assertThat(enrichedPaymentSummaries.size()).isEqualTo(2);
    }
  }

  private void prepareAllMocks() {
    when(accountsRepository.getAllUsersSync(ANY_ACCOUNT_ID)).thenReturn(sampleUsersResponse());
    when(paymentToCleanAirZoneMappingRepository.getPaymentToCleanAirZoneMapping(any()))
        .thenReturn(samplePaymentToCazMappingResult());
    when(paymentSummaryRepository.getPaginatedPaymentSummaryForUserIds(any(), anyInt(), anyInt()))
        .thenReturn(samplePaymentSummaryResult());
    when(vehicleComplianceRetrievalService.getCleanAirZoneIdToCleanAirZoneNameMap())
        .thenReturn(sampleCleanAirZonesMap());
    when(paymentSummaryRepository.getTotalPaymentsCountForUserIds(any()))
        .thenReturn(ANY_PAYMENTS_COUNT_RESULT);
    when(currencyFormatter.parsePenniesToBigDecimal(anyInt())).thenReturn(BigDecimal.valueOf(50));
  }

  private List<PaymentToCleanAirZoneMapping> samplePaymentToCazMappingResult() {
    return Arrays.asList(
        PaymentToCleanAirZoneMapping.builder()
            .paymentId(UUID.fromString("eae1d669-297e-41d0-b3b7-290d0300ca6d"))
            .cleanAirZoneId(UUID.fromString("f64fdc1b-70f6-4c87-bfce-3643b2e4c714"))
            .build(),
        PaymentToCleanAirZoneMapping.builder()
            .paymentId(UUID.fromString("749f8a00-257b-4d06-9589-b1ba7bbc934e"))
            .cleanAirZoneId(UUID.fromString("5e554ef5-8513-4d98-8cd5-625dc6a77e80"))
            .build()
    );
  }

  private List<PaymentSummary> samplePaymentSummaryResult() {
    return Arrays.asList(
        PaymentSummary.builder()
            .paymentId(UUID.fromString("eae1d669-297e-41d0-b3b7-290d0300ca6d"))
            .entriesCount(10)
            .payerId(OWNER_USER_ID)
            .totalPaid(10000)
            .paymentDate(LocalDate.now())
            .build(),
        PaymentSummary.builder()
            .paymentId(UUID.fromString("749f8a00-257b-4d06-9589-b1ba7bbc934e"))
            .entriesCount(5)
            .payerId(ANY_USER_ID)
            .totalPaid(5000)
            .paymentDate(LocalDate.now())
            .build()
    );
  }

  private Response<CleanAirZonesDto> sampleCleanAirZonesResponse() {
    List<CleanAirZoneDto> cazDtosList = sampleCleanAirZoneDtosList();
    CleanAirZonesDto cleanAirZonesDto = CleanAirZonesDto.builder()
        .cleanAirZones(cazDtosList)
        .build();
    return Response.success(cleanAirZonesDto);
  }

  private Map<UUID, String> sampleCleanAirZonesMap() {
    List<CleanAirZoneDto> cazDtosList = sampleCleanAirZoneDtosList();
    Map<UUID, String> cazMap = cazDtosList.stream()
        .collect(Collectors.toMap(CleanAirZoneDto::getCleanAirZoneId,
            CleanAirZoneDto::getName));
    return cazMap;
  }

  private List<CleanAirZoneDto> sampleCleanAirZoneDtosList() {
    return Arrays.asList(
        CleanAirZoneDto.builder()
            .cleanAirZoneId(UUID.fromString("f64fdc1b-70f6-4c87-bfce-3643b2e4c714"))
            .name("Birmingham")
            .build(),
        CleanAirZoneDto.builder()
            .cleanAirZoneId(UUID.fromString("5e554ef5-8513-4d98-8cd5-625dc6a77e80"))
            .name("Bath")
            .build()
    );
  }

  private Response<AccountUsersResponse> sampleUsersResponse() {
    List<AccountUserResponse> userResponseList = sampleAccountUserResponseList();
    AccountUsersResponse usersResponse = AccountUsersResponse.builder()
        .users(userResponseList)
        .build();

    return Response.success(usersResponse);
  }

  private List<AccountUserResponse> sampleAccountUserResponseList() {
    return Arrays.asList(
        AccountUserResponse.builder()
            .accountUserId(OWNER_USER_ID)
            .owner(true)
            .removed(false)
            .build(),
        AccountUserResponse.builder()
            .accountUserId(ANY_USER_ID)
            .owner(false)
            .removed(false)
            .name("Jan Kowalski")
            .build(),
        AccountUserResponse.builder()
            .accountUserId(REMOVED_USER_ID)
            .owner(false)
            .removed(true)
            .name("")
            .build()
    );
  }
}
