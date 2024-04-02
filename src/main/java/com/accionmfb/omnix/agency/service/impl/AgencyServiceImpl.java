/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.AccionAgent;
import com.accionmfb.omnix.agency.model.AppUser;
import com.accionmfb.omnix.agency.payload.AccionAgentBoardPayload;
import com.accionmfb.omnix.agency.payload.AccionAgentResponsePayload;
import com.accionmfb.omnix.agency.payload.AccountBalanceResponsePayload;
import com.accionmfb.omnix.agency.payload.FundsTransferResponsePayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.agency.payload.OmnixResponsePayload;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.AccountService;
import com.accionmfb.omnix.agency.service.AgencyService;
import com.accionmfb.omnix.agency.service.GenericService;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class AgencyServiceImpl implements AgencyService {

    @Autowired
    AgencyRepository agencyRepository;
    @Autowired
    GenericService genericService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    AccountService accountService;
    @Autowired
    Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.agency.banking.grupp.secretkey}")
    private String gruppSeccretKey;
    Logger logger = LoggerFactory.getLogger(AgencyServiceImpl.class);

    @Override
    public String localFundsTransferTest(String token, Object requestPayload) {
        OmnixResponsePayload responsePayload = new OmnixResponsePayload();
        try {
            int remainder = getRandomNumber() % 2;
            if (remainder == 0) {
                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
                ftResponse.setAmount("100");
                ftResponse.setCreditAccount("0011223344");
                ftResponse.setDebitAccount("4433221100");
                ftResponse.setNarration("Payment For Goods By John Doe");
                ftResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                ftResponse.setTransRef("FT67BNMMI9900");
                String response = gson.toJson(ftResponse);
                return response;
            } else {
                responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("Failed Transaction", new Object[0], Locale.ENGLISH));
                String response = gson.toJson(responsePayload);
                return response;
            }
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            String response = gson.toJson(responsePayload);
            return response;
        }
    }

    public String accountBalanceTest(String token, Object requestPayload) {
        OmnixResponsePayload responsePayload = new OmnixResponsePayload();
        try {
            int remainder = getRandomNumber() % 2;
            if (remainder == 0) {
                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
                ftResponse.setAmount("100");
                ftResponse.setCreditAccount("0011223344");
                ftResponse.setDebitAccount("4433221100");
                ftResponse.setNarration("Payment For Goods By John Doe");
                ftResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                ftResponse.setTransRef("FT67BNMMI9900");
                String response = gson.toJson(ftResponse);
                return response;
            } else {
                responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.ft.failed", new Object[0], Locale.ENGLISH));
                String response = gson.toJson(responsePayload);
                return response;
            }
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            String response = gson.toJson(responsePayload);
            return response;
        }
    }

    private int getRandomNumber() {
        return (int) ((Math.random() * (5 - 1)) + 1);
    }

    @Override
    public boolean validateAccionAgentBoardPayload(String token, AccionAgentBoardPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAgentName());
        rawString.add(requestPayload.getAgentAccountNumber());
        rawString.add(requestPayload.getAgentAddress());
        rawString.add(requestPayload.getAgentMobileNumber());
        rawString.add(requestPayload.getAgentTerminalId());
        rawString.add(requestPayload.getAgentId());
        rawString.add(requestPayload.getAgentState());
        rawString.add(requestPayload.getAgentCity());
        rawString.add(requestPayload.getAgentSupervisor());
        rawString.add(requestPayload.getAgentVendor());
        rawString.add(requestPayload.getStatus());
        rawString.add(requestPayload.getId());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processAccionAgentBoard(String token, AccionAgentBoardPayload requestPayload) {
        AccionAgentResponsePayload responsePayload = new AccionAgentResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Accion Agent Board", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Accion Agent Board", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Accion Agent Board", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if its a new or update
            if (requestPayload.getId().equalsIgnoreCase("0")) {
                //This is a new Agent record
                AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getAgentTerminalId(), "Grupp");
                if (agent != null) {
                    //Log the error
                    genericService.generateLog("Accion Agent Board", token, messageSource.getMessage("appMessages.agent.exist", new Object[]{"Terminal Id ", requestPayload.getAgentTerminalId(), "Grupp"}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                    //Create User Activity log
                    genericService.createUserActivity("", "Accion Agent Board", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.exist", new Object[]{"Terminal Id ", requestPayload.getAgentTerminalId(), "Grupp"}, Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                    errorResponse.setResponseCode(ResponseCodes.RECORD_EXIST_CODE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.agent.exist", new Object[]{"Terminal Id ", requestPayload.getAgentTerminalId(), "Grupp"}, Locale.ENGLISH));
                    return gson.toJson(responsePayload);
                }

                //Persist the record
                AccionAgent newAgent = new AccionAgent();
                newAgent.setAgentAccountNumber(requestPayload.getAgentAccountNumber());
                newAgent.setAgentAddress(requestPayload.getAgentAddress());
                newAgent.setAgentCity(requestPayload.getAgentCity());
                newAgent.setAgentId(requestPayload.getAgentId());
                newAgent.setAgentMobile(requestPayload.getAgentMobileNumber());
                newAgent.setAgentName(requestPayload.getAgentName());
                newAgent.setAgentState(requestPayload.getAgentState());
                newAgent.setAgentSupervisor(requestPayload.getAgentSupervisor());
                newAgent.setAgentVendor(requestPayload.getAgentVendor());
                newAgent.setCreatedAt(LocalDateTime.now());
                newAgent.setCreatedBy(requestBy);
                newAgent.setDateRegistered(LocalDate.now().toString());
                newAgent.setRanking("0");
                newAgent.setStatus("ACTIVE");
                newAgent.setTerminalId(requestPayload.getAgentTerminalId());
                AccionAgent createdAgent = agencyRepository.createAccionAgent(newAgent);

                //Send Notification to Grupp
                if (requestPayload.getAgentVendor().equalsIgnoreCase("Grupp")) {
                    //Call the Account microservices
                    OmnixRequestPayload drAccBalRequest = new OmnixRequestPayload();
                    drAccBalRequest.setAccountNumber(requestPayload.getAgentAccountNumber());
                    drAccBalRequest.setRequestId(requestPayload.getRequestId());
                    drAccBalRequest.setToken(token);
                    drAccBalRequest.setHash(genericService.hashAccountBalanceRequest(drAccBalRequest));
                    String drAccBalRequestJson = gson.toJson(drAccBalRequest);

                    //Call the account microservices
                    String drAccBalResponseJson = accountService.accountBalance(token, drAccBalRequestJson);
                    AccountBalanceResponsePayload drAccBalResponse = gson.fromJson(drAccBalResponseJson, AccountBalanceResponsePayload.class);

                    //Send details to Grupp via the webhook
                    AccionAgentBoardPayload newNotificationRequest = new AccionAgentBoardPayload();
                    newNotificationRequest.setAccountNumber(requestPayload.getAgentAccountNumber());
                    newNotificationRequest.setAmount(drAccBalResponse.getAvailableBalance().replace(",", ""));
                    String transRef = genericService.generateTransRef("NTF");
                    newNotificationRequest.setTransactionReference(transRef);

                    StringJoiner rawString = new StringJoiner("|");
                    rawString.add(gruppSeccretKey);
                    rawString.add(requestPayload.getAgentAccountNumber().trim());
                    rawString.add(transRef);
                    rawString.add(drAccBalResponse.getAvailableBalance().replace(",", ""));
                    String hashString = genericService.hash(rawString.toString(), "SHA512");
                    newNotificationRequest.setHash(hashString);

                    requestJson = gson.toJson(newNotificationRequest);
                    Unirest.setTimeouts(0, 0);
                    HttpResponse<String> httpResponse = Unirest.post("https://gruppacc.trygrupp.africa/mk-grupp-notify")
                            .header("Authorization", "Basic Z3J1cHAtYWNjb3VudDpNMG94WTBoQmVUTTBNelJPUkUweENnPT0=")
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body(requestJson)
                            .asString();
                    //Log the error
                    logger.info("Grupp Notification - Agent Board Request " + requestJson);
                    //Log the error
                    logger.info("Grupp Notification - Agent Board Response " + httpResponse.getBody());
                    httpResponse.getBody();
                }

                AccionAgentResponsePayload successResponse = new AccionAgentResponsePayload();
                successResponse.setAccountNumber(createdAgent.getAgentAccountNumber());
                successResponse.setAgentName(createdAgent.getAgentName());
                successResponse.setPhoneNumber(createdAgent.getAgentMobile());
                successResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                successResponse.setBankCode("090134");
                String responseJson = gson.toJson(successResponse);
                //Log the error
                genericService.generateLog("Accion Agent Board", token, responseJson, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAgentTerminalId(), "Accion Agent Board", "", channel, "Success", requestPayload.getRequestId(), 'S');
                return responseJson;
            }

            //Check if the Agent details exist already
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getAgentTerminalId(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Accion Agent Board", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Accion Agent Board", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            AccionAgent agentById = agencyRepository.getAgentUsingId(Long.valueOf(requestPayload.getId()));
            if (agentById != null) {
                agentById.setAgentAccountNumber(requestPayload.getAgentAccountNumber());
                agentById.setAgentAddress(requestPayload.getAgentAddress());
                agentById.setAgentCity(requestPayload.getAgentCity());
                agentById.setAgentId(requestPayload.getAgentId());
                agentById.setAgentMobile(requestPayload.getAgentMobileNumber());
                agentById.setAgentName(requestPayload.getAgentName());
                agentById.setAgentState(requestPayload.getAgentState());
                agentById.setAgentSupervisor(requestPayload.getAgentSupervisor());
                agentById.setAgentVendor(requestPayload.getAgentVendor());
                agentById.setStatus(requestPayload.getStatus());
                agentById.setTerminalId(requestPayload.getAgentTerminalId());

                AccionAgent createdAgent = agencyRepository.updateAccionAgent(agentById);
                AccionAgentResponsePayload successResponse = new AccionAgentResponsePayload();
                successResponse.setAccountNumber(createdAgent.getAgentAccountNumber());
                successResponse.setAgentName(createdAgent.getAgentName());
                successResponse.setPhoneNumber(createdAgent.getAgentMobile());
                successResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                successResponse.setBankCode("090134");
                String responseJson = gson.toJson(successResponse);
                //Log the error
                genericService.generateLog("Accion Agent Board", token, responseJson, "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getAgentTerminalId(), "Accion Agent Board", "", channel, "Success", requestPayload.getRequestId(), 'S');
                return responseJson;
            }

            //Log the response
            genericService.generateLog("Accion Agent Board", token, "Invalid Agent ID", "API Error", "DEBUG", requestPayload.getRequestId());
            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage("Invalid Agent ID");
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Accion Agent Board", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public boolean validateAccionAgentDetailsPayload(String token, MobileNumberRequestPayload requestPayload) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getRequestId().trim());
        String decryptedString = genericService.decryptString(requestPayload.getHash(), encryptionKey);
        return rawString.toString().equalsIgnoreCase(decryptedString);
    }

    @Override
    public String processAccionAgentDetails(String token, MobileNumberRequestPayload requestPayload) {
        AccionAgentResponsePayload responsePayload = new AccionAgentResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        //Log the request 
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Accion Agent Detalil", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                //Log the error
                genericService.generateLog("Accion Agent Details", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Accion Agent Details", "", channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            //Check if the Agent details exist 
            AccionAgent agent = agencyRepository.getAgentUsingPhoneNumber(requestPayload.getMobileNumber(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Accion Agent Details", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getMobileNumber(), "Accion Agent Details", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getMobileNumber()}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            AccionAgentResponsePayload successResponse = new AccionAgentResponsePayload();
            successResponse.setAccountNumber(agent.getAgentAccountNumber());
            successResponse.setAgentName(agent.getAgentName());
            successResponse.setPhoneNumber(agent.getAgentMobile());
            successResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            successResponse.setAgentAddress(agent.getAgentAddress());
            successResponse.setAgentCity(agent.getAgentCity());
            successResponse.setAgentState(agent.getAgentState());
            successResponse.setAgentSupervisor(agent.getAgentState());
            successResponse.setTerminalId(agent.getTerminalId());
            successResponse.setAgentVendor(agent.getAgentVendor());
            successResponse.setId(String.valueOf(agent.getId()));
            String responseJson = gson.toJson(successResponse);
            //Log the error
            genericService.generateLog("Accion Agent Details", token, responseJson, "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getMobileNumber(), "Accion Agent Details", "", channel, "Success", requestPayload.getRequestId(), 'S');
            return responseJson;
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Accion Agent Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }

    @Override
    public String openAccount(Map<String, String> customerInfo, double initialDepositAmount, String accountType, String openingBranch) {
        return null;
    }

}
