package uk.gov.caz.psr.model;

/**
 * Describes the entity which is responsible for updating the state of an instance of {@link
 * CazEntrantPayment}.
 */
public enum EntrantPaymentUpdateActor {
  USER,
  VCCS_API,
  LA
}
