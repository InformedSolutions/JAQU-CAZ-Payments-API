package uk.gov.caz.psr.util;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse;
import uk.gov.caz.psr.model.directdebit.CleanAirZoneWithMandates;
import uk.gov.caz.psr.model.directdebit.Mandate;

/**
 * Model-to-dto-converter for a list of {@link CleanAirZoneWithMandates}.
 */
@Component
public class CleanAirZoneWithMandatesToDtoConverter {

  /**
   * Converts the passed {@code cleanAirZoneWithMandates} to an instance of {@link
   * DirectDebitMandatesResponse}.
   */
  public DirectDebitMandatesResponse toDirectDebitMandatesResponse(
      List<CleanAirZoneWithMandates> cleanAirZoneWithMandates) {
    return DirectDebitMandatesResponse.builder()
        .cleanAirZones(convert(cleanAirZoneWithMandates))
        .build();
  }

  private List<DirectDebitMandatesResponse.CleanAirZoneWithMandates> convert(
      List<CleanAirZoneWithMandates> cleanAirZoneWithMandates) {
    return cleanAirZoneWithMandates.stream()
        .map(cleanAirZoneWithMandate ->
            DirectDebitMandatesResponse.CleanAirZoneWithMandates.builder()
                .cazId(cleanAirZoneWithMandate.getCleanAirZoneId())
                .directDebitEnabled(cleanAirZoneWithMandate.isDirectDebitEnabled())
                .cazName(cleanAirZoneWithMandate.getCazName())
                .mandates(convertMandates(cleanAirZoneWithMandate.getMandates()))
                .build())
        .collect(Collectors.toList());
  }

  private List<DirectDebitMandatesResponse.CleanAirZoneWithMandates.Mandate> convertMandates(
      List<Mandate> mandates) {
    return mandates.stream()
        .map(mandate -> DirectDebitMandatesResponse.CleanAirZoneWithMandates.Mandate.builder()
            .id(mandate.getId())
            .status(mandate.getStatus())
            .created(mandate.getCreated())
            .build())
        .collect(Collectors.toList());
  }
}
