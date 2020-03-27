package uk.gov.caz.psr.service.directdebit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;
import uk.gov.caz.psr.dto.accounts.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.external.Link;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateLinks;
import uk.gov.caz.psr.dto.external.directdebit.mandates.MandateResponse;
import uk.gov.caz.psr.repository.AccountsRepository;
import uk.gov.caz.psr.repository.ExternalDirectDebitRepository;

@ExtendWith(MockitoExtension.class)
class DirectDebitMandatesServiceTest {

  @Mock
  private ExternalDirectDebitRepository externalDirectDebitRepository;

  @Mock
  private AccountsRepository accountsRepository;

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
}