package uk.gov.caz.psr.dto;

public class PaymentUpdateSuccessResponse {
  private static final PaymentUpdateSuccessResponse INSTANCE = new PaymentUpdateSuccessResponse();

  public static PaymentUpdateSuccessResponse getInstance() {
    return INSTANCE;
  }

  public String getDetail() {
    return "Payment status updated successfully";
  }
}
