package uk.gov.caz.psr.controller;

import javax.ws.rs.QueryParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.psr.dto.VehiclePaymentHistoryResponse;

/**
 * API specification (swagger) for a controller which returns payment history for VRN.
 */
@RequestMapping(
    produces = MediaType.APPLICATION_JSON_VALUE,
    value = VehiclePaymentHistoryController.BASE_PATH
)
@Validated
public interface VehiclePaymentHistoryControllerApiSpec {

  /**
   * Fetches vehicle historical payments.
   * @param vrn vehicle identification
   * @param pageNumber page number to get
   * @param pageSize size of page
   * @return historical entries
   */
  @GetMapping(VehiclePaymentHistoryController.GET_VEHICLE_HISTORY)
  @ResponseStatus(HttpStatus.OK)
  ResponseEntity<VehiclePaymentHistoryResponse> historyForVehicle(
      @PathVariable("vrn") String vrn, @QueryParam("pageNumber") Integer pageNumber,
      @QueryParam("pageSize") Integer pageSize);
}
