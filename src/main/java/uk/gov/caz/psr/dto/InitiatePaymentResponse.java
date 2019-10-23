package uk.gov.caz.psr.dto;

import java.util.UUID;
import lombok.Value;

@Value
public class InitiatePaymentResponse {

  UUID paymentId;
  String nextUrl;
}
