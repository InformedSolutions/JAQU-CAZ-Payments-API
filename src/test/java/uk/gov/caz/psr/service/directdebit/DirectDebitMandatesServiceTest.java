package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gocardless.GoCardlessClient;
import com.gocardless.resources.RedirectFlow;
import com.gocardless.resources.RedirectFlow.Links;
import com.gocardless.services.RedirectFlowService;
import com.gocardless.services.RedirectFlowService.RedirectFlowCompleteRequest;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate.DirectDebitMandateStatus;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.external.Link;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateLinks;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateStatus;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandatesServiceTest {

  @Mock
  private ExternalDirectDebitRepository externalDirectDebitRepository;

  @Mock
  private VccsRepository vccsRepository;

  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private AbstractGoCardlessClientFactory goCardlessClientFactory;

  @Mock
  private GoCardlessClient goCardlessClient;

  @InjectMocks
  private DirectDebitMandatesService directDebitMandatesService;

  private final static String VALID_MANDATE_ID = "mandateID";
  private final static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private final static UUID ANY_CLEAN_AIR_ZONE_ID = UUID.randomUUID();
  private final static String ANY_RETURN_URL = "http://return-url.com";
  private final static String NEXT_URL = "http://some-address.com";

  @Nested
  class CreateDirectDebitMandate {

    @Test
    public void shouldReturnNextUrlIfValidParamsProvided() {
      // given
      mockValidCreateDirectDebitMandateInExternalProvider();
      mockSuccessCreateDirectDebitMandateInAccountService();

      // when
      String response = directDebitMandatesService
          .createDirectDebitMandate(ANY_CLEAN_AIR_ZONE_ID, ANY_ACCOUNT_ID, ANY_RETURN_URL);

      // then
      assertThat(response).isEqualTo(NEXT_URL);
    }

    @Nested
    class UponExceptionFromAccountsServiceCall {

      @Test
      public void shouldThrowExternalServiceCallException() {
        // given
        mockValidCreateDirectDebitMandateInExternalProvider();
        mockFailureOfCreationOFDirectDebitMandateInAccountService();

        // when
        Throwable throwable = catchThrowable(() -> directDebitMandatesService
            .createDirectDebitMandate(ANY_CLEAN_AIR_ZONE_ID, ANY_ACCOUNT_ID, ANY_RETURN_URL));

        // then
        assertThat(throwable).isInstanceOf(ExternalServiceCallException.class)
            .hasMessageStartingWith("Accounts service call failed");
      }
    }

    private void mockValidCreateDirectDebitMandateInExternalProvider() {
      when(externalDirectDebitRepository.createMandate(any(), any(), any()))
          .thenReturn(MandateResponse.builder()
              .mandateId(VALID_MANDATE_ID)
              .returnUrl(ANY_RETURN_URL)
              .links(MandateLinks.builder()
                  .nextUrl(new Link(NEXT_URL, "GET"))
                  .build())
              .build());
    }

    private void mockFailureOfCreationOFDirectDebitMandateInAccountService() {
      given(accountsRepository.createDirectDebitMandateSync(any(), any()))
          .willReturn(
              Response.error(400, ResponseBody.create(MediaType.get("application/json"), "")));
    }

    private void mockSuccessCreateDirectDebitMandateInAccountService() {
      given(accountsRepository.createDirectDebitMandateSync(any(), any()))
          .willReturn(accountsCreateDirectDebitMandateResponse());
    }

    private Response<CreateDirectDebitMandateResponse> accountsCreateDirectDebitMandateResponse() {
      return Response
          .success(CreateDirectDebitMandateResponse.builder()
              .cleanAirZoneId(UUID.randomUUID())
              .build());
    }
  }

  @Nested
  class CompleteDirectDebitMandateCreation {

    @Nested
    class WhenCallToCreateMandateInAccountsFails {

      @Test
      public void shouldThrowExternalServiceCallException() {
        // given
        String flowId = "my-flow";
        String sessionToken = "my-session-token";
        mockAbsenceOfAccountIdMetadata();

        // when
        Throwable throwable = catchThrowable(() -> directDebitMandatesService
            .completeMandateCreation(ANY_CLEAN_AIR_ZONE_ID, flowId, sessionToken));

        // then
        assertThat(throwable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("'accountId' is absent in the metadata! Please set it when the redirect "
                + "flow is initiated");
      }

      private void mockAbsenceOfAccountIdMetadata() {
        when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
            .thenReturn(goCardlessClient);
        RedirectFlowService redirectFlowService = Mockito.mock(RedirectFlowService.class);
        RedirectFlowCompleteRequest completeRequest = Mockito.mock(RedirectFlowCompleteRequest.class);
        RedirectFlow response = Mockito.mock(RedirectFlow.class);
        Links links = mock(Links.class);

        when(goCardlessClient.redirectFlows()).thenReturn(redirectFlowService);
        when(redirectFlowService.complete(anyString())).thenReturn(completeRequest);
        when(completeRequest.withSessionToken(anyString())).thenReturn(completeRequest);
        when(completeRequest.execute()).thenReturn(response);
        when(response.getMetadata()).thenReturn(Collections.emptyMap());
        when(response.getLinks()).thenReturn(links);
        when(links.getMandate()).thenReturn("any-mandate-id");
      }
    }
  }

  @Nested
  class GetCleanAirZonesAlongsideDirectDebitMandates {

    @Test
    public void shouldReturnMandatesWithBothCachedAndNotCachedStatuses() {
      // given
      mockCleanAirZonesInVccs();
      mockDirectDebitMandatesInAccountsWithStatuses(
          cacheableStatuses().iterator().next(),
          notCacheableStatuses().iterator().next()
      );
      mockExternalDirectDebitRepositoryWithStatus(DirectDebitMandateStatus.ACTIVE.toString());
      mockAccountsMandateUpdateCall();

      // when
      List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
          .getDirectDebitMandates(ANY_ACCOUNT_ID);

      // then
      assertThat(directDebitMandates).hasSize(1);
      assertThat(directDebitMandates.iterator().next().getMandates()).hasSize(2);
      verify(externalDirectDebitRepository, times(1)).getMandate(any(), any());
    }

    @Nested
    class WhenStatusIsApplicableToBeReturnedCached {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.psr.service.directdebit.DirectDebitMandatesServiceTest#cacheableStatuses")
      public void shouldNotCallExternalRepository(DirectDebitMandateStatus status) {
        // given
        mockCleanAirZonesInVccs();
        mockDirectDebitMandatesInAccountsWithStatuses(status);

        // when
        List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
            .getDirectDebitMandates(ANY_ACCOUNT_ID);

        // then
        assertThat(directDebitMandates).isNotEmpty();
        verifyNoInteractions(externalDirectDebitRepository);
        verify(accountsRepository, never())
            .updateDirectDebitMandatesSync(any(), any());
      }
    }

    @Nested
    class WhenStatusIsApplicableToBeFetched {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.psr.service.directdebit.DirectDebitMandatesServiceTest#notCacheableStatuses")
      public void shouldNotCallExternalRepository(DirectDebitMandateStatus status) {
        // given
        mockCleanAirZonesInVccs();
        mockDirectDebitMandatesInAccountsWithStatuses(status);
        mockExternalDirectDebitRepositoryWithStatus(randomNotCacheableStatusNotEqualTo(status));
        mockAccountsMandateUpdateCall();

        // when
        List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
            .getDirectDebitMandates(ANY_ACCOUNT_ID);

        // then
        assertThat(directDebitMandates).hasSize(1);
        verify(externalDirectDebitRepository).getMandate(any(), any());
        verify(accountsRepository).updateDirectDebitMandatesSync(eq(ANY_ACCOUNT_ID), any());
      }

      @Nested
      class WhenExternalStatusIsSame {

        @ParameterizedTest
        @MethodSource("uk.gov.caz.psr.service.directdebit.DirectDebitMandatesServiceTest#notCacheableStatuses")
        public void shouldNotUpdateMandatesInAccounts(DirectDebitMandateStatus status) {
          // given
          mockCleanAirZonesInVccs();
          mockDirectDebitMandatesInAccountsWithStatuses(status);
          mockExternalDirectDebitRepositoryWithStatus(status.toString());

          // when
          directDebitMandatesService.getDirectDebitMandates(ANY_ACCOUNT_ID);

          // then
          verify(accountsRepository, never())
              .updateDirectDebitMandatesSync(eq(ANY_ACCOUNT_ID), any());
        }
      }
    }

    private void mockAccountsMandateUpdateCall() {
      when(accountsRepository.updateDirectDebitMandatesSync(eq(ANY_ACCOUNT_ID), any()))
          .thenReturn(Response.success(null));
    }

    private String randomNotCacheableStatusNotEqualTo(DirectDebitMandateStatus status) {
      return StreamSupport.stream(notCacheableStatuses().spliterator(), false)
          .filter(s -> s != status)
          .map(Enum::name)
          .findFirst()
          .orElseThrow(IllegalStateException::new);
    }

    private void mockExternalDirectDebitRepositoryWithStatus(String status) {
      when(externalDirectDebitRepository.getMandate(anyString(), any())).thenReturn(
          MandateResponse.builder()
              .mandateId("some-id-1")
              .returnUrl("return-url")
              .providerId("some-id-2")
              .state(MandateStatus.builder()
                  .status(status)
                  .build())
              .build()
      );
    }

    private void mockDirectDebitMandatesInAccountsWithStatuses(
        DirectDebitMandateStatus... statuses) {
      List<DirectDebitMandate> mandates = Stream.of(statuses)
          .map(status -> DirectDebitMandate.builder()
              .accountId(ANY_ACCOUNT_ID)
              .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
              .directDebitMandateId(UUID.randomUUID())
              .status(status)
              .paymentProviderMandateId(UUID.randomUUID().toString())
              .build())
          .collect(Collectors.toList());
      when(accountsRepository.getAccountDirectDebitMandatesSync(ANY_ACCOUNT_ID))
          .thenReturn(Response.success(AccountDirectDebitMandatesResponse.builder()
              .directDebitMandates(mandates)
              .build()));
    }

    @SneakyThrows
    private void mockCleanAirZonesInVccs() {
      when(vccsRepository.findCleanAirZonesSync()).thenReturn(
          Response.success(CleanAirZonesDto.builder()
              .cleanAirZones(Collections.singletonList(CleanAirZoneDto.builder()
                  .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
                  .name("clean air zone name")
                  .boundaryUrl(new URI("http://localhost"))
                  .build()))
              .build()));
    }
  }

  public static Iterable<DirectDebitMandateStatus> cacheableStatuses() {
    return DirectDebitMandatesService.CACHEABLE_STATUSES;
  }

  public static Iterable<DirectDebitMandateStatus> notCacheableStatuses() {
    return EnumSet.complementOf(DirectDebitMandatesService.CACHEABLE_STATUSES);
  }
}