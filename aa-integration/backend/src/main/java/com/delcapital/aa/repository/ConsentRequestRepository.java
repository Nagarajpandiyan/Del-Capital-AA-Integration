package com.delcapital.aa.repository;

import com.delcapital.aa.model.ConsentRequest;
import com.delcapital.aa.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, UUID> {
    Optional<ConsentRequest> findByDigioConsentId(String digioConsentId);
    List<ConsentRequest> findByCustomer(Customer customer);
    List<ConsentRequest> findByCustomerOrderByCreatedAtDesc(Customer customer);
}
