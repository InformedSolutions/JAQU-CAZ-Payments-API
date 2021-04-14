package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.dto.ReferencesHistoryResponse.VehicleEntrantPaymentDetails;

class ReferencesHistoryResponseTest {

  private static final String VRN = "CAS123";

  @Test
  void toStringMasksTheVrnValue() {
    List<VehicleEntrantPaymentDetails> paymentDetails = Arrays
        .asList(VehicleEntrantPaymentDetails.builder()
            .vrn(VRN)
            .build());

    ReferencesHistoryResponse response = ReferencesHistoryResponse.builder()
        .lineItems(paymentDetails)
        .build();

    assertThat(response.toString().contains(VRN)).isFalse();
  }
}