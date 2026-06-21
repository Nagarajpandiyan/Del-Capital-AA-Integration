package com.delcapital.aa.repository;

import com.delcapital.aa.model.FetchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FetchRequestRepository extends JpaRepository<FetchRequest, UUID> {
    Optional<FetchRequest> findByDigioFetchId(String digioFetchId);
    List<FetchRequest> findByConsentRequestId(UUID consentId);
}
