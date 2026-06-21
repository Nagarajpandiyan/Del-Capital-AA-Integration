package com.delcapital.aa.repository;

import com.delcapital.aa.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    List<FinancialTransaction> findByAccountId(UUID accountId);
    List<FinancialTransaction> findByFetchId(UUID fetchId);
}
