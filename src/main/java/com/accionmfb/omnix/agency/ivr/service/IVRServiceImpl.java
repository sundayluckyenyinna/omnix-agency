package com.accionmfb.omnix.agency.ivr.service;

import com.accionmfb.omnix.agency.ivr.payload.BVNPayload;
import com.accionmfb.omnix.agency.ivr.payload.IVRResponsePayload;
import com.accionmfb.omnix.agency.ivr.payload.RequestPayload;
import com.accionmfb.omnix.agency.ivr.payload.ResponsePayload;
import com.accionmfb.omnix.agency.ivr.repository.BVNRepository;
import com.accionmfb.omnix.agency.ivr.repository.CustomerAccountRepository;
import com.accionmfb.omnix.agency.ivr.repository.IVRRepository;
import com.accionmfb.omnix.agency.model.ivr.*;
import com.accionmfb.omnix.agency.payload.AccountBalanceResponsePayload;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import com.accionmfb.omnix.agency.service.AccountService;
import com.accionmfb.omnix.agency.service.GenericService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Olaoye on 30/10/2023
 */
@Service
@RequiredArgsConstructor
public class IVRServiceImpl implements IVRService {

    private static String AUTHORIZATION = "";
    private static String TIMESTAMP = "";
    private final Gson gson;
    private final RestTemplate restTemplate;
    private final Environment env;
    private final CustomerAccountRepository custAccountRepositoy;
    private final IVRGenericService ivrGenericService;
    private final BVNRepository bvnRepository;
    private final MessageSource messageSource;
    private final IVRRepository ivrRepository;
    private final AccountService accountService;
    private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();
    private static final Logger LOGGER = Logger.getLogger(IVRServiceImpl.class.getName());

    @Override
    public Boolean checkRequestHeaderValidity(String authorization) {
        //Check if the request header is valid
        AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((env.getProperty("ivr.basic.authorization.username").trim() + ":" + env.getProperty("ivr.basic.authorization.password").trim()).getBytes());
        return AUTHORIZATION.equals(authorization);

    }

    @Override
    public Boolean typeValidation(String requestJson) {
        int errors = 0;

        RequestPayload requestPayload = new RequestPayload();
        requestPayload = gson.fromJson(requestJson, RequestPayload.class);

        //Check if the supplied parameters are correct
        List<String> requestType = Arrays.asList("ACCOUNT BALANCE", "ACCOUNT OPENING", "BLOCK ACCOUNT");
        List<String> input = Arrays.asList("ACCOUNT NUMBER", "BVN", "MOBILE NUMBER", "AUTHENTICATION", "PIN");
        if (!requestType.contains(requestPayload.getRequestType().trim().toUpperCase())) {
            errors++;
        }

        if (!input.contains(requestPayload.getInputType().trim().toUpperCase())) {
            errors++;
        }
        return errors == 0;
    }

