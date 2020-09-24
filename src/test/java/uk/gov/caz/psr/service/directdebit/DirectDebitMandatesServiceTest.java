package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gocardless.GoCardlessClient;
import com.gocardless.errors.GoCardlessApiException;
import com.gocardless.resources.Mandate;
import com.gocardless.resources.Mandate.Status;
import com.gocardless.resources.RedirectFlow;
import com.gocardless.resources.RedirectFlow.Links;
import com.gocardless.services.MandateService;
import com.gocardless.services.MandateService.MandateGetRequest;
import com.gocardless.services.RedirectFlowService;
import com.gocardless.services.RedirectFlowService.RedirectFlowCompleteRequest;
import com.gocardless.services.RedirectFlowService.RedirectFlowCreateRequest;
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
import uk.gov.caz.psr.controller.exception.directdebit.GoCardlessException;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate;
import uk.gov.caz.psr.dto.AccountDirectDebitMandatesResponse.DirectDebitMandate.DirectDebitMandateStatus;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.VccsRepository;
import uk.gov.caz.psr.service.exception.ExternalServiceCallException;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandatesServiceTest {

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

  private final static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private final static UUID ANY_CLEAN_AIR_ZONE_ID = UUID.randomUUID();
  private final static String ANY_RETURN_URL = "http://return-url.com";
  private final static String NEXT_URL = "http://some-address.com";
  private static final String ANY_SESSION_ID = "SESS_wSs0uGYMISxzqOBq";

  @Nested
  class CreateDirectDebitMandateRedirectFlow {

    @Test
    public void shouldReturnRedirectUrlOnSuccessfulCreation() {
      // given
      mockSuccessfulRedirectFlowCreation();

      // when
      String redirectUrl = directDebitMandatesService
          .initiateDirectDebitMandateCreation(ANY_CLEAN_AIR_ZONE_ID, ANY_ACCOUNT_ID, ANY_RETURN_URL,
              ANY_SESSION_ID);

      // then
      assertThat(redirectUrl).isEqualTo(NEXT_URL);
    }

    @Test
    public void shouldThrowGoCardlessExceptionOnUnsuccessfulCreation() {
      // given
      mockUnsuccessfulRedirectFlowCreation();

      // when
      Throwable throwable = catchThrowable(() -> directDebitMandatesService
          .initiateDirectDebitMandateCreation(ANY_CLEAN_AIR_ZONE_ID, ANY_ACCOUNT_ID, ANY_RETURN_URL,
              ANY_SESSION_ID));

      // then
      assertThat(throwable).isInstanceOf(GoCardlessException.class);
    }

    private void mockUnsuccessfulRedirectFlowCreation() {
      RedirectFlowCreateRequest createRequest = mockRedirectFlowCreateRequest();
      GoCardlessApiException exception = Mockito.mock(GoCardlessApiException.class);
      when(exception.getErrorMessage()).thenReturn("API exception");
      when(createRequest.execute()).thenThrow(exception);
    }

    private void mockSuccessfulRedirectFlowCreation() {
      RedirectFlowCreateRequest createRequest = mockRedirectFlowCreateRequest();
      RedirectFlow response = Mockito.mock(RedirectFlow.class);

      when(createRequest.execute()).thenReturn(response);
      when(response.getRedirectUrl()).thenReturn(NEXT_URL);
    }

    private RedirectFlowCreateRequest mockRedirectFlowCreateRequest() {
      when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
          .thenReturn(goCardlessClient);
      RedirectFlowService redirectFlowService = Mockito.mock(RedirectFlowService.class);
      RedirectFlowCreateRequest createRequest = Mockito
          .mock(RedirectFlowCreateRequest.class);

      when(goCardlessClient.redirectFlows()).thenReturn(redirectFlowService);
      when(redirectFlowService.create()).thenReturn(createRequest);
      when(createRequest.withDescription(anyString())).thenReturn(createRequest);
      when(createRequest.withSessionToken(anyString())).thenReturn(createRequest);
      when(createRequest.withSuccessRedirectUrl(anyString())).thenReturn(createRequest);
      when(createRequest.withMetadata(anyString(), anyString())).thenReturn(createRequest);

      return createRequest;
    }
  }

  @Nested
  class CompleteDirectDebitMandateCreation {

    @Nested
    class WhenCallToCreateMandateInAccountsFails {

      @Test
      public void shouldThrowExceptionOnMissingMetadata() {
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

      @Test
      public void shouldThrowExternalServiceCallExceptionOnUnsuccessfulResponse() {
        // given
        String flowId = "my-flow";
        String sessionToken = "my-session-token";
        mockSuccessfulGoCardlessCreation();
        mockUnsuccessfulAccountsResponse();

        // when
        Throwable throwable = catchThrowable(() -> directDebitMandatesService
            .completeMandateCreation(ANY_CLEAN_AIR_ZONE_ID, flowId, sessionToken));

        // then
        assertThat(throwable).isInstanceOf(ExternalServiceCallException.class);
      }

      private void mockSuccessfulGoCardlessCreation() {
        when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
            .thenReturn(goCardlessClient);
        RedirectFlowService redirectFlowService = Mockito.mock(RedirectFlowService.class);
        RedirectFlowCompleteRequest completeRequest = Mockito
            .mock(RedirectFlowCompleteRequest.class);
        RedirectFlow response = Mockito.mock(RedirectFlow.class);
        Links links = mock(Links.class);

        when(goCardlessClient.redirectFlows()).thenReturn(redirectFlowService);
        when(redirectFlowService.complete(anyString())).thenReturn(completeRequest);
        when(completeRequest.withSessionToken(anyString())).thenReturn(completeRequest);
        when(completeRequest.execute()).thenReturn(response);
        when(response.getMetadata())
            .thenReturn(Collections.singletonMap("accountId", UUID.randomUUID().toString()));
        when(response.getLinks()).thenReturn(links);
        when(links.getMandate()).thenReturn("any-mandate-id");
      }

      private void mockUnsuccessfulAccountsResponse() {
        when(accountsRepository.createDirectDebitMandateSync(any(), any()))
            .thenReturn(
                Response.error(400, ResponseBody.create(MediaType.get("application/json"), "")));
      }

      private void mockAbsenceOfAccountIdMetadata() {
        when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
            .thenReturn(goCardlessClient);
        RedirectFlowService redirectFlowService = Mockito.mock(RedirectFlowService.class);
        RedirectFlowCompleteRequest completeRequest = Mockito
            .mock(RedirectFlowCompleteRequest.class);
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
      mockGoCardlessGetMandate(Status.ACTIVE.toString());

      mockCleanAirZonesInVccs();
      mockDirectDebitMandatesInAccountsWithStatuses(
          cacheableStatuses().iterator().next(),
          notCacheableStatuses().iterator().next()
      );
      mockAccountsMandateUpdateCall();

      // when
      List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
          .getDirectDebitMandates(ANY_ACCOUNT_ID);

      // then
      assertThat(directDebitMandates).hasSize(1);
      assertThat(directDebitMandates.iterator().next().getMandates()).hasSize(2);
    }

    @Test
    public void shouldReturnSpecificMandateStatus() {
      // given
      String paymentProviderMandateId = UUID.randomUUID().toString();
      mockGoCardlessSpecificMandate(paymentProviderMandateId);
      mockCleanAirZonesInVccs();
      mockDirectDebitMandatesInAccountsWithStatuses(paymentProviderMandateId);
      mockAccountsMandateUpdateCall();

      // when
      List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
          .getDirectDebitMandates(ANY_ACCOUNT_ID);

      // then
      assertThat(directDebitMandates).hasSize(1);
      assertThat(directDebitMandates.get(0).getMandates().get(0).getStatus())
          .isEqualTo(Status.ACTIVE.toString());
      assertThat(directDebitMandates.iterator().next().getMandates()).hasSize(2);
    }

    private void mockDirectDebitMandatesInAccountsWithStatuses(String paymentProviderMandateId) {
      List<DirectDebitMandate> mandates = Stream.of(cacheableStatuses().iterator().next(),
          notCacheableStatuses().iterator().next())
          .map(status -> DirectDebitMandate.builder()
              .accountId(ANY_ACCOUNT_ID)
              .cleanAirZoneId(ANY_CLEAN_AIR_ZONE_ID)
              .directDebitMandateId(UUID.randomUUID())
              .status(status)
              .paymentProviderMandateId(paymentProviderMandateId)
              .build())
          .collect(Collectors.toList());
      when(accountsRepository.getAccountDirectDebitMandatesSync(ANY_ACCOUNT_ID))
          .thenReturn(Response.success(AccountDirectDebitMandatesResponse.builder()
              .directDebitMandates(mandates)
              .build()));
    }

    private void mockGoCardlessSpecificMandate(String paymentProviderMandateId) {
      MandateService mandateService = Mockito.mock(MandateService.class);
      MandateGetRequest mandateGetRequest = Mockito.mock(MandateGetRequest.class);
      Mandate mandate = Mockito.mock(Mandate.class);
      Status mandateStatus = Mockito.mock(Status.class);
      when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
          .thenReturn(goCardlessClient);
      when(goCardlessClient.mandates()).thenReturn(mandateService);
      when(goCardlessClient.mandates().get(paymentProviderMandateId)).thenReturn(mandateGetRequest);
      when(goCardlessClient.mandates().get(paymentProviderMandateId).execute()).thenReturn(mandate);
      when(goCardlessClient.mandates().get(paymentProviderMandateId).execute().getStatus())
          .thenReturn(mandateStatus);
      when(goCardlessClient.mandates().get(paymentProviderMandateId).execute().getStatus().name())
          .thenReturn(Status.ACTIVE.name());
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
        mockAccountsMandateUpdateCall();
        mockGoCardlessGetMandate(randomNotCacheableStatusNotEqualTo(status));

        // when
        List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
            .getDirectDebitMandates(ANY_ACCOUNT_ID);

        // then
        assertThat(directDebitMandates).hasSize(1);
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
          mockGoCardlessGetMandate(status.toString());

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

    private void mockGoCardlessGetMandate(String mandateStatus) {
      MandateService mandateService = Mockito.mock(MandateService.class);
      MandateGetRequest mandateGetRequest = Mockito.mock(MandateGetRequest.class);
      Mandate mandate = Mockito.mock(Mandate.class);
      Status status = Mockito.mock(Status.class);
      when(goCardlessClientFactory.createClientFor(ANY_CLEAN_AIR_ZONE_ID))
          .thenReturn(goCardlessClient);
      when(goCardlessClient.mandates()).thenReturn(mandateService);
      when(goCardlessClient.mandates().get(any())).thenReturn(mandateGetRequest);
      when(goCardlessClient.mandates().get(any()).execute()).thenReturn(mandate);
      when(goCardlessClient.mandates().get(any()).execute().getStatus()).thenReturn(status);
      when(goCardlessClient.mandates().get(any()).execute().getStatus().name())
          .thenReturn(mandateStatus);
    }
  }

  public static Iterable<DirectDebitMandateStatus> cacheableStatuses() {
    return DirectDebitMandatesService.CACHEABLE_STATUSES;
  }

  public static Iterable<DirectDebitMandateStatus> notCacheableStatuses() {
    return EnumSet.complementOf(DirectDebitMandatesService.CACHEABLE_STATUSES);
  }
}