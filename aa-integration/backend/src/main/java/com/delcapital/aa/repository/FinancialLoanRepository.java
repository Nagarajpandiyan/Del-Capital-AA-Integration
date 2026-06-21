package com.delcapital.aa.repository;

import com.delcapital.aa.model.FinancialLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialLoanRepository extends JpaRepository<FinancialLoan, UUID> {
    @Query("SELECT l FROM FinancialLoan l WHERE l.fetchId = :fetchId")
    List<FinancialLoan> findByFetchId(UUID fetchId);
    List<FinancialLoan> findByCustomerId(UUID customerId);
}
