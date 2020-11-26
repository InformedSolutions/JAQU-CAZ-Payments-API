package uk.gov.caz.psr.controller;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.directdebit.CompleteMandateCreationRequest;
import uk.gov.caz.psr.service.directdebit.DirectDebitMandatesService;

@RestController
@AllArgsConstructor
@Slf4j
public class DirectDebitRedirectFlowsController implements
    DirectDebitRedirectFlowsControllerApiSpec {

  public static final String BASE_PATH = "/v1/payments/direct_debit_redirect_flows/"
      + "{flowId}/complete";

  private final DirectDebitMandatesService directDebitMandatesService;

  @Override
  public ResponseEntity<Void> completeMandateCreation(String flowId,
      CompleteMandateCreationRequest request) {
    request.validate();

    directDebitMandatesService.completeMandateCreation(
        UUID.fromString(request.getCleanAirZoneId()),
        flowId,
        request.getSessionToken()
    );

    return ResponseEntity.ok().build();
  }
}
