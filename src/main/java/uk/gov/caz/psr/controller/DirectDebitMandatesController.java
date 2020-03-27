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
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.service.directdebit.DirectDebitMandatesService;
import uk.gov.caz.psr.util.CleanAirZoneWithMandatesToDtoConverter;

@RestController
@AllArgsConstructor
@Slf4j
public class DirectDebitMandatesController implements DirectDebitMandatesControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments/accounts/{accountId}/direct-debit-mandates";

  private final DirectDebitMandatesService directDebitMandatesService;
  private final CleanAirZoneWithMandatesToDtoConverter converter;

  @Override
  public ResponseEntity<DirectDebitMandatesResponse> getDirectDebitMandates(UUID accountId) {
    List<CleanAirZoneWithMandates> directDebitMandates = directDebitMandatesService
        .getDirectDebitMandates(accountId);
    return ResponseEntity.ok(converter.toDirectDebitMandatesResponse(directDebitMandates));
  }

  @Override
  public ResponseEntity<CreateDirectDebitMandateResponse> createDirectDebitMandate(UUID accountId,
      CreateDirectDebitMandateRequest request) {
    request.validate();

    String nextUrl = directDebitMandatesService
        .createDirectDebitMandate(request.getCleanAirZoneId(), accountId, request.getReturnUrl());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(CreateDirectDebitMandateResponse.builder().nextUrl(nextUrl).build());
  }
}
