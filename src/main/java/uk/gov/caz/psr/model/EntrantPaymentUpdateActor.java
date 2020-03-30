package uk.gov.caz.psr.model;

/**
 * Describes the entity which is responsible for updating the state of an instance of {@link
 * EntrantPayment}.
 */
public enum EntrantPaymentUpdateActor {
  USER,
  VCCS_API,
  LA
}
