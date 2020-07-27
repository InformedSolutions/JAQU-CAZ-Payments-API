package uk.gov.caz.psr.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.caz.psr.dto.PaymentDetailsResponse.VehicleEntrantPaymentDetails;

class PaymentDetailsResponseTest {

  @Test
  public void shouldMaskVrn() {
    // given
    VehicleEntrantPaymentDetails entrantPaymentDetails = VehicleEntrantPaymentDetails.builder()
        .vrn("CAS312")
        .build();

    // when
    String result = entrantPaymentDetails.toString();

    // then
    assertThat(result).contains("CAS***");
  }
}