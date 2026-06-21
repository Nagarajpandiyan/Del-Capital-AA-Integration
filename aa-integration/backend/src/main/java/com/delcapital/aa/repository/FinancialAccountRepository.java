package com.delcapital.aa.repository;

import com.delcapital.aa.model.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {
    @Query("SELECT a FROM FinancialAccount a WHERE a.fetchId = :fetchId")
    List<FinancialAccount> findByFetchId(UUID fetchId);
    List<FinancialAccount> findByCustomerId(UUID customerId);
}
