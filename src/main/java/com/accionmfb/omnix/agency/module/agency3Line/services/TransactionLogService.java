package com.accionmfb.omnix.agency.module.agency3Line.services;

public interface TransactionLogService {
    void updateTransactionLog(String accountNumber, double amount, String transactionType, String authorizationCode);

    void updateTransactionLog();

    void updateTransactionLog(String transactionID, String transactionType, String transactionDetails);
}

