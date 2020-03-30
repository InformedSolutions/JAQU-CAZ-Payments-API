package uk.gov.caz.psr.model;

/**
 * A method of payment in GOV UK Pay service.
 */
public enum PaymentMethod {
  /**
   * Paid by credit or debit card.
   */
  CREDIT_DEBIT_CARD {
    @Override
    public <T> T accept(PaymentMethodVisitor<T> visitor) {
      return visitor.visitCreditDebitCard();
    }
  },
  /**
   * Paid by direct debit.
   */
  DIRECT_DEBIT {
    @Override
    public <T> T accept(PaymentMethodVisitor<T> visitor) {
      return visitor.visitDirectDebit();
    }
  };

  /**
   * Allows implementers to do future-safe map-like operations on {@link PaymentMethod} enum.
   *
   * @param <T> Specifies type of map result.
   */
  public interface PaymentMethodVisitor<T> {

    /**
     * Method called by CREDIT_DEBIT_CARD enum value.
     *
     * @return result of caller defined operation.
     */
    T visitCreditDebitCard();

    /**
     * Method called by DIRECT_DEBIT enum value.
     *
     * @return result of caller defined operation.
     */
    T visitDirectDebit();
  }

  /**
   * Each {@link PaymentMethod} enum value implements it and delegates to matching method on {@link
   * PaymentMethodVisitor} implementation hence allowing to do future-safe operations on enum
   * values.
   *
   * @param <T> Specifies type of map result.
   * @return result of caller defined operation.
   */
  public abstract <T> T accept(PaymentMethodVisitor<T> visitor);
}
