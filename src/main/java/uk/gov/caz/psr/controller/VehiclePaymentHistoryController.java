package uk.gov.caz.psr.controller;

import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.psr.dto.VehiclePaymentHistoryResponse;
import uk.gov.caz.psr.model.EntrantPaymentEnriched;
import uk.gov.caz.psr.service.VehiclePaymentHistoryService;

/**
 * Controller returning payment history for VRN.
 */
@RestController
@AllArgsConstructor
public class VehiclePaymentHistoryController implements VehiclePaymentHistoryControllerApiSpec {

  private final VehiclePaymentHistoryService service;

  public static final String BASE_PATH = "/v1/payments/vehicles-history";

  @Override
  public ResponseEntity<VehiclePaymentHistoryResponse> historyForVehicle(
      @PathVariable("vrn") String vrn, @QueryParam("pageNumber") int pageNumber,
      @QueryParam("pageSize") int pageSize) {
    Page<EntrantPaymentEnriched> entrantPaymentEnrichedPage = service.paymentHistoryForVehicle(vrn,
        pageNumber, pageSize);
    VehiclePaymentHistoryResponse response = VehiclePaymentHistoryResponse
        .fromPage(entrantPaymentEnrichedPage);
    return ResponseEntity.ok(response);
  }
}
