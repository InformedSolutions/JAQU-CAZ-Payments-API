package uk.gov.caz.psr.service.paymentinfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uk.gov.caz.psr.model.Payment;

public interface PaymentRepository2 extends JpaRepository<Payment, Long>,
    JpaSpecificationExecutor<Payment> {

}
