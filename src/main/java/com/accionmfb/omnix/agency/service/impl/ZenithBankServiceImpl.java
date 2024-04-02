/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.Account;
import com.accionmfb.omnix.agency.model.FundsTransfer;
import com.accionmfb.omnix.agency.payload.AccountValidation;
import com.accionmfb.omnix.agency.payload.LocalTransferWithChargesPayload;
import com.accionmfb.omnix.agency.payload.OmnixResponsePayload;
import com.accionmfb.omnix.agency.payload.TransactionPosting;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.AccountService;
import com.accionmfb.omnix.agency.service.FundsTransferService;
import com.accionmfb.omnix.agency.service.GenericService;
import com.accionmfb.omnix.agency.service.ZenithBankService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class ZenithBankServiceImpl implements ZenithBankService {

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
    private String ZENITH_BANK_CONTRA_ACCOUNT = "NGN1045100010001";
    XmlMapper xmlMapper;

    ZenithBankServiceImpl() {
        xmlMapper = new XmlMapper();
        xmlMapper.setSerializationInclusion(Include.NON_NULL)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    @Override
    public String getAccountDetails(String token, AccountValidation requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        String requestId = genericService.generateTransRef("ZTH");
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Zenith Bank Account Validation", token, requestJson, "API Request", "INFO", requestId);
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        AccountValidation responseBody = new AccountValidation();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"ZENITH".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Zenith Bank Account Validation", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestId}, Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Zenith Bank Account Validation", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestId, 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestId}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            if (!requestPayload.getBankFiid().trim().equals("0")) {
                responseBody.setAccountStatus("");
                responseBody.setAccountTitle("");
                responseBody.setAcctNo("");
                responseBody.setAvailableBalance("");
                responseBody.setChequeDepositFlag("");
                responseBody.setCurrency("");
                responseBody.setRespCode(4);
                responseBody.setRespMessage("Invalid BankCode");
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);
                //Log the error
                genericService.generateLog("Zenith Bank Account Validation", token, "Bank Code Validation Failed", "API Response", "INFO", requestId);
                return response;
            }

            //Check if the account exist
            Account account = agencyRepository.getAccountUsingAccountNumber(requestPayload.getAcctNo());
            if (account == null) {
                responseBody.setAccountStatus("");
                responseBody.setAccountTitle("");
                responseBody.setAcctNo("");
                responseBody.setAvailableBalance("");
                responseBody.setChequeDepositFlag("");
                responseBody.setCurrency("");
                responseBody.setRespCode(25);
                responseBody.setRespMessage("Unable to locate record");
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);

                //Log the error
                genericService.generateLog("Zenith Bank Account Validation", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAcctNo()}, Locale.ENGLISH), "API Response", "INFO", requestId);
                return response;
            }

            responseBody.setAccountStatus("1"); //Defaulted to 1 as Active
            responseBody.setAccountTitle(account.getCustomer().getLastName() + ", " + account.getCustomer().getOtherName());
            responseBody.setAcctNo(requestPayload.getAcctNo());
            responseBody.setAvailableBalance("0"); //Defaulted to 0 balance
            responseBody.setBranchName(account.getBranch().getBranchName());
            responseBody.setChequeDepositFlag("1");
            responseBody.setCurrency("NGN"); //Defaulted to Nigerian Naira
            responseBody.setRespCode(0);
            responseBody.setRespMessage("Approved or completed successfully");
            response = xmlMapper.writeValueAsString(responseBody);
            response = formatResponse(response);
            //Log the error
            genericService.generateLog("Zenith Bank Account Validation", token, response, "OMNIX Response", "INFO", requestId);
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Zenith Bank Account Validation", token, ex.getMessage(), "API Error", "DEBUG", requestId);

            responseBody.setAccountStatus("");
            responseBody.setAccountTitle("");
            responseBody.setAcctNo("");
            responseBody.setAvailableBalance("");
            responseBody.setChequeDepositFlag("");
            responseBody.setCurrency("");
            responseBody.setRespCode(96);
            responseBody.setRespMessage("System Malfunction");
            try {
                response = xmlMapper.writeValueAsString(responseBody);
            } catch (JsonProcessingException ex1) {
                Logger.getLogger(ZenithBankServiceImpl.class.getName()).log(Level.SEVERE, null, ex1);
            }
            response = formatResponse(response);
            return response;
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
    public String processDepositTransaction(String token, TransactionPosting requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        String requestId = genericService.generateTransRef("ZTH");
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Zenith Bank Deposit Transaction", token, requestJson, "API Request", "INFO", requestId);
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        TransactionPosting responseBody = new TransactionPosting();
        try {
            //Check if the transaction is coming from Zenith Bank
            if (!"ZENITH".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Zenith Bank Deposit Transaction", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestId}, Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Zenith Bank Deposit Transaction", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestId, 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestId}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            if (!requestPayload.getBankFiid().trim().equals("0")) {
                responseBody.setRespCode(4);
                responseBody.setRespMessage("Invalid BankCode");
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);
                //Log the error
                genericService.generateLog("Zenith Bank Deposit Transaction", token, "Bank Code Validation Failed", "API Response", "INFO", requestId);
                return response;
            }

            //Check if the transaction exist in history
            FundsTransfer fundsTransfer = agencyRepository.getFundsTransferUsingRequestId(requestPayload.getTransRefNo());
            if (fundsTransfer != null) {
                responseBody.setRespCode(94);
                responseBody.setRespMessage("Duplicate transaction");
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);
                //Log the error
                genericService.generateLog("Zenith Bank Deposit Transaction", token, "Duplicate Transaction", "API Response", "INFO", requestId);
                return response;
            }

            //Create the request payload to Omnix
            LocalTransferWithChargesPayload ftRequestPayload = new LocalTransferWithChargesPayload();
            ftRequestPayload.setMobileNumber("01234567890");  //This is defaulted because it is not provided
            ftRequestPayload.setDebitAccount(ZENITH_BANK_CONTRA_ACCOUNT);
            ftRequestPayload.setCreditAccount(requestPayload.getCreditAccountNumber());
            ftRequestPayload.setAmount(requestPayload.getAmount());
            ftRequestPayload.setNarration(requestPayload.getNarration());
            ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
            ftRequestPayload.setBranchCode("NG0010068");
            ftRequestPayload.setInputter("Zenith-" + requestPayload.getCreditAccountNumber());
            ftRequestPayload.setAuthorizer("Zenith-" + requestPayload.getCreditAccountNumber());
            ftRequestPayload.setNoOfAuthorizer("0");
            ftRequestPayload.setRequestId(requestId);
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashLocalFundsTransferValidationRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Zenith Bank Deposit Transaction", token, ftRequestJson, "OMNIX Request", "INFO", requestId);
            //Call the Account Microservice 
            response = ftService.localTransferWithInternal(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Zenith Bank Deposit Transaction", token, response, "OMNIX Response", "INFO", requestId);

            OmnixResponsePayload responsePayload = gson.fromJson(response, OmnixResponsePayload.class);
            //Check the response status
            if (responsePayload.getResponseCode().trim().equals("00")) {
                responseBody.setRespCode(0);
                responseBody.setRespMessage("Approved or completed successfully");
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);
                //Log the error
                genericService.generateLog("Zenith Bank Deposit Transaction", token, "Approved or completed successfully", "API Response", "INFO", requestId);
                return response;
            } else {
                responseBody.setRespCode(Integer.valueOf(responsePayload.getResponseCode()));
                responseBody.setRespMessage(responsePayload.getResponseMessage());
                response = xmlMapper.writeValueAsString(responseBody);
                response = formatResponse(response);
                //Log the error
                genericService.generateLog("Zenith Bank Deposit Transaction", token, responsePayload.getResponseMessage(), "API Response", "INFO", requestId);
                return response;
            }
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Zenith Bank Deposit Transaction", token, ex.getMessage(), "API Error", "DEBUG", requestId);

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    private String formatResponse(String stringToFormat) {
        stringToFormat = stringToFormat.replace("acctNo", "AcctNo")
                .replace("availableBalance", "AvailableBalance")
                .replace("accountStatus", "AccountStatus")
                .replace("accountTitle", "AccountTitle")
                .replace("chequeDepositFlag", "ChequeDepositFlag")
                .replace("currency", "Currency")
                .replace("respCode", "RespCode")
                .replace("respMessage", "RespMessage")
                .replace("acctNo", "AcctNo")
                .replace("availableBalance", "AvailableBalance");
        return stringToFormat;
    }
}
