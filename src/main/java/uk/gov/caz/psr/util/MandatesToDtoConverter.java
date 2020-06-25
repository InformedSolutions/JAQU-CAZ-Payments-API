package uk.gov.caz.psr.util;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesForCazResponse;
import uk.gov.caz.psr.dto.directdebit.DirectDebitMandatesResponse;
import uk.gov.caz.psr.model.directdebit.Mandate;

/**
 * Model-to-dto-converter for a list of {@link Mandate}.
 */
@Component
public class MandatesToDtoConverter {

  /**
   * Converts the passed {@code Mandates} to an instance of
   * {@link DirectDebitMandatesForCazResponse}.
   */
  public DirectDebitMandatesForCazResponse toDirectDebitMandatesResponse(
      List<Mandate> mandates) {

    return DirectDebitMandatesForCazResponse.builder()
        .mandates(mandates.stream()
            .map(mandate -> DirectDebitMandatesResponse.CleanAirZoneWithMandates.Mandate.builder()
                .id(mandate.getId())
                .created(mandate.getCreated())
                .status(mandate.getStatus()).build()
            ).collect(Collectors.toList()))
        .build();
  }
}
