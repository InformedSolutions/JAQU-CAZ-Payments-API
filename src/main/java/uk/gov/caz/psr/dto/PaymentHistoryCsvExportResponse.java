package uk.gov.caz.psr.dto;

import io.swagger.annotations.ApiModelProperty;
import java.net.URL;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that represents details of an exported csv file.
 */
@Value
@Builder
public class PaymentHistoryCsvExportResponse {

  /**
   * String containing the url to the s3 file.
   */
  @ApiModelProperty(value = "swagger.model.descriptions.payment-history-csv-export.fileUrl")
  URL fileUrl;

  /**
   * String containing the bucket name.
   */
  @ApiModelProperty(value = "swagger.model.descriptions.payment-history-csv-export.bucketName")
  String bucketName;
}
