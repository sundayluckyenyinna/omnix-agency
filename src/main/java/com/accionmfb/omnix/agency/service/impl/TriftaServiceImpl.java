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
import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.agency.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.agency.payload.AirtimeOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.BillerRequestPayload;
import com.accionmfb.omnix.agency.payload.CableTVPayload;
import com.accionmfb.omnix.agency.payload.CableTVRequestPayload;
import com.accionmfb.omnix.agency.payload.DataOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityBillerRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityPayload;
import com.accionmfb.omnix.agency.payload.ElectricityRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithoutBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.LocalTransferPayload;
import com.accionmfb.omnix.agency.payload.LocalTransferWithChargesPayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.agency.payload.NIPNameEnquiryPayload;
import com.accionmfb.omnix.agency.payload.NIPTransferPayload;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import com.accionmfb.omnix.agency.payload.OmnixResponsePayload;
import com.accionmfb.omnix.agency.payload.SmartcardRequestPayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.*;
import com.google.gson.Gson;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import java.time.LocalDate;
import java.util.Locale;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
public class TriftaServiceImpl implements TriftaService {

    @Autowired
    ElectricityService electricityService;
    @Autowired
    CableTVService cabletvService;
    @Autowired
    AirtimeService airtimeService;
    @Autowired
    FundsTransferService ftService;
    @Autowired
    CustomerService customerService;
    @Autowired
    AccountService accountService;
    @Autowired
    Gson gson;
    @Autowired
    GenericService genericService;
    @Autowired
    AgencyRepository agencyRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    MessageSource messageSource;
    @Value("${omnix.agency.banking.trifta.account}")
    private String triftaSettlementAccount;