    @Override
    public String ivrRequest(String authorization, String requestJson, String requestSource, String remoteIP, String sessionId) {
        String failedCode = env.getProperty("api.webservice.transaction.failed.error.code").trim();
        String noRecordCode = env.getProperty("api.webservice.record.not.found.error.code").trim();
        String fieldNotSupplied = env.getProperty("api.webservice.field.not.supplied").trim();
        String successCode = env.getProperty("api.webservice.success.code").trim();
        ResponsePayload failedResponse = new ResponsePayload();

        IVRResponsePayload responsePayload = new IVRResponsePayload();
        String response = "";
        try {
            RequestPayload request = new RequestPayload();
            request = gson.fromJson(requestJson, RequestPayload.class);
            LOGGER.log(Level.INFO, request.getMobileNumber());

            //Account Opening Request
            LOGGER.log(Level.INFO, "IVR Request ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : REQUEST ").concat(requestJson));
            if (request.getRequestType().equalsIgnoreCase("Account Opening")) {
                T24Customers mobile = custAccountRepositoy.getCustomerDetails(request.getMobileNumber());
                BVNIVR bvn = bvnRepository.getBVN(request.getBvn());

                //validate the input type for account opening
                if (request.getInputType().equalsIgnoreCase("Mobile Number")) {
                    //check length of mobile number supplied
                    if (!request.getMobileNumber().matches("[0-9]{11}")) {
                        failedResponse.setResponseCode(failedCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.invalidLength", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                    //check that all request field is supplied
                    if (request.getMobileNumber() == null || request.getMobileNumber().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    if (request.getMobileNumber() != null || !request.getMobileNumber().equalsIgnoreCase("")) {
                        if (mobile != null) {
                            //check if save brighta account exist
                            T24Accounts account = custAccountRepositoy.getT24AccountUsingPhoneNumberAndProductCategory(request.getMobileNumber(), "Save Brighta");
                            if (account != null) {
                                //Return account exist
                                failedResponse.setResponseCode(successCode);
                                failedResponse.setResponseMessage(messageSource.getMessage("appMessages.saveBrightaAccountExist", new Object[0], Locale.ENGLISH));
                                String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                                LOGGER.log(Level.INFO, "Failed Response ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                                return failedResponseJson;
                            }
                            responsePayload.setMobileNumber(request.getMobileNumber());
                            responsePayload.setResponseCode(noRecordCode);
                            responsePayload.setResponseDescription(messageSource.getMessage("appMessages.recordNotFound", new Object[0], Locale.ENGLISH));
                            response = gson.toJson(responsePayload, IVRResponsePayload.class);
                            LOGGER.log(Level.INFO, "Mobile Number Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                        }
                        if (mobile == null) {
                            responsePayload.setMobileNumber(request.getMobileNumber());
                            responsePayload.setResponseCode(noRecordCode);
                            responsePayload.setResponseDescription(messageSource.getMessage("appMessages.recordNotFound", new Object[0], Locale.ENGLISH));
                            response = gson.toJson(responsePayload, IVRResponsePayload.class);
                            LOGGER.log(Level.INFO, "Mobile Number Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                        }
                    }
                }

                //validate the input type for Bvn Request
                if (request.getInputType().equalsIgnoreCase("BVN")) {
                    if (!request.getBvn().matches("[0-9]{11}")) {
                        failedResponse.setResponseCode(failedCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.invalidLength", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    //check that all request filed is supplied
                    if (request.getMobileNumber() == null || request.getMobileNumber().equalsIgnoreCase("")
                            || request.getBvn().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    //check if savebrighta account exist with bvn
                    T24Accounts account = custAccountRepositoy.getT24AccountUsingBVNAndProductCategory(request.getBvn(), "Save Brighta");
                    if (account != null) {
                        //Return account exist
                        failedResponse.setResponseCode(failedCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.saveBrightaAccountExist", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    if (bvn != null) {
                        BVNPayload details = new BVNPayload();
                        details.setFirstName(bvn.getFirstName());
                        details.setLastName(bvn.getLastName());
                        details.setMiddleName(bvn.getMiddleName());
                        details.setGender(bvn.getGender());
                        details.setDob(bvn.getDateOfBirth());

                        //check that bvn is valid
                        responsePayload.setMobileNumber(request.getMobileNumber());
                        responsePayload.setBvn(details);
                        responsePayload.setResponseCode(successCode);
                        responsePayload.setResponseDescription(messageSource.getMessage("appMessages.nextInput", new Object[0], Locale.ENGLISH));
                        response = gson.toJson(responsePayload, IVRResponsePayload.class);
                        LOGGER.log(Level.INFO, "BVN Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                        return response;
                    }

                    //retrieve bvn details from third party if not in db
                    if (bvn == null) {
                        BVNPayload bvnPayload = new BVNPayload();
                        String url = env.getProperty("ussd.webservice.bvn.url").trim();
                        String requestPayload = "{" + "\"bvn\" : \"".concat(request.getBvn().trim()).concat("\"") + "}";
                        HttpHeaders header = new HttpHeaders();
                        header.setContentType(MediaType.APPLICATION_JSON);
                        header.setBasicAuth(env.getProperty("bvn.authorization.username").trim(), env.getProperty("bvn.authorization.password").trim());
                        HttpEntity<String> requestEntity = new HttpEntity<>(requestPayload, header);
                        String resultJson = restTemplate.postForObject(url, requestEntity, String.class);
                        bvnPayload = gson.fromJson(resultJson, BVNPayload.class);

                        BVNPayload details = new BVNPayload();
                        details.setFirstName(bvnPayload.getFirstName());
                        details.setLastName(bvnPayload.getLastName());
                        details.setMiddleName(bvnPayload.getMiddleName());
                        details.setGender(bvnPayload.getGender());
                        details.setDob(bvnPayload.getDob());

                        if (bvnPayload.getResponseCode() != null) {
                            failedResponse.setResponseCode(noRecordCode);
                            failedResponse.setResponseMessage(messageSource.getMessage("appMessages.recordNotFound", new Object[0], Locale.ENGLISH));
                            String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                            LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                            return failedResponseJson;
                        }
                        //return valid message
                        responsePayload.setMobileNumber(request.getMobileNumber());
                        responsePayload.setBvn(details);
                        responsePayload.setResponseCode(successCode);
                        responsePayload.setResponseDescription(messageSource.getMessage("appMessages.nextInput", new Object[0], Locale.ENGLISH));
                        response = gson.toJson(responsePayload, IVRResponsePayload.class);
                        LOGGER.log(Level.INFO, "BVN Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                        return response;
                    }

                }

                //validate moblie number with bvn number
                if (request.getInputType().equalsIgnoreCase("Authentication")) {
                    if (request.getMobileNumber() == null || request.getMobileNumber().equalsIgnoreCase("")
                            || request.getBvn().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                    if (!bvn.getMobileNumber().equalsIgnoreCase(request.getMobileNumber())) {
                        failedResponse.setResponseCode(failedCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.bvnMismatch", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    responsePayload.setResponseCode(successCode);
                    responsePayload.setResponseDescription(messageSource.getMessage("appMessages.nextInput", new Object[0], Locale.ENGLISH));
                    response = gson.toJson(responsePayload, IVRResponsePayload.class);
                    LOGGER.log(Level.INFO, "BVN Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                    return response;
                }

                //validate the request type for Pin Request
                if (request.getInputType().equalsIgnoreCase("Pin")) {
                    if (request.getMobileNumber() == null || request.getBvn() == null || request.getPin() == null
                            || request.getMobileNumber().equalsIgnoreCase("") || request.getBvn().equalsIgnoreCase("") || request.getPin().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    if (!request.getPin().matches("[0-9]{4}")) {
                        failedResponse.setResponseCode(failedCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.invalidPINLength", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    IVRCustomer cust = ivrRepository.getT24CustomerUsingPhoneNumber(request.getMobileNumber());
                    if (cust == null || cust.getStatus().equalsIgnoreCase("Failed")) {
                        IVRCustomer newCustomer = new IVRCustomer();
                        newCustomer.setAccountNumber("");
                        newCustomer.setBranchCode("");
                        newCustomer.setCreatedAt(LocalDateTime.now());
                        newCustomer.setCreatedBy(request.getMobileNumber());
                        newCustomer.setCustomerNumber("");
                        newCustomer.setCustomerType("Customer");
                        newCustomer.setDateOfBirth("");
                        newCustomer.setDailyLimit(env.getProperty("daily.limit").trim());
                        newCustomer.setFirstName(bvn.getLastName());
                        newCustomer.setGender(bvn.getFirstName());
                        newCustomer.setLastName(bvn.getLastName());
                        newCustomer.setMobileNumber(request.getMobileNumber());
                        newCustomer.setPin(ivrGenericService.encryptText(request.getPin()));
                        newCustomer.setStatus("Pending");
                        newCustomer.setTelco("");
                        newCustomer.setSecurityQuestion("");
                        newCustomer.setSecurityAnswer("");
                        newCustomer.setTransactionLimit(env.getProperty("trans.limit").trim());
                        newCustomer.setUnboardAt("1900-01-01");
                        newCustomer.setSessionId("");
                        newCustomer.setTimePeriod(ivrGenericService.getTimePeriod());
                        IVRCustomer createdCustomer = ivrRepository.createCustomer(newCustomer);
                    }
                    if (cust != null && cust.getStatus().equalsIgnoreCase("Failed")) {
                        cust.setPin(ivrGenericService.encryptText(request.getPin()));
                        cust.setStatus("Pending");
                    }

                    //Check if the user is a customer of Accion
                    if (mobile != null) {
                        String result = accountOpening(request.getMobileNumber());
                        return result;
                    } else {
                        String result = createCustomer(request.getBvn());
                        return result;
                    }

                }
            }

            //Account Balance Api Request
            if (request.getRequestType().equalsIgnoreCase("Account Balance")) {
                //check length of mobile number supplied
                String mobileNumber = request.getMobileNumber();
                if (mobileNumber.startsWith("234")) {
                    mobileNumber = mobileNumber.replaceFirst("234", "0");
                }
                if (mobileNumber.startsWith("00")) {
                    mobileNumber = mobileNumber.replaceFirst("00", "0");
                }

                IVRCustomer cust = ivrRepository.getT24CustomerUsingPhoneNumber(mobileNumber);
                //validate the input type mobile number
                if (request.getInputType().equalsIgnoreCase("Mobile Number")) {
                    if (mobileNumber == null || request.getMobileNumber().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Account Balance Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }

                    if (cust != null) {
                        // is account valid
                        if (cust.getAccountNumber() != null || !cust.getAccountNumber().equalsIgnoreCase("")) {
                            responsePayload.setResponseCode(successCode);
                            responsePayload.setResponseDescription(messageSource.getMessage("appMessages.nextInput", new Object[0], Locale.ENGLISH));
                            response = gson.toJson(responsePayload, IVRResponsePayload.class);
                            LOGGER.log(Level.INFO, "Account Validation Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                            return response;
                        }
                    } else {
                        failedResponse.setResponseCode(noRecordCode);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.recordNotFound", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Account Balance Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                }

                // validate the input type for pin
                if (request.getInputType().equalsIgnoreCase("Pin")) {
                    if (mobileNumber == null || request.getPin() == null
                            || mobileNumber.equalsIgnoreCase("") || request.getPin().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Account Balance Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                    if (request.getPin() != null
                            || !mobileNumber.equalsIgnoreCase("") || !request.getPin().equalsIgnoreCase("")) {

                        IVRCustomer customerDetail = ivrRepository.getT24CustomerUsingPhoneNumber(mobileNumber);
                        //Check if the PIN is a match
                        Boolean pinMatch = bCryptEncoder.matches(request.getPin(), customerDetail.getPin().trim());
                        pinMatch = true;
                        if (!pinMatch) {
                            //PIN mismatch. Generate error message
                            failedResponse.setResponseCode(noRecordCode);
                            failedResponse.setResponseMessage(messageSource.getMessage("appMessages.invalidPIN", new Object[0], Locale.ENGLISH));
                            String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                            LOGGER.log(Level.INFO, "Pin Validation Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                            return failedResponseJson;
                        } else {
                            // Get Account Balance
                            DecimalFormat nf = new DecimalFormat("0.00");
                            nf.setMaximumFractionDigits(2);
                            nf.setRoundingMode(RoundingMode.CEILING);

                            OmnixRequestPayload drAccBalRequest = new OmnixRequestPayload();
                            drAccBalRequest.setAccountNumber(request.getAccountNumber());
                            drAccBalRequest.setRequestId(request.getTransId());
                            drAccBalRequest.setToken(authorization);
                            drAccBalRequest.setHash(ivrGenericService.hashAccountBalanceRequest(drAccBalRequest));
                            String drAccBalRequestJson = gson.toJson(drAccBalRequest);

                            String accountBalance = accountService.accountBalance(authorization, drAccBalRequestJson);
                            AccountBalanceResponsePayload drAccBalResponse = gson.fromJson(accountBalance, AccountBalanceResponsePayload.class);

                            IVRResponsePayload balanceResponse = new IVRResponsePayload();
                            Float amount = Float.parseFloat(drAccBalResponse.getAvailableBalance());
                            balanceResponse.setResponseCode(successCode);
                            balanceResponse.setResponseDescription(messageSource.getMessage("appMessages.operationSuccessful", new Object[0], Locale.ENGLISH));
                            balanceResponse.setAvailableBalance(nf.format(amount));
                            balanceResponse.setAccountNumber(balanceResponse.getAccountNumber());
                            String balance = gson.toJson(balanceResponse, IVRResponsePayload.class);
                            LOGGER.log(Level.INFO, "Account Balance Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(balance));
                            return balance;
                        }
                    }
                }
            }

            //Block Account Api request type
            if (request.getRequestType().equalsIgnoreCase("Block Account")) {
                T24Accounts account = custAccountRepositoy.getT24AccountUsingAccountNumber(request.getAccountNumber());
                //validate the input type account number
                if (request.getInputType().equalsIgnoreCase("Account Number")) {
                    if (request.getAccountNumber() == null || request.getAccountNumber().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Account Validation Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                    if (request.getAccountNumber() != null || !request.getAccountNumber().equalsIgnoreCase("")) {
                        if (account == null) {
                            failedResponse.setResponseCode(noRecordCode);
                            failedResponse.setResponseMessage(messageSource.getMessage("appMessages.recordNotFound", new Object[0], Locale.ENGLISH));
                            String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                            LOGGER.log(Level.INFO, "Account Validation Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                            return failedResponseJson;
                        } else {
                            responsePayload.setResponseCode(successCode);
                            responsePayload.setResponseDescription(messageSource.getMessage("appMessages.nextInput", new Object[0], Locale.ENGLISH));
                            response = gson.toJson(responsePayload, IVRResponsePayload.class);
                            LOGGER.log(Level.INFO, "Account Validation Response ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                            return response;
                        }
                    }
                }

                //validate the input type for pin
                if (request.getInputType().equalsIgnoreCase("Pin")) {
                    if (request.getAccountNumber() == null || request.getPin() == null
                            || request.getAccountNumber().equalsIgnoreCase("") || request.getPin().equalsIgnoreCase("")) {
                        failedResponse.setResponseCode(fieldNotSupplied);
                        failedResponse.setResponseMessage(messageSource.getMessage("appMessages.missingRequiredField", new Object[0], Locale.ENGLISH));
                        String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Block Account Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                        return failedResponseJson;
                    }
                    if (request.getAccountNumber() != null || request.getPin() != null
                            || !request.getAccountNumber().equalsIgnoreCase("") || !request.getPin().equalsIgnoreCase("")) {

                        IVRCustomer customerDetail = ivrRepository.getCustomerUsingAccountNumber(request.getAccountNumber());
                        //Check if the PIN is a match
                        Boolean pinMatch = bCryptEncoder.matches(request.getPin(), customerDetail.getPin().trim());
                        if (!pinMatch) {
                            //PIN mismatch. Generate error message
                            failedResponse.setResponseCode(noRecordCode);
                            failedResponse.setResponseMessage(messageSource.getMessage("appMessages.invalidPIN", new Object[0], Locale.ENGLISH));
                            String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                            LOGGER.log(Level.INFO, "Pin Validation Failed Resposnse ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
                            return failedResponseJson;

                        } else {
//

                            //Put Pnd on Customer on CBA
                            StringBuilder ofs = new StringBuilder("");
                            ofs.append("POSTING.RESTRICT::=").append("14").append(",");
                            //OFS to append posting restriction on the customer
                            String ofsRequestCustomer = "\"" + env.getProperty("webservice.customer.posting.restriction").trim() + ","
                                    + env.getProperty("webservice.T24.inputter.login.credentials").trim()
                                    + "/" + "NG0010068" + "," + account.getCustomerId() + "," + ofs + "\"";

                            LOGGER.log(Level.INFO, "Block Account OFS Request: {0}", ofsRequestCustomer);

                            String ofsResponseCustomer = ivrGenericService.endPointPostRequest("/generic/payment/postofs", ofsRequestCustomer,
                                    env.getProperty("webservice.middleware.production.username"),
                                    env.getProperty("webservice.middleware.production.password"));

                            LOGGER.log(Level.INFO, "Block Account OFS Request {0}", ofsResponseCustomer);

                            String validationResponse = ivrGenericService.validateResponse(ofsResponseCustomer);
                            if (validationResponse != null) {
                                //Create activity log
                                failedResponse.setResponseCode(failedCode);
                                failedResponse.setResponseDescription(validationResponse);
                                String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
                                LOGGER.log(Level.INFO, "Block Account Response ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                                return failedResponseJson;
                            }

                            if (ofsResponseCustomer.contains("//1")) {
                                responsePayload.setAccountNumber(request.getAccountNumber());
                                responsePayload.setResponseCode(successCode);
                                responsePayload.setResponseDescription(messageSource.getMessage("appMessages.operationSuccessful", new Object[0], Locale.ENGLISH));

                                response = gson.toJson(responsePayload, IVRResponsePayload.class);
                                LOGGER.log(Level.INFO, "Block Account Response".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(response));
                                return response;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            failedResponse.setResponseCode(failedCode);
            failedResponse.setResponseDescription(ex.getMessage());
            String failedResponseJson = gson.toJson(failedResponse, ResponsePayload.class);
            LOGGER.log(Level.INFO, "Failed Response ".concat(requestSource).concat(" - IP ADDRESS = ").concat(remoteIP).concat(" : RESPONSE ").concat(failedResponseJson));
            return failedResponseJson;
        }
        return response;
    }

    private String accountOpening(String mobileNumber) {
        ResponsePayload response = new ResponsePayload();
        String responseJson = "";
        try {
            T24Customers customer = custAccountRepositoy.getCustomerDetails(mobileNumber);
            if (customer != null) {
                //create numbering code
                StringBuilder accNumberingOfsBase = new StringBuilder("");
                accNumberingOfsBase.append("CUSTOMER.NO::=").append(customer.getCustomerId().trim()).append(",");
                accNumberingOfsBase.append("PDT.CODE::=").append("14").append(",");
                accNumberingOfsBase.append("CREATED.Y.N::=N");

                String accNumberingOfsRequest = "\"" + env.getProperty("a24core.account.numbering.code").trim() + ","
                        + env.getProperty("a24core.T24.inputter.login.credentials").trim()
                        + "/" + customer.getBranchCode().trim() + ",," + accNumberingOfsBase + "\"";

                LOGGER.log(Level.INFO, "Request: Account Numbering Code {0}", accNumberingOfsRequest);

                //Check where the environment is pointed to
                String accNumberingResponse = ivrGenericService.ofsResponse(env.getProperty("a24core.live"), accNumberingOfsRequest);
                if (accNumberingResponse == null) {
                    return messageSource.getMessage("appMessages.invalidMenuPointer", new Object[0], Locale.ENGLISH);
                }

                String accNumberingValidationResponse = ivrGenericService.validateResponse(accNumberingResponse);
                if (accNumberingValidationResponse != null) {
                    //Create activity log
                    response.setResponseCode(env.getProperty("api.webservice.transaction.failed.error.code"));
                    response.setResponseDescription(accNumberingValidationResponse);
                    String failedResponseJson = gson.toJson(response, ResponsePayload.class);
                    LOGGER.log(Level.INFO, "Failed Response: Account Numbering Code {0}", accNumberingValidationResponse);
                    return failedResponseJson;

                }

                if (accNumberingResponse.contains("//1")) {
                    LOGGER.log(Level.INFO, "Response: Account Numbering Generated{0}", accNumberingResponse);
                    //Add to ivr account table
                    IVRAccount ivraccount = new IVRAccount();
                    ivraccount.setAccountNumber("");
                    ivraccount.setBranchCode(customer.getBranchCode());
                    ivraccount.setBvn(customer.getBvn());
                    ivraccount.setCreatedAt(LocalDateTime.now());
                    ivraccount.setCreatedBy("IVR");
                    ivraccount.setDateOfBirth(customer.getDob());
                    ivraccount.setFirstName(customer.getFirstName());
                    ivraccount.setLastName(customer.getLastName());
                    ivraccount.setKyc("1");
                    ivraccount.setMobileNumber(mobileNumber);
                    ivraccount.setProductCategory("6002");
                    ivraccount.setNumberingCodeGenerated(true);
                    ivraccount.setStatus("Pending");

                    IVRAccount createdAccount = ivrRepository.createAccount(ivraccount);

                    //Update the account record
                    String accountNumber = ivrGenericService.getStringFromOFSResponse(accNumberingResponse, "ACCT.CODE:1:1");
                    StringBuilder ofsBase = new StringBuilder("");
                    ofsBase.append("CUSTOMER:1:1::=").append(customer.getCustomerId().trim()).append(",");
                    ofsBase.append("CURRENCY:1:1::=").append("NGN").append(",");
                    ofsBase.append("MNEMONIC:1:1::=").append(customer.getFirstName().substring(0, 1).toUpperCase()
                            .concat(ivrGenericService.generateMnemonic(6))).append(",");
                    ofsBase.append("ACCOUNT.OFFICER:1:1::=").append("7801").append(",");
                    ofsBase.append("OTHER.OFFICER:1:1::=").append("9998").append(",");
                    ofsBase.append("CATEGORY:1:1::=").append("6002");

                    String ofsRequest = "\"" + env.getProperty("ussd.account.save.brighta.create").trim() + ","
                            + env.getProperty("a24core.T24.inputter.login.credentials").trim()
                            + "/" + customer.getBranchCode().trim() + "," + accountNumber + "," + ofsBase + "\"";

                    LOGGER.log(Level.INFO, "Request: Account Opening {0}", ofsRequest);

                    String accountResponse = ivrGenericService.endPointPostRequest("/generic/payment/postofs", ofsRequest,
                            env.getProperty("webservice.middleware.production.username"),
                            env.getProperty("webservice.middleware.production.password"));

                    String validationResponse = ivrGenericService.validateResponse(accountResponse);
                    if (validationResponse != null) {
                        LOGGER.log(Level.INFO, "Failed Response: Account Opening {0}", validationResponse);
                        //Create activity log
                        response.setResponseCode(env.getProperty("api.webservice.transaction.failed.error.code"));
                        response.setResponseDescription(validationResponse);
                        String failedResponseJson = gson.toJson(response, ResponsePayload.class);

                        return failedResponseJson;
                    }
                    if (accountResponse.contains("//1")) {
                        //update the ivr account table
                        LOGGER.log(Level.INFO, "Successful Response: Account Opening {0}", accountResponse);
                        ivraccount.setAccountNumber(ivrGenericService.getStringFromOFSResponse(accountResponse, "ALT.ACCT.ID:4:1"));
                        ivraccount.setNumberingCodeGenerated(false);
                        ivraccount.setStatus("Enabled");

                        IVRAccount updateAccount = ivrRepository.updateAccount(ivraccount);

                        //update the customer table
                        IVRCustomer newCustomer = ivrRepository.getCustomerUsingPhoneNumber(mobileNumber);
                        if (newCustomer != null) {
                            newCustomer.setAccountNumber(ivraccount.getAccountNumber());
                            newCustomer.setStatus("Enabled");
                            newCustomer.setFirstName(customer.getFirstName());
                            newCustomer.setLastName(customer.getLastName());
                            newCustomer.setGender(customer.getGender());
                            newCustomer.setDateOfBirth(customer.getDob());
                            newCustomer.setBranchCode(customer.getBranchCode());
                            newCustomer.setCustomerNumber(customer.getCustomerId());

                            IVRCustomer updateCustomer = ivrRepository.updateCustomer(newCustomer);
                        }

                        //Add this account to T24 account table
                        T24Accounts account = new T24Accounts();
                        account.setAccountDescription(ivrGenericService.getProductCodeWithProductCode(ivrGenericService.getStringFromOFSResponse(accountResponse, "CATEGORY:1:1")));
                        account.setAccountName(ivrGenericService.getStringFromOFSResponse(accountResponse, "ACCOUNT.TITLE.1:1:1"));
                        account.setAccountNumber(ivrGenericService.getStringFromOFSResponse(accountResponse, "ALT.ACCT.ID:4:1"));
                        account.setAccountRestriction(ivrGenericService.getStringFromOFSResponse(accountResponse, "POSTING.RESTRICT:1:1"));
                        account.setAccountStatus("Active");
                        account.setAccountType("AC");
                        account.setBranchCode(customer.getBranchCode());
                        account.setBranchName(ivrGenericService.getBranchNameUsingCode(customer.getBranchCode()));
                        account.setBvn(customer.getBvn());
                        account.setCategoryCode(ivrGenericService.getStringFromOFSResponse(accountResponse, "CATEGORY:1:1"));
                        account.setCurrency(ivrGenericService.getStringFromOFSResponse(accountResponse, "CURRENCY:1:1"));
                        account.setCustomerId(customer.getCustomerId());
                        account.setMobileNumber(customer.getMobileNumber());
                        String openingDate = ivrGenericService.getStringFromOFSResponse(accountResponse, "OPENING.DATE:1:1");
                        account.setOpeningDate(openingDate.substring(0, 4).concat("/").concat(openingDate.substring(4, 6)).concat("/").concat(openingDate.substring(6, openingDate.length())));
                        account.setProductCode(ivrGenericService.getStringFromOFSResponse(accountResponse, "CATEGORY:1:1"));
                        account.setProductDescription(ivrGenericService.getProductCodeWithProductCode(ivrGenericService.getStringFromOFSResponse(accountResponse, "CATEGORY:1:1")));
                        account.setT24AccountNumber(ivrGenericService.getT24TransIdFromResponse(accountResponse));

                        T24Accounts createAccount = custAccountRepositoy.createT24Accounts(account);

                        response.setResponseCode(env.getProperty("api.webservice.success.code"));
                        response.setResponseDescription(messageSource.getMessage("appMessages.operationSuccessful", new Object[0], Locale.ENGLISH));
                        response.setAccountNumber(createAccount.getAccountNumber());
                        responseJson = gson.toJson(response, ResponsePayload.class);
                        LOGGER.log(Level.INFO, "Response: Account Opening Response Json{0}", responseJson);
                        return responseJson;

                    }
                }
            }

        } catch (Exception ex) {
            return ex.getMessage();
        }

        return responseJson;
    }

    private IVRResponsePayload accountBalance(String accountNumber) {

        RequestPayload balanceRequest = new RequestPayload();
        balanceRequest.setTransId("mob1234567890");
        balanceRequest.setAccountNumber(accountNumber);
        String balanceJson = gson.toJson(balanceRequest);
        String accountResponse = ivrGenericService.endPointPostRequest("/generic/account/balance", balanceJson,
                env.getProperty("webservice.middleware.production.username"),
                env.getProperty("webservice.middleware.production.password"));
        IVRResponsePayload balance = gson.fromJson(accountResponse, IVRResponsePayload.class);
        return balance;
    }

    private String createCustomer(String bvn) {
        ResponsePayload responsePayload = new ResponsePayload();
        String failedResponseJson = "";
        BVNIVR custbvn = bvnRepository.getBVN(bvn);
        IVRCustomer createCust = ivrRepository.getT24CustomerUsingPhoneNumber(custbvn.getMobileNumber());
        if (custbvn != null) {
            StringBuilder ofsBase = new StringBuilder("");
            //Add static fields
            ofsBase.append("SHORT.NAME::=").append(custbvn.getLastName().toUpperCase()).append(",");
            ofsBase.append("NAME.1::=").append(custbvn.getFirstName().toUpperCase()).append(",");
            ofsBase.append("BIRTH.INCORP.DATE::=").append(custbvn.getDateOfBirth().replace("-", "")).append(",");
            ofsBase.append("ACCOUNT.OFFICER::=").append("7801").append(",");
            ofsBase.append("OTHER.OFFICER::=").append("9998").append(",");
            ofsBase.append("ID.TYPES::=").append("5").append(",");
            ofsBase.append("IDENTIFY.OTHER2::=").append("NIL").append(",");
            ofsBase.append("UTILITY.BILL::=").append("N").append(",");
            ofsBase.append("WORK.GEN::=").append(custbvn.getGender().trim().equals("Male") ? "M" : "F").append(",");
            ofsBase.append("MARRIED.STATUS::=").append("2").append(",");
            ofsBase.append("STREET::=").append("NA").append(",");
            ofsBase.append("SUBURB.TOWN::=").append("NA").append(",");
            ofsBase.append("PROVINCE.STATE::=").append("NA").append(",");
            ofsBase.append("TEL.MOBILE::=").append(custbvn.getMobileNumber()).append(",");
            ofsBase.append("SECTOR::=").append("1100").append(",");
            ofsBase.append("SMS.NOTIFICATN::=").append("Y").append(",");
            ofsBase.append("MNEMONIC::=").append(custbvn.getFirstName().substring(0, 1).toUpperCase()
                    .concat(ivrGenericService.generateMnemonic(6))).append(",");
            ofsBase.append("E.MAIL.ADDRESS::=").append("NA");

            String ofsRequest = "\"" + env.getProperty("ussd.customer.create.withBVN").trim() + ","
                    + env.getProperty("webservice.T24.inputter.login.credentials").trim()
                    + "/" + "NG0010068" + ",," + ofsBase + "\"";

            String response = ivrGenericService.endPointPostRequest("/generic/payment/postofs", ofsRequest,
                    env.getProperty("webservice.middleware.production.username"),
                    env.getProperty("webservice.middleware.production.password"));

            String validationResponse = ivrGenericService.validateResponse(response);
            if (validationResponse != null) {
                //update the ussd customer table
                if (createCust != null) {
                    createCust.setStatus("Failed");
                    createCust.setFirstName(custbvn.getFirstName());
                    createCust.setLastName(custbvn.getLastName());
                    createCust.setGender(custbvn.getGender());
                    createCust.setDateOfBirth(null);
                    createCust.setBranchCode(null);
                    createCust.setCustomerNumber(null);
                    createCust.setFailureReason(validationResponse);
                    IVRCustomer updateCustomer = ivrRepository.updateCustomer(createCust);
                }

                responsePayload.setResponseCode(env.getProperty("api.webservice.transaction.failed.error.code"));
                responsePayload.setResponseDescription(validationResponse);
                failedResponseJson = gson.toJson(responsePayload, ResponsePayload.class);
                return failedResponseJson;
            }

            if (response.contains("//1")) {

                //update customer table
                T24Customers cust = new T24Customers();
                cust.setAccountOfficer("7801");
                cust.setAddress("");
                cust.setBranchCode("NG0010068");
                cust.setBranchName("DIGITAL");
                cust.setBvn(custbvn.getCustomerBvn().toUpperCase());
                cust.setCity("");
                cust.setCustomerId(ivrGenericService.getStringFromOFSResponse(response, "CUS.AMFB.NO:1:1"));
                cust.setCustomerName(custbvn.getFirstName().concat(" ").concat(custbvn.getLastName()));
                cust.setCustomerRestriction("");
                cust.setDob(custbvn.getDateOfBirth().toUpperCase());
                cust.setEmail("");
                cust.setFirstName(custbvn.getFirstName().toUpperCase());
                cust.setGender(custbvn.getGender().toUpperCase());
                cust.setKycTier("1");
                cust.setLastName(custbvn.getLastName().toUpperCase());
                cust.setMiddleName(custbvn.getMiddleName().toUpperCase());
                cust.setMobileNumber(custbvn.getMobileNumber().toUpperCase());
                cust.setState("");
                cust.setStreet("");
                T24Customers customer = custAccountRepositoy.createCustomer(cust);

                String result = accountOpening(custbvn.getMobileNumber());
                return result;

            }

        }
        return failedResponseJson;

    }

}
