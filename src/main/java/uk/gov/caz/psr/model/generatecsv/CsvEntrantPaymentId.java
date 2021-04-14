package uk.gov.caz.psr.model.generatecsv;

import java.io.Serializable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CsvEntrantPaymentId implements Serializable {

  UUID entrantPaymentId;

  UUID entrantPaymentMatchId;
}
