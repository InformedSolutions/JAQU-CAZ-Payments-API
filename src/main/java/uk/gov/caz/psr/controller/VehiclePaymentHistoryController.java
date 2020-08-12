package uk.gov.caz.psr.controller;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Optional;
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

  private static final int DEFAULT_PAGE_NUMBER = 1;

  private static final int DEFAULT_PAGE_SIZE = 10;

  @VisibleForTesting
  public static final String GET_VEHICLE_HISTORY = "{vrn}";

  @Override
  public ResponseEntity<VehiclePaymentHistoryResponse> historyForVehicle(
      @PathVariable("vrn") String vrn, @QueryParam("pageNumber") Integer pageNumber,
      @QueryParam("pageSize") Integer pageSize) {


    int nonNullPageNumber = Optional.ofNullable(pageNumber).orElse(DEFAULT_PAGE_NUMBER);
    int nonNullPageSize = Optional.ofNullable(pageSize).orElse(DEFAULT_PAGE_SIZE);

    Preconditions.checkArgument(nonNullPageNumber >= 1, "Page number should "
        + "be equal or greater than one");
    Preconditions.checkArgument(nonNullPageSize >= 0, "Page size should be "
        + "equal or greater than zero");

    Page<EntrantPaymentEnriched> entrantPaymentEnrichedPage = service.paymentHistoryForVehicle(vrn,
        nonNullPageNumber - 1, nonNullPageSize);
    VehiclePaymentHistoryResponse response = VehiclePaymentHistoryResponse
        .fromPage(entrantPaymentEnrichedPage);

    return ResponseEntity.ok(response);
  }
}
