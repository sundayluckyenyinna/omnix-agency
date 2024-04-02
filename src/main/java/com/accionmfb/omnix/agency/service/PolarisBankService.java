/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;

/**
 *
 * @author bokon
 */
public interface PolarisBankService {

    Object checkIfSameRequestId(String requestId);

    boolean validateAccountNumberPayload(String token, AccountNumberPayload requestPaayload);

    String getAccountDetails(String token, AccountNumberPayload requestPaayload);

    boolean validateDepositTransactionPayload(String token, DepositRequestPayload requestPayload);

    String processDepositTransaction(String token, DepositRequestPayload requestPayload);

    boolean validateTransactionPayload(String token, TransactionPayload requestPayload);

    String processTransactionQuery(String token, TransactionPayload requestPayload);

}
