package uk.gov.caz.psr.controller;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateRequest;
import uk.gov.caz.psr.dto.CreateDirectDebitMandateResponse;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesForCazResponse;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.model.directdebit.Mandate;
import uk.gov.caz.psr.service.directdebit.DirectDebitMandatesService;
import uk.gov.caz.psr.util.CleanAirZoneWithMandatesToDtoConverter;
import uk.gov.caz.psr.util.MandatesToDtoConverter;

@RestController
@AllArgsConstructor
@Slf4j
public class DirectDebitMandatesController implements DirectDebitMandatesControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments/accounts/{accountId}/direct-debit-mandates";
  public static final String FOR_CAZ_PATH = BASE_PATH + "/{cleanAirZoneId}";

  private final DirectDebitMandatesService directDebitMandatesService;
  private final CleanAirZoneWithMandatesToDtoConverter converter;
  private final MandatesToDtoConverter mandatesToDtoConverter;

  @Override
  public ResponseEntity<DirectDebitMandatesResponse> getDirectDebitMandates(UUID accountId) {
    List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
        .getDirectDebitMandates(accountId);
    return ResponseEntity.ok(converter.toDirectDebitMandatesResponse(directDebitMandates));
  }

  @Override
  public ResponseEntity<DirectDebitMandatesForCazResponse> getDirectDebitMandatesForCaz(
      UUID accountId, UUID cleanAirZoneId) {
    List<Mandate> mandates = directDebitMandatesService
        .getMandatesForCazWithStatus(accountId, cleanAirZoneId);
    return ResponseEntity
        .ok(mandatesToDtoConverter.toDirectDebitMandatesResponse(mandates));
  }

  @Override
  public ResponseEntity<CreateDirectDebitMandateResponse> createDirectDebitMandate(UUID accountId,
      CreateDirectDebitMandateRequest request) {
    request.validate();

    String nextUrl = directDebitMandatesService
        .createDirectDebitMandate(request.getCleanAirZoneId(), accountId, request.getReturnUrl(),
            request.getSessionId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateDirectDebitMandateResponse.builder().nextUrl(nextUrl).build());
  }
}
