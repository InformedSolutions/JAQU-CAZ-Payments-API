package uk.gov.caz.psr.dto;

import static uk.gov.caz.psr.util.AttributesNormaliser.normalizeVrn;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import uk.gov.caz.psr.util.Strings;

@Value
@Builder
public class ModificationHistoryDetails {

  @ApiModelProperty(
      value = "${swagger.model.descriptions.references-history.modification-amount}"
  )
  int amount;

  @ApiModelProperty(
      value = "${swagger.model.descriptions.references-history.modification-travel-date}"
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate travelDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.references-history.modification-vrn}")
  String vrn;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.references-history.modification-case-reference}")
  String caseReference;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.references-history.modification-timestamp}")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime modificationTimestamp;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.references-history.modification-entrant-payment-status}")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  String entrantPaymentStatus;

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  @ToString.Include(name = "vrn")
  private String maskedVrn() {
    return Strings.mask(normalizeVrn(vrn));
  }
}