    @Override
    public boolean validateAccountNumberPayload(String token, AccountNumberPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processAccountBalance(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Trifta Account Balance", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Trifta Account Balance", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Trifta Account Balance", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
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
                genericService.generateLog("Trifta Account Balance", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Balance", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!account.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Account Balance", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Balance", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Account Balance", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Trifta Account Balance", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
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
            genericService.generateLog("Trifta Account Opening", token, accRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());

            //Call the Account Microservice 
            response = accountService.accountBalance(token, accRequestJson);
            //Log the error
            genericService.generateLog("Trifta Account Balance", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Trifta Account Balance", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Account Balance", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateAccountStatementPayload(String token, AccountStatementRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getAccountNumber());
        rawString.add(requestPayload.getStartDate());
        rawString.add(requestPayload.getEndDate());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    @HystrixCommand(fallbackMethod = "accountStatementFallback")
    public String getAccountStatement(String token, AccountStatementRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Account Statement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Account Statement", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Statement", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
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
                genericService.generateLog("Trifta Account Statement", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Statement", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!account.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Account Statement", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Statement", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Account Statement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Trifta Account Statement", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload stmtRequestPayload = new OmnixRequestPayload();
            stmtRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            stmtRequestPayload.setAccountNumber(requestPayload.getAccountNumber());
            stmtRequestPayload.setStartDate(requestPayload.getStartDate());
            stmtRequestPayload.setEndDate(requestPayload.getEndDate());
            stmtRequestPayload.setRequestId(requestPayload.getRequestId());
            stmtRequestPayload.setToken(token);
            stmtRequestPayload.setHash(genericService.hashAccountStatementRequest(stmtRequestPayload));
            String accRequestJson = gson.toJson(stmtRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Account Statement", token, accRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());

            //Call the Account Microservice 
            response = accountService.accountStatement(token, accRequestJson);
            //Log the error
            genericService.generateLog("Trifta Account Statement", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Account Statement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @SuppressWarnings("unused")
    public String accountStatementFallback(String token, AccountStatementRequestPayload requestPayload) {
        return messageSource.getMessage("appMessages.fallback.account", new Object[]{LocalDate.now()}, Locale.ENGLISH);
    }

    @Override
    public Object checkIfSameRequestId(String requestId) {
        try {
            Account accountRecord = agencyRepository.getRecordUsingRequestId(requestId);
            return accountRecord == null;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public boolean validateMobileNumberPayload(String token, MobileNumberRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processCustomerDetails(String token, MobileNumberRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Customer Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Customer Details", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Customer Details", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            Customer customer = agencyRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber().trim());
            if (customer == null) {
                //Log the error
                genericService.generateLog("Trifta Customer Details", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Customer Details", "", channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Customer Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload custRequestPayload = new OmnixRequestPayload();
            custRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            custRequestPayload.setRequestId(requestPayload.getRequestId());
            custRequestPayload.setToken(token);
            custRequestPayload.setHash(genericService.hashCustomerDetailsRequest(custRequestPayload));
            String custRequestJson = gson.toJson(custRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Customer Details", token, custRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());

            //Call the Account Microservice 
            response = customerService.customerDetails(token, custRequestJson);
            //Log the error
            genericService.generateLog("Trifta Customer Details", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer Details", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Customer Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String getAccountDetails(String token, AccountNumberPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Account Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Account Details", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Details", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
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
                genericService.generateLog("Trifta Account Details", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Details", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!account.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Account Details", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Details", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getAccountNumber(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Account Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAccountNumber(), "Trifta Account Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
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
            genericService.generateLog("Trifta Account Details", token, accRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());

            //Call the Account Microservice 
            response = accountService.accountDetails(token, accRequestJson);
            //Log the error
            genericService.generateLog("Trifta Account Details", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getAccountNumber(), "Trifta Account Details", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Account Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateAccountOpeningRequestPayload(String token, AccountOpeningRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getCustomerNumber());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processAccountOpening(String token, AccountOpeningRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Account Opening", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Account Opening", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Opening", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Account Opening", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Account Opening", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload accRequestPayload = new OmnixRequestPayload();
            accRequestPayload.setAccountOfficer("7801"); // 1535 Oluwatimilehin Olatunji. Provided by Tokunbo
            accRequestPayload.setBranchCode("NG0010068"); //Defaulted to Digital branch
            accRequestPayload.setCustomerNumber(requestPayload.getCustomerNumber());
            accRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            accRequestPayload.setOtherOfficer("9998"); //Defaulted to Commercial Supervisor Ojodu. Provided by Tokunbo
            accRequestPayload.setProductCode("6006"); //Defaulted to Brighta Purse
            accRequestPayload.setRequestId(requestPayload.getRequestId());
            accRequestPayload.setToken(token);
            accRequestPayload.setHash(genericService.hashAccountOpeningRequest(accRequestPayload));
            String accRequestJson = gson.toJson(accRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Account Opening", token, accRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = accountService.accountOpening(token, accRequestJson);
            //Log the error
            genericService.generateLog("Trifta Account Opening", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Account Opening", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Account Opening", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateCreateIndividualCustomerWithBvnPayload(String token, IndivCustomerWithBvnRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getMaritalStatus().trim());
        rawString.add(requestPayload.getStateOfResidence().trim());
        rawString.add(requestPayload.getCityOfResidence().trim());
        rawString.add(requestPayload.getResidentialAddress().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processCreateIndividaulCustomerWithBvn(String token, IndivCustomerWithBvnRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Customer With BVN", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Customer With BVN", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Customer With BVN", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Customer With BVN", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer With BVN", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload custRequestPayload = new OmnixRequestPayload();
            custRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            custRequestPayload.setBvn(requestPayload.getBvn());
            custRequestPayload.setMaritalStatus(requestPayload.getMaritalStatus());
            custRequestPayload.setBranchCode("NG0010068"); //Defaulted to Digital Branch
            custRequestPayload.setSector("1000");
            custRequestPayload.setStateOfResidence(requestPayload.getStateOfResidence());
            custRequestPayload.setCityOfResidence(requestPayload.getCityOfResidence());
            custRequestPayload.setResidentialAddress(requestPayload.getResidentialAddress());
            custRequestPayload.setAccountOfficer("7801"); //"1535" Defaulted to Oluwatimilehin Olatunji. Provided by Tokunbo
            custRequestPayload.setOtherOfficer("9998"); //2844 Defaulted to Commercial Supervisor Ojodu. Provided by Tokunbo
            custRequestPayload.setRequestId(requestPayload.getRequestId());
            custRequestPayload.setToken(token);
            custRequestPayload.setHash(genericService.hashCustomerWithBvnRequest(custRequestPayload));
            String custRequestJson = gson.toJson(custRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Customer With BVN", token, custRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = customerService.createCustomerWithBvn(token, custRequestJson);
            //Log the error
            genericService.generateLog("Trifta Customer With BVN", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer With BVN", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Customer With BVN", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateCreateIndividualCustomerWithoutBvnPayload(String token, IndivCustomerWithoutBvnRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getLastName().trim());
        rawString.add(requestPayload.getOtherName().trim());
        rawString.add(requestPayload.getDob().trim());
        rawString.add(requestPayload.getGender().trim());
        rawString.add(requestPayload.getMaritalStatus().trim());
        rawString.add(requestPayload.getStateOfResidence().trim());
        rawString.add(requestPayload.getCityOfResidence().trim());
        rawString.add(requestPayload.getResidentialAddress().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processCreateIndividualCustomerWithoutBvn(String token, IndivCustomerWithoutBvnRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Customer Without BVN", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Customer Without BVN", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Customer Without BVN", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Customer Without BVN", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer Without BVN", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload custRequestPayload = new OmnixRequestPayload();
            custRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            custRequestPayload.setLastName(requestPayload.getLastName());
            custRequestPayload.setOtherName(requestPayload.getOtherName());
            custRequestPayload.setDob(requestPayload.getDob());
            custRequestPayload.setGender(requestPayload.getGender());
            custRequestPayload.setMaritalStatus(requestPayload.getMaritalStatus());
            custRequestPayload.setBranchCode("NG0010068");
            custRequestPayload.setSector("1000");
            custRequestPayload.setStateOfResidence(requestPayload.getStateOfResidence());
            custRequestPayload.setCityOfResidence(requestPayload.getCityOfResidence());
            custRequestPayload.setResidentialAddress(requestPayload.getResidentialAddress());
            custRequestPayload.setAccountOfficer("7801"); //1535 Defaulted to Oluwatimilehin Olatunji. Provided by Tokunbo
            custRequestPayload.setOtherOfficer("9998"); //2844 Defaulted to Commercial Supervisor Ojodu
            custRequestPayload.setRequestId(requestPayload.getRequestId());
            custRequestPayload.setToken(token);
            custRequestPayload.setHash(genericService.hashCustomerWithoutBvnRequest(custRequestPayload));
            String custRequestJson = gson.toJson(custRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Customer Without BVN", token, custRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = customerService.createCustomerWithoutBvn(token, custRequestJson);
            //Log the error
            genericService.generateLog("Trifta Customer Without BVN", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Customer Without BVN", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Customer Without BVN", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateLocalFundsTransferPayload(String token, LocalTransferPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processLocalFundsTransfer(String token, LocalTransferPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Funds Transfer", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the debit account is from Trifta
            Account creditAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getCreditAccount());
            if (creditAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getCreditAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getCreditAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getCreditAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!creditAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getCreditAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Funds Transfer", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getCreditAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getCreditAccount(), 'F');
                return response;
            }

            //Create the request payload
            LocalTransferWithChargesPayload ftRequestPayload = new LocalTransferWithChargesPayload();
            ftRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            ftRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            ftRequestPayload.setCreditAccount(requestPayload.getCreditAccount());
            ftRequestPayload.setAmount(requestPayload.getAmount());
            ftRequestPayload.setNarration(requestPayload.getNarration());
            ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
            ftRequestPayload.setBranchCode("NG0010068");
            ftRequestPayload.setInputter("Trifta-" + requestPayload.getMobileNumber());
            ftRequestPayload.setAuthorizer("Trifta-" + requestPayload.getMobileNumber());
            ftRequestPayload.setNoOfAuthorizer("0");
            ftRequestPayload.setRequestId(requestPayload.getRequestId());
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashLocalFundsTransferValidationRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Funds Transfer", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.localTransfer(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Trifta Funds Transfer", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Funds Transfer", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Funds Transfer", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

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
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processTransactionQuery(String token, TransactionPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Transaction Query", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Transaction Query", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cashout Notification", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Transaction Query", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Transaction Query", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
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
            genericService.generateLog("Trifta Transaction Query", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.transactionQuery(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Trifta Transaction Query", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Transaction Query", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Transaction Query", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String processLocalFundsTransferReversal(String token, TransactionPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Funds Transfer Reversal", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Funds Transfer Reversal", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer Reversal", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Funds Transfer Reversal", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Funds Transfer Reversal", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
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
            genericService.generateLog("Trifta Funds Transfer Reversal", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.transactionReversal(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Trifta Funds Transfer Reversal", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Funds Transfer Reversal", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Funds Transfer Reversal", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateNIPTransferPayload(String token, NIPTransferPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getBeneficiaryAccount().trim());
        rawString.add(requestPayload.getBeneficiaryAccountName());
        rawString.add(requestPayload.getBeneficiaryBankCode().trim());
        rawString.add(requestPayload.getBeneficiaryKycLevel().trim());
        rawString.add(requestPayload.getBeneficiaryBvn().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processNIPTransfer(String token, NIPTransferPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta NIP", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta NIP", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta NIP", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta NIP", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta NIP", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Create the request payload
            OmnixRequestPayload ftRequestPayload = new OmnixRequestPayload();
            ftRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            ftRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            ftRequestPayload.setBeneficiaryAccount(requestPayload.getBeneficiaryAccount());
            ftRequestPayload.setBeneficiaryAccountName(requestPayload.getBeneficiaryAccountName());
            ftRequestPayload.setBeneficiaryBankCode(requestPayload.getBeneficiaryBankCode());
            ftRequestPayload.setBeneficiaryKycLevel(requestPayload.getBeneficiaryKycLevel());
            ftRequestPayload.setBeneficiaryBvn(requestPayload.getBeneficiaryBvn());
            ftRequestPayload.setAmount(requestPayload.getAmount());
            ftRequestPayload.setNarration(requestPayload.getNarration());
            ftRequestPayload.setRequestId(requestPayload.getRequestId());
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashNIPValidationRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Trifta NIP", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.nipTransfer(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Trifta NIP", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Trifta Funds Transfer", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta NIP", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateNIPNameEnquiryPayload(String token, NIPNameEnquiryPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBeneficiaryAccount().trim());
        rawString.add(requestPayload.getBeneficiaryBankCode().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processNIPNameEnquiry(String token, NIPNameEnquiryPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta NIP Name Enquiry", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta NIP Name Enquiry", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP Name Enquiry", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta NIP Name Enquiry", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta NIP Name Enquiry", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload ftRequestPayload = new OmnixRequestPayload();
            ftRequestPayload.setBeneficiaryAccount(requestPayload.getBeneficiaryAccount());
            ftRequestPayload.setBeneficiaryBankCode(requestPayload.getBeneficiaryBankCode());
            ftRequestPayload.setRequestId(requestPayload.getRequestId());
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash(genericService.hashNIPNameEnquiryRequest(ftRequestPayload));
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request
            genericService.generateLog("Trifta NIP Name Enquiry", token, ftRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = ftService.nipNameEnquiry(token, ftRequestJson);
            //Log the error
            genericService.generateLog("Trifta NIP Name Enquiry", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta NIP Name Enquiry", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta NIP Name Enquiry", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateAirtimeOthersPayload(String token, AirtimeOtherRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getThirdPartyMobileNumber().trim());
        rawString.add(requestPayload.getThirdPartyTelco());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processAirtime(String token, AirtimeOtherRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Airtime", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Airtime", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Airtime", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Airtime", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Airtime", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Airtime", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Airtime", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Airtime", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Airtime", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload airtimeRequestPayload = new OmnixRequestPayload();
            airtimeRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            airtimeRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            airtimeRequestPayload.setThirdPartyMobileNumber(requestPayload.getThirdPartyMobileNumber());
            airtimeRequestPayload.setThirdPartyTelco(requestPayload.getThirdPartyTelco());
            airtimeRequestPayload.setAmount(requestPayload.getAmount());
            airtimeRequestPayload.setRequestId(requestPayload.getRequestId());
            airtimeRequestPayload.setToken(token);
            airtimeRequestPayload.setHash(genericService.hashAirtimeValidationRequest(airtimeRequestPayload));
            String airtimeRequestJson = gson.toJson(airtimeRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Airtime", token, airtimeRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = airtimeService.airtimeOthers(token, airtimeRequestJson);
            //Log the error
            genericService.generateLog("Trifta Airtime", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Airtime", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Airtime", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateDataOthersPayload(String token, DataOtherRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getThirdPartyMobileNumber().trim());
        rawString.add(requestPayload.getThirdPartyTelco().trim());
        rawString.add(requestPayload.getDataPlanId());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processData(String token, DataOtherRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Data", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Data", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Data", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Data", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Data", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Data", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Data", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Data", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Data", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload dataRequestPayload = new OmnixRequestPayload();
            dataRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            dataRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            dataRequestPayload.setThirdPartyMobileNumber(requestPayload.getThirdPartyMobileNumber());
            dataRequestPayload.setThirdPartyTelco(requestPayload.getThirdPartyTelco());
            dataRequestPayload.setDataPlanId(requestPayload.getDataPlanId());
            dataRequestPayload.setRequestId(requestPayload.getRequestId());
            dataRequestPayload.setToken(token);
            dataRequestPayload.setHash(genericService.hashDataValidationRequest(dataRequestPayload));
            String dataRequestJson = gson.toJson(dataRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Data", token, dataRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = airtimeService.dataOthers(token, dataRequestJson);
            //Log the error
            genericService.generateLog("Trifta Data", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Data", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Data", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateCableTVPayload(String token, CableTVRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getSmartCard().trim());
        rawString.add(requestPayload.getBillerId().trim());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processCableTVSubscription(String token, CableTVRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Cable TV", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Cable TV", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Cable TV", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Cable TV", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Cable TV", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload cableRequestPayload = new OmnixRequestPayload();
            cableRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            cableRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            cableRequestPayload.setSmartCard(requestPayload.getSmartCard());
            cableRequestPayload.setBillerId(requestPayload.getBillerId());
            cableRequestPayload.setRequestId(requestPayload.getRequestId());
            cableRequestPayload.setToken(token);
            cableRequestPayload.setHash(genericService.hashCableTVValidationRequest(cableRequestPayload));
            String cableRequestJson = gson.toJson(cableRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Cable TV", token, cableRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = cabletvService.cableSubscription(token, cableRequestJson);
            //Log the error
            genericService.generateLog("Trifta Cable TV", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Cable TV", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Cable TV", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateCableTVPayload(String token, CableTVPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getTransRef().trim());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String getCableTVDetails(String token, CableTVPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Cable TV Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Details", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV Details", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Account Statement", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload cableRequestPayload = new OmnixRequestPayload();
            cableRequestPayload.setTransRef(requestPayload.getTransRef());
            cableRequestPayload.setRequestId(requestPayload.getRequestId());
            cableRequestPayload.setToken(token);
            cableRequestPayload.setHash(genericService.hashCableTVTransRefValidationRequest(cableRequestPayload));
            String cableRequestJson = gson.toJson(cableRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Cable TV Details", token, cableRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = cabletvService.cableTVDetails(token, cableRequestJson);
            //Log the error
            genericService.generateLog("Trifta Cable TV Details", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Cable TV Details", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Cable TV Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateBillerPayload(String token, BillerRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBiller());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String getCableTVBiller(String token, BillerRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Cable TV Billers", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Billers", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV Billers", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Billers", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV Billers", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload cableRequestPayload = new OmnixRequestPayload();
            cableRequestPayload.setBiller(requestPayload.getBiller());
            cableRequestPayload.setRequestId(requestPayload.getRequestId());
            cableRequestPayload.setToken(token);
            cableRequestPayload.setHash(genericService.hashCableTVBillerValidationRequest(cableRequestPayload));
            String cableRequestJson = gson.toJson(cableRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Cable TV Billers", token, cableRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = cabletvService.cableTVBillers(token, cableRequestJson);
            //Log the error
            genericService.generateLog("Trifta Cable TV Billers", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Cable TV Billers", "", channel, "", requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Cable TV Billers", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateSmartcardPayload(String token, SmartcardRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getSmartcard());
        rawString.add(requestPayload.getBiller());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String getCableTVSmartcardDetails(String token, SmartcardRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV Smartcard Lookup", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Cable TV Smartcard Lookup", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload cableRequestPayload = new OmnixRequestPayload();
            cableRequestPayload.setSmartCard(requestPayload.getBiller());
            cableRequestPayload.setBiller(requestPayload.getBiller());
            cableRequestPayload.setRequestId(requestPayload.getRequestId());
            cableRequestPayload.setToken(token);
            cableRequestPayload.setHash(genericService.hashCableTVBillerValidationRequest(cableRequestPayload));
            String cableRequestJson = gson.toJson(cableRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, cableRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = cabletvService.cableTVSmartcardLookup(token, cableRequestJson);
            //Log the error
            genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Cable TV Smartcard Lookup", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Cable TV Smartcard Lookup", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateElectricityPayload(String token, ElectricityRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getMeterNumber().trim());
        rawString.add(requestPayload.getBillerId().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String processElectricityPayment(String token, ElectricityRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Electricity", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Electricity", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Electricity", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the debit account is from Trifta
            Account debitAccount = agencyRepository.getAccountUsingAccountNumber(requestPayload.getDebitAccount());
            if (debitAccount == null) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Electricity", token, messageSource.getMessage("appMessages.account.notexist", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity", "", channel, messageSource.getMessage("appMessages.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Check if the account was opened by Trifta
            if (!debitAccount.getAppUser().getUsername().equalsIgnoreCase("TRIFTA")) {
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH));
                response = gson.toJson(errorResponse);

                //Log the error
                genericService.generateLog("Trifta Electricity", token, messageSource.getMessage("appMessages.account.nottrifta", new Object[]{requestPayload.getDebitAccount()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity", "", channel, messageSource.getMessage("appMessages.account.nottrifta", new Object[0], Locale.ENGLISH), requestPayload.getDebitAccount(), 'F');
                return response;
            }

            //Create the request payload
            OmnixRequestPayload electRequestPayload = new OmnixRequestPayload();
            electRequestPayload.setMobileNumber(requestPayload.getMobileNumber());
            electRequestPayload.setDebitAccount(requestPayload.getDebitAccount());
            electRequestPayload.setMeterNumber(requestPayload.getMeterNumber());
            electRequestPayload.setBillerId(requestPayload.getBillerId());
            electRequestPayload.setAmount(requestPayload.getAmount());
            electRequestPayload.setRequestId(requestPayload.getRequestId());
            electRequestPayload.setToken(token);
            electRequestPayload.setHash(genericService.hashElectricityValidationRequest(electRequestPayload));
            String electricityRequestJson = gson.toJson(electRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Electricity", token, electricityRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = electricityService.electricityPayment(token, electricityRequestJson);
            //Log the error
            genericService.generateLog("Trifta Electricity", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Electricity", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Electricity", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateElectricityPayload(String token, ElectricityPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getTransRef().trim());
        rawString.add(requestPayload.getRequestId().trim());
        String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String getElectricityDetails(String token, ElectricityPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Electroicity Details", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Electricity Details", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Details", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Electricity Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload electRequestPayload = new OmnixRequestPayload();
            electRequestPayload.setTransRef(requestPayload.getTransRef());
            electRequestPayload.setRequestId(requestPayload.getRequestId());
            electRequestPayload.setToken(token);
            electRequestPayload.setHash(genericService.hashElectricityDetailsValidationRequest(electRequestPayload));
            String electricityRequestJson = gson.toJson(electRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Electricity Details", token, electricityRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = electricityService.electricityDetails(token, electricityRequestJson);
            //Log the error
            genericService.generateLog("Trifta Electricity Details", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Electricity Details", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Electricity Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public boolean validateBillerPayload(String token, ElectricityBillerRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBiller());
        rawString.add(requestPayload.getRequestId().trim());
         String encryptedString = genericService.encryptTriftaString(rawString.toString(), encryptionKey);
        return encryptedString.equalsIgnoreCase(requestPayload.getHash());
    }

    @Override
    public String getElectricityBiller(String token, ElectricityBillerRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Electricity Billers", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Electricity Billers", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Billers", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Electricity Billers", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Billers", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload electRequestPayload = new OmnixRequestPayload();
            electRequestPayload.setBiller(requestPayload.getBiller());
            electRequestPayload.setRequestId(requestPayload.getRequestId());
            electRequestPayload.setToken(token);
            electRequestPayload.setHash(genericService.hashElectricityBillersValidationRequest(electRequestPayload));
            String electricityRequestJson = gson.toJson(electRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Electricity Billers", token, electricityRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = electricityService.electricityBillers(token, electricityRequestJson);
            //Log the error
            genericService.generateLog("Trifta Electricity Billers", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Electricity Billers", "", channel, "", requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Electricity Billers", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String getElectricitySmartcardDetails(String token, SmartcardRequestPayload requestPayload) {
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String response = "";
        //Create request log
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Trifta Electricity Smartcard Lookup", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        try {
            //Check if the transaction is coming from TRIFTA
            if (!"TRIFTA".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Trifta Electricity Smartcard Lookup", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Smartcard Lookup", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Trifta Electricity Smartcard Lookup", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Trifta Electricity Smartcard Lookup", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Create the request payload
            OmnixRequestPayload electRequestPayload = new OmnixRequestPayload();
            electRequestPayload.setSmartCard(requestPayload.getSmartcard());
            electRequestPayload.setBiller(requestPayload.getBiller());
            electRequestPayload.setRequestId(requestPayload.getRequestId());
            electRequestPayload.setToken(token);
            electRequestPayload.setHash(genericService.hashElectricityBillerValidationRequest(electRequestPayload));
            String electricityRequestJson = gson.toJson(electRequestPayload);

            //Log the request
            genericService.generateLog("Trifta Electricity Smartcard Lookup", token, electricityRequestJson, "OMNIX Request", "INFO", requestPayload.getRequestId());
            //Call the Account Microservice 
            response = cabletvService.cableTVSmartcardLookup(token, electricityRequestJson);
            //Log the error
            genericService.generateLog("Trifta Electricity Smartcard Lookup", token, response, "OMNIX Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity("", "Trifta Electricity Smartcard Lookup", "", channel, response, requestBy, 'S');
            return response;
        } catch (Exception ex) {
            //Log the error
            genericService.generateLog("Trifta Electricity Smartcard Lookup", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

}
