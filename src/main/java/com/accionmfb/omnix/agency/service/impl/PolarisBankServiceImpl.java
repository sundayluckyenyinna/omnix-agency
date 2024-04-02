/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.Account;
import com.accionmfb.omnix.agency.model.AppUser;
import com.accionmfb.omnix.agency.model.Customer;
import com.accionmfb.omnix.agency.model.FundsTransfer;
import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.ChargeTypes;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.LocalTransferWithChargesPayload;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import com.accionmfb.omnix.agency.payload.OmnixResponsePayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.AccountService;
import com.accionmfb.omnix.agency.service.FundsTransferService;
import com.accionmfb.omnix.agency.service.GenericService;
import com.accionmfb.omnix.agency.service.PolarisBankService;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class PolarisBankServiceImpl implements PolarisBankService {

    @Autowired
    GenericService genericService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    Gson gson;
    @Autowired
    MessageSource messageSource;
    @Autowired
    AgencyRepository agencyRepository;
    @Autowired
    AccountService accountService;
    @Autowired
    FundsTransferService ftService;
    private String POLARIS_BANK_CONTRA_ACCOUNT = "NGN1045100010001";

    @Override
    public boolean validateAccountNumberPayload(String token, AccountNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String getAccountDetails(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Polaris Bank Account Validation", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from Polaris Bank
            if (!"POLARIS".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Polaris Bank Account Validation", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Polaris Bank Account Validation", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the account exist
            Account account = agencyRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Polaris Bank Account Validation", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Polaris Bank Account Validation", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Polaris Bank Account Validation", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Polaris Bank Account Validation", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload accRequestPayload = new OmnixRequestPayload();
            accRequestPayload.setAccountNumber(requestPayload.getAccountNumber());
            accRequestPayload.setRequestId(requestPayload.getRequestId());
            accRequestPayload.setToken(token);
            accRequestPayload.setHash(genericService.hashAccountBalanceRequest(accRequestPayload));
            String accRequestJson = gson.toJson(accRequestPayload);

            //Log the request
            genericService.generateLog("Polaris Bank Account Validation", token, accRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());

            //Call the Account Microservice 
            response = accountService.accountDetails(token, accRequestJson);
            //Log the error
            genericService.generateLog("Polaris Bank Account Validation", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Polaris Bank Account Validation", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Polaris Bank Account Validation", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public Object checkIfSameRequestId(String requestId) {
        try {
            FundsTransfer ftRecord = agencyRepository.getFundsTransferUsingRequestId(requestId);
            return ftRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public boolean validateDepositTransactionPayload(String token, DepositRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getAccountNumber());
        rawString.add(requestPayload.getAmount());
        rawString.add(requestPayload.getNarration());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processDepositTransaction(String token, DepositRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Polaris Bank Deposit Transaction", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from Polaris Bank
            if (!"POLARIS".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Polaris Bank Deposit Transaction", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Polaris Bank Deposit Transaction", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the account exist
            Account account = agencyRepository.getAccountUsingAccountNumber(requestPayload.getAccountNumber());
            if (account == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Polaris Bank Deposit Transaction", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Polaris Bank Deposit Transaction", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //check customer type
            Customer customer = agencyRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
            String customerType = "";
            if (customer != null) {
                customerType = customer.getCustomerType();
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Polaris Bank Deposit Transaction", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Polaris Bank Deposit Transaction", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            LocalTransferWithChargesPayload ftRequestPayload = new LocalTransferWithChargesPayload();
            ftRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            ftRequestPayload.setDebitAccount(POLARIS_BANK_CONTRA_ACCOUNT);
            ftRequestPayload.setCreditAccount(requestPayload.getAccountNumber());
            ftRequestPayload.setAmount(requestPayload.getAmount());
            ftRequestPayload.setNarration(requestPayload.getNarration());
            ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
            ftRequestPayload.setBranchCode("NG0010068");
            ftRequestPayload.setInputter("Polaris-" + requestPayload.getMobileNumber());
            ftRequestPayload.setAuthorizer("Polaris-" + requestPayload.getMobileNumber());
            ftRequestPayload.setNoOfAuthorizer("0");
            ftRequestPayload.setRequestId(requestPayload.getRequestId());
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashLocalFundsTransferValidationRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Polaris Bank Deposit Transaction", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.localTransferWithInternal(token, ftRequestJson);
            //check 
            if (response.contains("\"responseCode\":\"00\",")) {
                BigDecimal excessIndivChargePercent = BigDecimal.ZERO;
                excessIndivChargePercent = new BigDecimal(customerType.equalsIgnoreCase("Individual") ? "2" : "3");
                BigDecimal cbnTxnThreshHoldIndiv = new BigDecimal(customerType.equalsIgnoreCase("Individual") ? "500000" : "3000000");
                BigDecimal amountToCharge = BigDecimal.ZERO;
                BigDecimal percent = new BigDecimal("100");
                BigDecimal amountDifference = BigDecimal.ZERO;
                BigDecimal transAmount = new BigDecimal(requestPayload.getAmount().trim().replace(",", ""));
                BigDecimal stampDutyAmount = new BigDecimal("10000");
                if (transAmount.compareTo(stampDutyAmount) > 0) {
                    ftRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
                    ftRequestPayload.setDebitAccount(requestPayload.getAccountNumber());
                    ftRequestPayload.setCreditAccount("NGN1604800010001");
                    ftRequestPayload.setAmount("50");
                    ftRequestPayload.setNarration("Stamp Duty Charge");
                    ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
                    ftRequestPayload.setBranchCode("NG0010068");
                    ftRequestPayload.setInputter("Polaris-" + requestPayload.getMobileNumber());
                    ftRequestPayload.setAuthorizer("Polaris-" + requestPayload.getMobileNumber());
                    ftRequestPayload.setNoOfAuthorizer("0");
                    ftRequestPayload.setRequestId(genericService.generateTransRef("POLARIS"));
                    ftRequestPayload.setToken(token);
                    ftRequestPayload.setHash(genericService.hashLocalFundsTransferValidationRequest(ftRequestPayload));
                    ftRequestJson = gson.toJson(ftRequestPayload);

                    //Log the request
                    genericService.generateLog("Polaris Bank Stamp Duty Transaction", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
                    //Call the Account Microservice 
                    String stampresponse = ftService.localTransferWithInternal(token, ftRequestJson);
                }

                if (transAmount.compareTo(cbnTxnThreshHoldIndiv) > 0) {
                    amountDifference = transAmount.subtract(cbnTxnThreshHoldIndiv);
                    amountToCharge = amountDifference.multiply(excessIndivChargePercent).divide(percent);
                    ftRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
                    ftRequestPayload.setDebitAccount(requestPayload.getAccountNumber());
                    ftRequestPayload.setCreditAccount("PL52002");
                    ftRequestPayload.setAmount(amountToCharge.toString());
                    ftRequestPayload.setNarration("CBN excess charge");
                    ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
                    ftRequestPayload.setBranchCode("NG0010068");
                    ftRequestPayload.setInputter("Polaris-" + requestPayload.getMobileNumber());
                    ftRequestPayload.setAuthorizer("Polaris-" + requestPayload.getMobileNumber());
                    ftRequestPayload.setNoOfAuthorizer("0");
                    ftRequestPayload.setRequestId(genericService.generateTransRef("POLARIS"));
                    ftRequestPayload.setToken(token);
                    ftRequestPayload.setHash(genericService.hashLocalFundsTransferValidationRequest(ftRequestPayload));
                    ftRequestJson = gson.toJson(ftRequestPayload);

                    //Log the request
                    genericService.generateLog("Polaris Bank Excess Charge Transaction", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
                    //Call the Account Microservice 
                    String excessresponse = ftService.localTransferWithInternal(token, ftRequestJson);
                }

            }

            //Log the error
            genericService.generateLog("Polaris Bank Deposit Transaction", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Polaris Bank Deposit Transaction", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Polaris Bank Deposit Transaction", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateTransactionPayload(String token, TransactionPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getTransRef());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processTransactionQuery(String token, TransactionPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Polaris Bank Transaction Query", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from Polaris Bank
            if (!"POLARIS".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Polaris Bank Transaction Query", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Polaris Bank Transaction Query", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Polaris Bank Transaction Query", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getTransRef(), "Polaris Bank Transaction Query", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload ftRequestPayload = new OmnixRequestPayload();
            ftRequestPayload.setTransRef(requestPayload.getTransRef());
            ftRequestPayload.setRequestId(requestPayload.getRequestId());
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashTransactionQueryRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Polaris Bank Transaction Query", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.transactionQuery(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Polaris Bank Transaction Query", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Polaris Bank Transaction Query", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Polaris Bank Transaction Query", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

}
