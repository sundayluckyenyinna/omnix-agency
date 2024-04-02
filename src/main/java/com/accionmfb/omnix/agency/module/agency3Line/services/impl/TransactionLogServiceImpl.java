package com.accionmfb.omnix.agency.module.agency3Line.services.impl;

import com.accionmfb.omnix.agency.module.agency3Line.services.TransactionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionLogServiceImpl implements TransactionLogService {

    @Override
    public void updateTransactionLog(String accountNumber, double amount, String transactionType, String authorizationCode) {
        log.info("Updating transaction log: Account Number={}, Amount={}, Transaction Type={}, Authorization Code={}",
                accountNumber, amount, transactionType, authorizationCode);
    }

    @Override
    public void updateTransactionLog() {

    }

    @Override
    public void updateTransactionLog(String transactionID, String transactionType, String transactionDetails) {

    }
}

