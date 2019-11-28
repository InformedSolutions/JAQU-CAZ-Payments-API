package uk.gov.caz.psr.repository.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.gov.caz.psr.model.info.PaymentInfo;
import uk.gov.caz.psr.model.info.VehicleEntrantPaymentInfo;

public interface VehicleEntrantPaymentInfoRepository extends PagingAndSortingRepository<VehicleEntrantPaymentInfo, UUID>,
    JpaSpecificationExecutor<VehicleEntrantPaymentInfo> {
}
