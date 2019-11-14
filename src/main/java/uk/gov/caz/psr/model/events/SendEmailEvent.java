package uk.gov.caz.psr.model.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import uk.gov.caz.psr.model.Payment;

/**
 * Event raised to enable sending of receipt email.
 */
@Getter
public class SendEmailEvent extends ApplicationEvent {
  private static final long serialVersionUID = 1911485375258518221L;
  private Payment payment;

  public SendEmailEvent(Object source, Payment payment) {
    super(source);
    this.payment = payment;
  }

}
