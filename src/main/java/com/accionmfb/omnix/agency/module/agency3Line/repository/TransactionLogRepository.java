package com.accionmfb.omnix.agency.module.agency3Line.repository;

import com.accionmfb.omnix.agency.module.agency3Line.model.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    TransactionLog findByTransactionReference(String transactionReference);

    List<TransactionLog> findByAccountNumber(String accountNumber);

    List<TransactionLog> findByDateBetween(String startDate, String endDate);

    List<TransactionLog> findByAccountNumberAndDateRange(String accountNumber, String startDate, String endDate);
}

