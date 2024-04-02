/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.AccountValidation;
import com.accionmfb.omnix.agency.payload.TransactionPosting;

/**
 *
 * @author bokon
 */
public interface ZenithBankService {

    Object checkIfSameRequestId(String requestId);

    String getAccountDetails(String token, AccountValidation requestPaayload);

    String processDepositTransaction(String token, TransactionPosting requestPayload);

//    boolean validateTransactionPayload(String token, TransactionPayload requestPayload);

//    String processTransactionQuery(String token, TransactionPayload requestPayload);
}
