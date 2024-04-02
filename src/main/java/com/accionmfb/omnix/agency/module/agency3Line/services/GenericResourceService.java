package com.accionmfb.omnix.agency.module.agency3Line.services;


import com.accionmfb.omnix.agency.payload.DepositRequestPayload;

import java.util.Map;


/**
 * @author Chikodi on 30/3/2024
 */
public interface GenericResourceService {
    String depositNotification(DepositRequestPayload depositRequestPayload);

    String getAccountBalance(String accountNumber, Map<String, String> accessCredentials);

    String getAccountDetails(String accountNumber, Map<String, String> accessCredentials);

    String getCustomerDetailsByMobileNo(String mobileNumber, String customerPIN, String verificationMode);

    String openAccount(Map<String, String> customerInfo, double initialDepositAmount, String accountType, String openingBranch);

    String posCallback(String transactionResponse, String terminalID, String merchantID, String transactionType);

    String verifyTransaction(String transactionID, String transactionType, String transactionDetails);

    String withdrawal(double withdrawalAmount, String accountNumber, String withdrawalReason, String withdrawalAuthorizationCode);

    }
