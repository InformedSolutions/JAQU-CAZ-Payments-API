package uk.gov.caz.psr.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.psr.service.CleanAirZoneService;
import uk.gov.caz.psr.service.receipt.CustomPaymentReceiptEmailCreator;
import uk.gov.caz.psr.service.receipt.DirectDebitPaymentReceiptRequestCreator;
import uk.gov.caz.psr.service.receipt.FleetPaymentReceiptRequestCreator;
import uk.gov.caz.psr.service.receipt.OfflinePaymentReceiptEmailCreator;
import uk.gov.caz.psr.service.receipt.SingleVehiclePaymentReceiptRequestCreator;
import uk.gov.caz.psr.util.CurrencyFormatter;

@Configuration
public class PaymentReceiptEmailConfiguration {

  /**
   * Creates a list of email-request creators. Please bear in mind that the order is crucial.
   */
  @Bean
  public List<CustomPaymentReceiptEmailCreator> emailReceiptRequestCreators(
      CurrencyFormatter currencyFormatter,
      CleanAirZoneService cleanAirZoneNameGetterService,
      ObjectMapper objectMapper,
      @Value("${services.sqs.offline-payment-id}") String offlinePaymentTemplateId,
      @Value("${services.sqs.template-id}") String singleVehicleTemplateId,
      @Value("${services.sqs.account-payment-template-id}") String multipleVehicleTemplateId,
      @Value("${services.sqs.direct-debit-payment-template-id}") String directDebitTemplateId) {
    return ImmutableList.of(
        new DirectDebitPaymentReceiptRequestCreator(currencyFormatter,
            cleanAirZoneNameGetterService,objectMapper, directDebitTemplateId),
        new OfflinePaymentReceiptEmailCreator(currencyFormatter, cleanAirZoneNameGetterService,
            objectMapper, offlinePaymentTemplateId),
        new SingleVehiclePaymentReceiptRequestCreator(currencyFormatter,
            cleanAirZoneNameGetterService, objectMapper, singleVehicleTemplateId),
        new FleetPaymentReceiptRequestCreator(currencyFormatter, cleanAirZoneNameGetterService,
            objectMapper, multipleVehicleTemplateId)
    );
  }
}
