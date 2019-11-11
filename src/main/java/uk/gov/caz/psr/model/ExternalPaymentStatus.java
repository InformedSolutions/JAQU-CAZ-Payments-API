package uk.gov.caz.psr.model;

/**
 * Status of the payment as stored in GOV UK Pay with two additional states: a) INITIATED, when a
 * payment is created internally, but not yet submitted to GOV UK Pay; b) UNKNOWN, if a status
 * returned from GOV UK Pay is not equal to one of the predefined ones.
 */
public enum ExternalPaymentStatus {
  INITIATED,

  CREATED,
  STARTED,
  SUBMITTED,

  SUCCESS,
  FAILED,
  CANCELLED,
  ERROR,

  UNKNOWN,
}
