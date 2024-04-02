/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.model.Branch;
import com.accionmfb.omnix.agency.payload.*;
import com.accionmfb.omnix.agency.service.utils.Pair;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author bokon
 */
public interface GenericService {

    void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId);

    void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status);

    String postToT24(String requestBody);

    String encryptString(String textToEncrypt, String token);

    String decryptString(String textToDecrypt, String encryptionKey);

    String validateT24Response(String responseString);

    String getT24TransIdFromResponse(String response);

    String getTextFromOFSResponse(String ofsResponse, String textToExtract);

    String formatDateWithHyphen(String dateToFormat);

    String generateAccountNumbering(String customerNumber, String userCredentials, String branchCode, String productId, String token, String requestId);

    String generateMnemonic(int max);

    String generateTransRef(String transType);

    String hash(String plainText, String algorithm);

    char getTimePeriod();

    Branch getBranchUsingBranchCode(String branchCode);

    String hashCableTVValidationRequest(OmnixRequestPayload requestPayload);

    String hashElectricityValidationRequest(OmnixRequestPayload requestPayload);

    String hashNIPValidationRequest(OmnixRequestPayload requestPayload);

    String hashAirtimeValidationRequest(OmnixRequestPayload requestPayload);

    String hashLocalTransferValidationRequest(LocalTransferWithInternalPayload requestPayload);

    String hashAccountDetailsValidationRequest(OmnixRequestPayload requestPayload);

    String hashLocalFundsTransferWithChargesValidationRequest(LocalTransferWithChargesPayload requestPayload);

    String hashLocalFundsTransferValidationRequest(LocalTransferWithChargesPayload requestPayload);

    String hashAccountBalanceRequest(OmnixRequestPayload requestPayload);
    String hashAccountBalanceRequest2(AccountNumberPayload requestPayload);

    String hashNIPNameEnquiryValidationRequest(NIPNameEnquiryPayload requestPayload);

    String hashAccountStatementRequest(OmnixRequestPayload requestPayload);

    String hashCustomerDetailsRequest(OmnixRequestPayload requestPayload);

    String hashAccountOpeningRequest(OmnixRequestPayload requestPayload);

    String hashCableTVTransRefValidationRequest(OmnixRequestPayload requestPayload);

    String hashCableTVBillerValidationRequest(OmnixRequestPayload requestPayload);

    String hashElectricityDetailsValidationRequest(OmnixRequestPayload requestPayload);

    String hashElectricityBillersValidationRequest(OmnixRequestPayload requestPayload);

    String hashElectricityBillerValidationRequest(OmnixRequestPayload requestPayload);

    String hashCustomerWithBvnRequest(OmnixRequestPayload requestPayload);

    String hashCustomerWithoutBvnRequest(OmnixRequestPayload requestPayload);

    String hashTransactionQueryRequest(OmnixRequestPayload requestPayload);

    String hashNIPNameEnquiryRequest(OmnixRequestPayload requestPayload);

    String hashDataValidationRequest(OmnixRequestPayload requestPayload);

    String formatOfsUserCredentials(String ofs, String userCredentials);

    String getTransactionType(String channel, String transType);

    String decryptTriftaString(String textToDecrypt, String encryptionKey);
    
    String encryptTriftaString(String textToDecrypt, String encryptionKey);

    String generateFTOFS(String transRef, String debitAccount, String creditAccount, String amount, String narration, String transType, String inputter, String authorizer);
    CompletableFuture<String> sendDebitSMS(NotificationPayload requestPayload);


    String postToMiddleware(String requestEndpoint, String requestBody);

    String postToMiddleware(String requestEndPoint, String requestBody, String middlewareAuthorization, String middlewareUsername);
    
    public Pair<String, String> getResponseMessage(String sResponse);

}
