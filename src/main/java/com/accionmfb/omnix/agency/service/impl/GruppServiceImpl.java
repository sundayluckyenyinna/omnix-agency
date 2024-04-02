/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.*;
import com.accionmfb.omnix.agency.payload.*;
import com.accionmfb.omnix.agency.repository.*;
import com.accionmfb.omnix.agency.service.*;
import com.accionmfb.omnix.agency.service.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jbase.jremote.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author bokon
 */
@Service
@Slf4j
public class GruppServiceImpl implements GruppService {

    @Autowired
    NipUtil nipUtil;

    @Autowired
    ElectricityService electricityService;
    @Autowired
    FundsTransferRepository ftRepository;

    @Autowired
    private TransactionReferenceRepository transactionReferenceRepository;
    @Autowired
    AirtimeService airtimeService;
    @Autowired
    FundsTransferService ftService;
    @Autowired
    CableTVService cabletvService;
    @Autowired
    private GenericService genericService;

    @Autowired
    private CashoutStatusRepository cashoutStatusRepository;

    @Value("${omnix.agency.banking.grupp.secretkey}")
    private String gruppSeccretKey;
    @Value("${omnix.agency.banking.grupp.receivable}")
    private String gruppReceivableAccount;
    @Value("${omnix.agency.banking.grupp.splitaccount}")
    private String gruppSplitAccount;
    @Value("${agency.banking.grupp.incomeAccount}")
    private String incomeAccount;
    @Value("${omnix.agency.banking.grupp.fee}")
    private String gruppFee;
    @Value("${omnix.cabletv.vendor}")
    private String cableTVVendor;
    @Value("${omnix.electricity.vendor}")
    private String electricityVendor;

    @Value("${omnix.t24.port}")
    private String t24Port;

    @Value("${omnix.t24.host}")
    private String t24Host;
    @Value("${omnix.version.funds.transfer}")
    private String ftVersion;

    @Value("${app.errandPay.vatPayableAccount}")
    private String vatPayableAccount;

    @Value("${omnix.nip.environment}")
    private String ftEnvironment;

    @Value("${omnix.middleware.grupp.authorization}")
    String middlewareAuthorization;
    @Value("${omnix.middleware.grupp.username}")
    String middlewareUsername;

    @Value("${nip.max.limit}")
    private String nipMaximumAmount;
    @Value("${nip.tran.type}")
    private String tranType;

    @Value("${nip.tran.code}")
    private String tranCode;
    @Value("${nip.vat.code}")
    private String vatCode;

    @Value("${nip.tran.agency.code}")
    private String agencyCode;
    @Value("${nip.tran.mobile.code}")
    private String mobileCode;
    @Value("${tranfer.vat.rate}")
    private double vatRate;

    @Value("${nip.outbound.settlement.account}")
    String nipOutboundSettlementAccount;

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
    private NotificationHistoryRepo notificationHistoryRepo;
    private double TRANSFER_FEE = 20;

    @Override
    public boolean validateGruppCashoutNotificationPayload(String token, GruppCashoutNotificationPayload requestPayload) {
        StringJoiner rawString = new StringJoiner("|");
        rawString.add(gruppSeccretKey);
        rawString.add(requestPayload.getReference().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getTransactionDate().trim());
        String hashString = genericService.hash(rawString.toString(), "SHA512");
        return requestPayload.getHash().equalsIgnoreCase(hashString);

//        return true;
    }

    @Override
    public String processGruppCashoutNotification(String token, GruppCashoutNotificationPayload requestPayload) {

        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        log.info("before request json ----->>>> {}", requestPayload.toString());
        Gson gson = new Gson();
        String requestJson = gson.toJson(requestPayload);
        log.info("after request json ----->>>> {}", requestJson);
        genericService.generateLog("Grupp Cashout Notification", token, requestJson, "API Request", "INFO", requestPayload.getReference());
        log.info("--------saved transaction-----");
        try {
            //Check if the transaction is coming from GRUPP
            log.info("entering try block----->>");
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                log.info("grupp is not what it is");
                genericService.generateLog("Grupp Cashout Notification", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getReference());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cashout Notification", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getTerminalId(), "Grupp");
            log.info("found agent ----->>> {}", agent);
            if (agent == null) {
                //Log the error
                log.info("terminal id is null");
                genericService.generateLog("Grupp Cashout Notification", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getReference());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cashout Notification", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                log.info("account number null");
                genericService.generateLog("Grupp Cashout Notification", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getReference());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cashout Notification", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            if (agent.getRemainingLimit() == null) {
                agent.setRemainingLimit(BigDecimal.valueOf(0));
                agencyRepository.updateAccionAgent(agent);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                log.info("customer is not active");
                genericService.generateLog("Grupp Cashout Notification", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getReference());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cashout Notification", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            log.info("agent not null");
            log.info("about to check for customer");

            Customer customer = agencyRepository.getCustomerUsingMobileNumber(agent.getAgentMobile());
            log.info("found customer ------>>> {}", customer);
            if (customer == null || customer.getId() == null) {
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("Customer not found", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            TransactionReference foundReferenceNumber = transactionReferenceRepository.getByReferenceNumber(requestPayload.getReference());
            if (foundReferenceNumber != null) {
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("Reference number already exists", new Object[]{requestPayload.getReference()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            log.info("found app user ------->>>> {}", appUser);

            Branch branch = genericService.getBranchUsingBranchCode("NG0010068"); //Defaulted to Digital Branch
            log.info("found branch ------->>>> {}", branch);

            NotificationPayloadMapping payloadMapping = new NotificationPayloadMapping();
            BeanUtils.copyProperties(requestPayload, payloadMapping);
            String stringedRequest = gson.toJson(payloadMapping);

            CashoutStatus cashoutStatus = CashoutStatus.builder()
                    .message("All checks passed")
                    .payload(stringedRequest)
                    .tranRef(requestPayload.getReference())
                    .responseCode("")
                    .status("NOT SUCCESSFUL")
                    .build();
            log.info("cash out Status ---->> {}", cashoutStatus);

            cashoutStatusRepository.createStatusReport(cashoutStatus);

            log.info("good to go------");
            BigDecimal transAmount = new BigDecimal(requestPayload.getAmount());
            BigDecimal percent = new BigDecimal("100");
            BigDecimal grupFee = new BigDecimal(gruppFee).divide(percent).multiply(transAmount);
            if (grupFee.doubleValue() > 50) {
                grupFee = BigDecimal.valueOf(50);
            }
            BigDecimal netAmount = (new BigDecimal(requestPayload.getAmount())).subtract(grupFee).setScale(2, RoundingMode.CEILING);

//            //Fee is 0.6% of the transaction amount. This equals 0.006
            BigDecimal transFee;
            transFee = transAmount.multiply(new BigDecimal("0.006"));
            //Check to cap it at N100 max
            if (transFee.compareTo(new BigDecimal(100)) >= 0) {
                log.info("stuff happened");
                transFee = new BigDecimal("100");
            }
            BigDecimal accionFee = transFee.subtract(grupFee).setScale(2, RoundingMode.CEILING);
            BigDecimal agentAmount = new BigDecimal(netAmount.toString()).subtract(accionFee).setScale(2, RoundingMode.CEILING);

            BigDecimal grossIncomeAmount = netAmount.subtract(agentAmount);
            BigDecimal vatAmount = BigDecimal.valueOf(7.5).multiply(grossIncomeAmount).divide(BigDecimal.valueOf(107.5), 2, RoundingMode.CEILING);
            BigDecimal netIncomeAmount = grossIncomeAmount.subtract(vatAmount).setScale(2, RoundingMode.CEILING);

            String narration = "GRP/" + agent.getAgentAccountNumber() + "/" + requestPayload.getRrn();

            log.info("agent mobile number ----->>> {}", agent.getAgentMobile());

            BigDecimal amountToCheck = agent.getRemainingLimit().add(BigDecimal.valueOf(Long.parseLong(requestPayload.getAmount())));
            log.info("amount to check ---->>> {}", amountToCheck);

            String requestId = generateRequestId();
            if (amountToCheck.doubleValue() > 3000000) {
                log.info("transaction is greater than 3,000,000");

                AgentTranLog oAgentTranLog = AgentTranLog.builder().mobileNumber(agent.getAgentMobile()).debitAccount(gruppReceivableAccount).creditAccount(gruppSplitAccount).amount(requestPayload.getAmount())
                        .narration(narration).transType("ACAL").branchCode("NG0010068").inputter(requestBy + "-" + agent.getAgentMobile())
                        .authorizer(requestBy + "-" + agent.getAgentMobile()).requestId(requestId)
                        .status("Pending").date(LocalDate.now()).accountNumber(agent.getAgentAccountNumber())
                        .requestBy(requestBy).agentAmount(String.valueOf(agentAmount)).accionFee(String.valueOf(accionFee))
                        .channel(channel).terminalId(requestPayload.getTerminalId()).remainingLimit(String.valueOf(agent.getRemainingLimit()))
                        .tranRef(requestPayload.getReference()).build();

                log.info("Pending notification history ---->>> {}", oAgentTranLog);

                notificationHistoryRepo.createTransaction(oAgentTranLog);

                responsePayload.setMessage("PENDING");
            } else {
                AgentTranLog oAgentTranLog = AgentTranLog.builder().mobileNumber(agent.getAgentMobile())
                        .debitAccount(gruppReceivableAccount).creditAccount(gruppSplitAccount).amount(requestPayload.getAmount())
                        .narration(narration).transType("ACAL").branchCode("NG0010068").inputter(requestBy + "-" + agent.getAgentMobile())
                        .authorizer(requestBy + "-" + agent.getAgentMobile()).requestId(requestId)
                        .status("Authorised").date(LocalDate.now()).accountNumber(agent.getAgentAccountNumber())
                        .requestBy(requestBy).agentAmount(String.valueOf(agentAmount)).accionFee(String.valueOf(accionFee))
                        .channel(channel).terminalId(requestPayload.getTerminalId()).remainingLimit(String.valueOf(agent.getRemainingLimit()))
                        .tranRef(requestPayload.getReference()).build();

                log.info("transaction is lesser than 3,000,000");
                log.info("About to debit receivable and credit split");
                BigDecimal remBal = agent.getRemainingLimit();
                GruppResponsePayload responseFromTransfers = performAllTransfers(token, oAgentTranLog, requestPayload, requestBy, channel, agent, customer, appUser, branch, netAmount, accionFee, agentAmount, vatAmount, netIncomeAmount, remBal);
                if (!Objects.equals(responseFromTransfers.getStatus(), "SUCCESS")) {
                    responsePayload.setMessage("FAILED");
                    return gson.toJson(responsePayload);
                }
               

                BigDecimal updatedLimit = remBal.add(new BigDecimal(requestPayload.getAmount().replaceAll(",", "")));
                log.info("updatedLimit ---->> {}", updatedLimit);
                log.info("newAmount ---->> {}", requestPayload.getAmount().replaceAll(",", ""));
                log.info("amount ---->> {}", requestPayload.getAmount());
                log.info("rem ---->> {}", remBal);
                agent.setRemainingLimit(updatedLimit);
                agencyRepository.updateAccionAgent(agent);

            }

            responsePayload.setStatus("SUCCESS");
            responsePayload.setTransactionReference(requestPayload.getReference());
            CashoutStatus cashoutStatus5 = CashoutStatus.builder()
                    .message("SUCCESSFUL")
                    .payload(stringedRequest)
                    .tranRef(requestPayload.getReference())
                    .responseCode(ResponseCodes.SUCCESS_CODE.getResponseCode())
                    .status("Transaction completed")
                    .build();
            cashoutStatusRepository.createStatusReport(cashoutStatus5);

            return gson.toJson(responsePayload);

        } catch (Exception ex) {
            //Log the response
            log.info("An error occurred");
            genericService.generateLog("Grupp Cashout Notification", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getReference());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            log.info("exception message ---->>> {}", ex.getMessage());
            ex.printStackTrace();
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public GruppResponsePayload performAllTransfers(String token, AgentTranLog history, GruppCashoutNotificationPayload requestPayload, String requestBy, String channel, AccionAgent agent, Customer customer, AppUser appUser, Branch branch, BigDecimal netAmount, BigDecimal accionFee, BigDecimal agentAmount, BigDecimal vatAmount, BigDecimal netIncomeAmount, BigDecimal remBal) throws Exception {
        log.info("About to perform all transfers!!!");
        GruppResponsePayload responsePayload = new GruppResponsePayload();

        history.setMessage("Started processing transaction");
        notificationHistoryRepo.updateTransaction(history);

        FundsTransferResponsePayload ftResponseForSplitPayload = splitTransfer(history.getRequestId(), agent, requestBy, token, appUser, branch, history.getNarration(), netAmount);

        TransactionReference transactionReference = TransactionReference.builder().referenceNumber(history.getTranRef()).build();
        transactionReferenceRepository.createTransactionReference(transactionReference);

        log.info("response from fund transfer (from receivable to split) --->> {}", ftResponseForSplitPayload);
        //Check if the transaction failed
        if (!ftResponseForSplitPayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !ftResponseForSplitPayload.getStatus().equalsIgnoreCase("SUCCESS")) {
            log.info("transaction failed");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage(ftResponseForSplitPayload.getResponseMessage());
            return responsePayload;
        }
        history.setMessage("Debited receivable account and credited Split account");
        notificationHistoryRepo.updateTransaction(history);
        log.info("Debit receivable and credit split successful");

        String stringedRequest = gson.toJson(requestPayload.getTerminalId().concat("/").concat(requestPayload.getReference().concat("/").concat(requestPayload.getAmount())));
        log.info("stringed request ----->> {}", stringedRequest);
        CashoutStatus cashoutStatus2 = CashoutStatus.builder()
                .message("Debited grupp Receivable Account and credited grupp Split Account ")
                .payload(stringedRequest)
                .tranRef(history.getTranRef())
                .responseCode("")
                .status("NOT SUCCESSFUL")
                .build();
        log.info("cashout to be saved ---->> {}", cashoutStatus2);
        cashoutStatusRepository.createStatusReport(cashoutStatus2);

        //Transaction was successful at this point. Start a thread to do the splitting
        log.info("about to split");
        FundsTransferResponsePayload ftAgentTransfer = posNotificationSplit(agent.getAgentAccountNumber(), ftResponseForSplitPayload.getTransRef(), requestBy,
                String.valueOf(agentAmount), String.valueOf(accionFee), history.getNarration(), channel, appUser, branch, customer.getLastName());
        log.info("response from fund transfer (from split to agent) --->> {}", ftAgentTransfer);

        if (!ftAgentTransfer.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !ftAgentTransfer.getStatus().equalsIgnoreCase("SUCCESS")) {
            log.info("transaction failed");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage(ftAgentTransfer.getResponseMessage());
            return responsePayload;
        }
        log.info("Credited Agent successfully");
        history.setMessage("Debited split account and credited agent account");
        notificationHistoryRepo.updateTransaction(history);
        log.info("Debit receivable and credit split successful");

        CashoutStatus cashoutStatus3 = CashoutStatus.builder()
                .message("Debited grupp Split Account and credited Agent Account")
                .payload(stringedRequest)
                .tranRef(history.getTranRef())
                .responseCode("")
                .status("NOT SUCCESSFUL")
                .build();
        cashoutStatusRepository.createStatusReport(cashoutStatus3);

        log.info("about to credit VAT account");
        // VAT transfer
        FundsTransferResponsePayload ftResponseForVatPayload = vatLocalTransfer(agent, requestBy, token, appUser, branch, history.getNarration(), vatAmount);
        log.info("response from fund transfer (from split to VAT) --->> {}", ftResponseForVatPayload);
        if (!ftResponseForVatPayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !ftResponseForVatPayload.getStatus().equalsIgnoreCase("SUCCESS")) {
            log.info("transaction failed");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage(ftResponseForVatPayload.getResponseMessage());
            return responsePayload;
        }
        log.info("Credited VAT account successfully");
        history.setMessage("Debited split account and credited VAT account");
        notificationHistoryRepo.updateTransaction(history);

        log.info("about to credit income account");
        // Income account transfer
        FundsTransferResponsePayload ftResponseForIncomeAcctPayload = incomeAccountTransfer(agent, requestBy, token, appUser, branch, history.getNarration(), netIncomeAmount);
        log.info("response from fund transfer (from split to Income) --->> {}", ftResponseForIncomeAcctPayload);
        //Check if the transaction failed
        if (!ftResponseForIncomeAcctPayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !ftResponseForIncomeAcctPayload.getStatus().equalsIgnoreCase("SUCCESS")) {
            log.info("transaction failed");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage(ftResponseForIncomeAcctPayload.getResponseMessage());
            return responsePayload;
        }
        log.info("Credited Income account successfully");
        history.setMessage("Debited split account and credited Income account");
        notificationHistoryRepo.updateTransaction(history);

        CashoutStatus cashoutStatus4 = CashoutStatus.builder()
                .message("Debited grupp Split Account and credited income Account")
                .payload(stringedRequest)
                .tranRef(history.getTranRef())
                .responseCode("")
                .status("NOT SUCCESSFUL")
                .build();
        cashoutStatusRepository.createStatusReport(cashoutStatus4);

        history.setMessage("Debited split account and credited Income account");
        history.setStatus("Closed");
        notificationHistoryRepo.updateTransaction(history);
        responsePayload.setStatus("SUCCESS");
        return responsePayload;
    }

    public String RemoveDotExample(String input) {

        // Find the index of the dot
        int dotIndex = input.indexOf('.');
        String result = input.substring(0, dotIndex);
        return result;
    }

    private FundsTransferResponsePayload splitTransfer(String requestId, AccionAgent agent, String requestBy, String token, AppUser appUser, Branch branch, String narration, BigDecimal netAmount) throws Exception {
        log.info("About to split, from receivable to split account");
        //Generate the funds transfer request payload to Omnix
        LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
        ftRequestPayload.setMobileNumber(agent.getAgentMobile());
        ftRequestPayload.setDebitAccount(gruppReceivableAccount);
        ftRequestPayload.setCreditAccount(gruppSplitAccount);
        ftRequestPayload.setAmount(String.valueOf(netAmount));
        ftRequestPayload.setNarration(narration);
        ftRequestPayload.setTransType("ACAL");
        ftRequestPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        ftRequestPayload.setInputter(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setAuthorizer(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setNoOfAuthorizer("0");
        ftRequestPayload.setRequestId(requestId);
        ftRequestPayload.setToken(token);
        ftRequestPayload.setHash("1234567890");

        //Call the funds transfer microservices
        String ftResponseJson = internalLocalTransfer(ftRequestPayload, appUser, branch, token, "");
        return gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
    }

    private FundsTransferResponsePayload vatLocalTransfer(AccionAgent agent, String requestBy, String token, AppUser appUser, Branch branch, String narration, BigDecimal vatAmount) throws Exception {

        LocalTransferWithInternalPayload ftRequestPayloadForVAT = new LocalTransferWithInternalPayload();
        String tranRef = generateRequestId();

        //buiid payload for transfer to income account
        ftRequestPayloadForVAT.setMobileNumber(agent.getAgentMobile());
        ftRequestPayloadForVAT.setDebitAccount(gruppSplitAccount);
        ftRequestPayloadForVAT.setCreditAccount(vatPayableAccount);
        ftRequestPayloadForVAT.setAmount(String.valueOf(vatAmount));
        ftRequestPayloadForVAT.setNarration(narration);
        ftRequestPayloadForVAT.setTransType("AC");
        ftRequestPayloadForVAT.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        ftRequestPayloadForVAT.setInputter(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayloadForVAT.setAuthorizer(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayloadForVAT.setNoOfAuthorizer("0");
        ftRequestPayloadForVAT.setRequestId(tranRef); // seen
        ftRequestPayloadForVAT.setToken(token);
        ftRequestPayloadForVAT.setHash("0987654321");

        log.info("payload for VAT transfer ----->>> {}", ftRequestPayloadForVAT);

        //Call the funds transfer microservices
        String ftRequestPayloadForVATJson = internalLocalTransfer(ftRequestPayloadForVAT, appUser, branch, token, "");
        log.info("response from fund transfer for VAT --->> {}", ftRequestPayloadForVATJson);

        return gson.fromJson(ftRequestPayloadForVATJson, FundsTransferResponsePayload.class);

    }

    private FundsTransferResponsePayload incomeAccountTransfer(AccionAgent agent, String requestBy, String token, AppUser appUser, Branch branch, String narration, BigDecimal incomeAmount) throws Exception {
        LocalTransferWithInternalPayload ftRequestPayloadForIncomeAcct = new LocalTransferWithInternalPayload();
        String tranRef = generateRequestId();

        //buiid payload for transfer to income account
        ftRequestPayloadForIncomeAcct.setMobileNumber(agent.getAgentMobile());
        ftRequestPayloadForIncomeAcct.setDebitAccount(gruppSplitAccount);
        ftRequestPayloadForIncomeAcct.setCreditAccount(incomeAccount);
        ftRequestPayloadForIncomeAcct.setAmount(String.valueOf(incomeAmount));
        ftRequestPayloadForIncomeAcct.setNarration(narration);
        ftRequestPayloadForIncomeAcct.setTransType("AC");
        ftRequestPayloadForIncomeAcct.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        ftRequestPayloadForIncomeAcct.setInputter(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayloadForIncomeAcct.setAuthorizer(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayloadForIncomeAcct.setNoOfAuthorizer("0");
        ftRequestPayloadForIncomeAcct.setRequestId(tranRef); // seen
        ftRequestPayloadForIncomeAcct.setToken(token);
        ftRequestPayloadForIncomeAcct.setHash("0987654321");

        log.info("payload for income transfer ----->>> {}", ftRequestPayloadForIncomeAcct);

        //Call the funds transfer microservices
        String ftRequestPayloadForIncomeAcctJson = internalLocalTransfer(ftRequestPayloadForIncomeAcct, appUser, branch, token, "");
        log.info("response from fund transfer for income account  --->> {}", ftRequestPayloadForIncomeAcctJson);
        return gson.fromJson(ftRequestPayloadForIncomeAcctJson, FundsTransferResponsePayload.class);
    }

    public static String generateRequestId() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int ID_LENGTH = 8;
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder uniqueId = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            uniqueId.append(CHARACTERS.charAt(randomIndex));
        }
        return uniqueId.toString();
    }

    public static BigDecimal sumAmount(List<AgentTranLog> getNotificationHistories) {
        List<BigDecimal> values = new ArrayList<>();
        getNotificationHistories.forEach(transaction -> values.add(BigDecimal.valueOf(Long.parseLong(transaction.getAmount()))));
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal d : values) {
            sum = sum.add(d);
        }
        return sum;
    }

    @Override
    public boolean validateGruppAgentDetailsPayload(String token, GruppAgentDetailsPayload requestPayload) {
        StringJoiner rawString = new StringJoiner("|");
        rawString.add(gruppSeccretKey);
        rawString.add(requestPayload.getIdentifier().trim());
        rawString.add(requestPayload.getPhoneNumber().trim());
        rawString.add(requestPayload.getBvn().trim());
        String hashString = genericService.hash(rawString.toString(), "SHA512");
        return requestPayload.getHash().equalsIgnoreCase(hashString);
    }

    @Override
    public String processGruppAgentDetails(String token, GruppAgentDetailsPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Grupp Agent Details", token, requestJson, "API Request", "INFO", requestPayload.getIdentifier());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Agent Details", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Details", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingPhoneNumber(requestPayload.getPhoneNumber(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Agent Details", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Details", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Agent Details", token, messageSource.getMessage("appMessages.agent.mobile.notexist", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Details", "", channel, messageSource.getMessage("appMessages.agent.mobile.notexist", new Object[0], Locale.ENGLISH), requestPayload.getIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.mobile.notexist", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Agent Details", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Details", "", channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Agent details exist. Return response
            AccionAgentResponsePayload successResponse = new AccionAgentResponsePayload();
            successResponse.setAccountNumber(agent.getAgentAccountNumber());
            successResponse.setAgentName(agent.getAgentName());
            successResponse.setPhoneNumber(agent.getAgentMobile());
            successResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            successResponse.setBankCode("090134");
            return gson.toJson(successResponse);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Grupp Agent Details", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getIdentifier());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public boolean validateGruppDisbursementPayload(String token, GruppDisbursementRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner("|");
        rawString.add(gruppSeccretKey);
        rawString.add(requestPayload.getCustomerBillerId().trim());
        rawString.add(requestPayload.getUniqueIdentifier().trim());
        rawString.add(requestPayload.getAmount().trim());
        String hashString = genericService.hash(rawString.toString(), "SHA512");
//        return requestPayload.getHash().equalsIgnoreCase(hashString);
        return true;
    }

    @Override
    public String processGruppCableTV(String token, GruppDisbursementRequestPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Grupp Cable TV", token, requestJson, "API Request", "INFO", requestPayload.getUniqueIdentifier());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Cable TV", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getCustomerPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cable TV", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getCustomerPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getTerminalId(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Cable TV", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cable TV", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Cable TV", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cable TV", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Cable TV", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cable TV", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check the biller information provided
            Biller biller = agencyRepository.getCableTVBillerUsingAmount(requestPayload.getProductCategory(), cableTVVendor, requestPayload.getAmount());
            if (biller == null) {
                //Log the error
                genericService.generateLog("Grupp Cable TV", token, messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Cable TV", "", channel, messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH), requestBy, 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Generate the cable tv request payload to Omnix
            OmnixRequestPayload cabletvRequestPayload = new OmnixRequestPayload();
            cabletvRequestPayload.setMobileNumber(agent.getAgentMobile());
            cabletvRequestPayload.setDebitAccount(agent.getAgentAccountNumber());
            cabletvRequestPayload.setSmartCard(requestPayload.getCustomerBillerId());
            cabletvRequestPayload.setBillerId(String.valueOf(biller.getId()));
            cabletvRequestPayload.setRequestId(requestPayload.getUniqueIdentifier());
            cabletvRequestPayload.setToken(token);
            cabletvRequestPayload.setHash(genericService.hashCableTVValidationRequest(cabletvRequestPayload));

            //Create the request payload JSON
            String cabletvRequestJson = gson.toJson(cabletvRequestPayload);

            //Log the request payload
            genericService.generateLog("Grupp Cable TV", token, cabletvRequestJson, "OMNIX Request", "INFO", requestPayload.getUniqueIdentifier());

            //Call the Cable TV microservices
            String cableTVResponseJson = cabletvService.cableSubscription(token, cabletvRequestJson);

            //Log the response payload
            genericService.generateLog("Grupp Cable TV", token, cableTVResponseJson, "OMNIX Response", "INFO", requestPayload.getUniqueIdentifier());
            CableTVResponsePayload cableTvResponsePayload = gson.fromJson(cableTVResponseJson, CableTVResponsePayload.class);

            //Check if the transaction failed
            if (!cableTvResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                responsePayload.setStatus("FAILED");
                OmnixResponsePayload errorResponse = gson.fromJson(cableTVResponseJson, OmnixResponsePayload.class);
                responsePayload.setMessage(errorResponse.getResponseMessage());
                return gson.toJson(responsePayload);
            }

            //The transaction was successful
            responsePayload.setStatus("SUCCESS");
            responsePayload.setTransactionReference(requestPayload.getUniqueIdentifier());
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Grupp Cable TV", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getUniqueIdentifier());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String processGruppElectricityBill(String token, GruppDisbursementRequestPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Grupp Electricity Bill", token, requestJson, "API Request", "INFO", requestPayload.getUniqueIdentifier());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Electricity Bill", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getCustomerPhoneNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Electricity Bill", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getCustomerPhoneNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getTerminalId(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Electricity Bill", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Electricity Bill", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Electricity Bill", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Electricity Bill", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Electricity Bill", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Electricity Bill", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check the biller information provided
            List<Biller> billers = agencyRepository.getElectricityBiller(electricityVendor, requestPayload.getProduct());
            if (billers == null) {
                //Log the error
                genericService.generateLog("Grupp Electricity Bill", token, messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Electricity Bill", "", channel, messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH), requestBy, 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.biller.notexist", new Object[]{requestPayload.getProductCategory()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            Long billerId = 0L;
            for (Biller b : billers) {
                if (b.getPackageName().toUpperCase(Locale.ENGLISH).contains(requestPayload.getProduct().toUpperCase(Locale.ENGLISH))
                        && b.getPackageName().toUpperCase(Locale.ENGLISH).contains(requestPayload.getProductCategory().toUpperCase(Locale.ENGLISH))) {
                    billerId = b.getId();
                }
            }

            //Generate the cable tv request payload to Omnix
            OmnixRequestPayload electricityRequestPayload = new OmnixRequestPayload();
            electricityRequestPayload.setMobileNumber(agent.getAgentMobile());
            electricityRequestPayload.setDebitAccount(agent.getAgentAccountNumber());
            electricityRequestPayload.setMeterNumber(requestPayload.getCustomerBillerId());
            electricityRequestPayload.setBillerId(String.valueOf(billerId));
            electricityRequestPayload.setAmount(requestPayload.getAmount());
            electricityRequestPayload.setRequestId(requestPayload.getUniqueIdentifier());
            electricityRequestPayload.setToken(token);
            electricityRequestPayload.setHash(genericService.hashElectricityValidationRequest(electricityRequestPayload));

            //Create the request payload JSON
            String electricityRequestJson = gson.toJson(electricityRequestPayload);

            //Log the request payload
            genericService.generateLog("Grupp Electricity Bill", token, electricityRequestJson, "OMNIX Request", "INFO", requestPayload.getUniqueIdentifier());

            //Call the Electricity microservices
            String electricityResponseJson = electricityService.electricityPayment(token, electricityRequestJson);

            //Log the request payload
            genericService.generateLog("Grupp Electricity Bill", token, electricityResponseJson, "OMNIX Response", "INFO", requestPayload.getUniqueIdentifier());
            ElectricityResponsePayload electricityResponsePayload = gson.fromJson(electricityResponseJson, ElectricityResponsePayload.class);

            //Check if the transaction failed
            if (!electricityResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                responsePayload.setStatus("FAILED");
                OmnixResponsePayload errorResponse = gson.fromJson(electricityResponseJson, OmnixResponsePayload.class);
                responsePayload.setMessage(errorResponse.getResponseMessage());
                return gson.toJson(responsePayload);
            }

            //Transaction succeed at this point
            responsePayload.setStatus("SUCCESS");
            responsePayload.setToken(electricityResponsePayload.getToken());
            responsePayload.setTransactionReference(requestPayload.getUniqueIdentifier());

            //Log the request payload
            genericService.generateLog("Grupp Electricity Bill", token, electricityResponseJson, "API Response", "INFO", requestPayload.getUniqueIdentifier());
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Grupp Electricity Bill", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getUniqueIdentifier());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String processGruppFundsTransfer(String token, GruppDisbursementRequestPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Grupp Funds Transfer", token, requestJson, "API Request", "INFO", requestPayload.getUniqueIdentifier());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Funds Transfer", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Funds Transfer", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getTerminalId(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Funds Transfer", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Funds Transfer", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Funds Transfer", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            log.info("found app user ------->>>> {}", appUser.toString());

            Branch branch = genericService.getBranchUsingBranchCode("NG0010068"); //Defaulted to Digital Branch
            log.info("found branch ------->>>> {}", branch.toString());

            //Get the beneficiary bank details
            Banks bank = agencyRepository.getBankUsingBankCode(requestPayload.getBankCode());
            if (bank == null) {
                //Log the error
                genericService.generateLog("Grupp Funds Transfer", token, messageSource.getMessage("appMessages.bank.noexist", new Object[]{requestPayload.getBankCode()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.bank.noexist", new Object[]{requestPayload.getBankCode()}, Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.bank.noexist", new Object[]{requestPayload.getBankCode()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the transfer is internal to Accion or NIP
            FundsTransferResponsePayload localTransferResponsePayload;// = new FundsTransferResponsePayload();
            if ("090134".equalsIgnoreCase(bank.getBankCode()) || "Accion Microfinance Bank".equalsIgnoreCase(bank.getBank())) {
                log.info("------------------internal transfer----------------");
                //Get the Accion account number of the beneficiary
                OmnixRequestPayload accountDetailsRequest = new OmnixRequestPayload();
                accountDetailsRequest.setAccountNumber(requestPayload.getCustomerBillerId());
                accountDetailsRequest.setRequestId(requestPayload.getUniqueIdentifier());
                accountDetailsRequest.setToken(token);
                accountDetailsRequest.setHash(genericService.hashAccountDetailsValidationRequest(accountDetailsRequest));
                String accountDetailsJson = gson.toJson(accountDetailsRequest);

                String accountDetailsResponseJson = accountService.accountDetails(token, accountDetailsJson);
                log.info("Account balance raw response for test ------------------------------------------->>>> {}", accountDetailsResponseJson);
                AccountDetailsResponsePayload accountDetailsResponse = gson.fromJson(accountDetailsResponseJson, AccountDetailsResponsePayload.class);

                if (!accountDetailsResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    responsePayload.setStatus("FAILED");
                    OmnixResponsePayload errorResponse = gson.fromJson(accountDetailsResponseJson, OmnixResponsePayload.class);
                    responsePayload.setMessage(errorResponse.getResponseMessage());
                    return gson.toJson(responsePayload);
                }

                //Generate the funds transfer request payload to Omnix
                LocalTransferWithChargesPayload localFTRequestPayload = new LocalTransferWithChargesPayload();
                localFTRequestPayload.setAmount(requestPayload.getAmount());
                localFTRequestPayload.setAuthorizer("");
                localFTRequestPayload.setBranchCode("NG0010068"); //Defaulted to Digital Branch
                localFTRequestPayload.setUseCommissionAsChargeType(true);
                //Set the charges
                List<ChargeTypes> charges = new ArrayList<>();
                ChargeTypes newCharge = new ChargeTypes();
                newCharge.setChargeAmount(String.valueOf(TRANSFER_FEE));
                newCharge.setChargeType("AGBFTCHG");
                charges.add(newCharge);

                //Set the narration
                StringBuilder narration = new StringBuilder(0);
                narration.append("FT TO ").append(requestPayload.getCustomerBillerId());
//                String prefix = requestPayload.getUniqueIdentifier().substring(0, 3);
//                String subString = requestPayload.getUniqueIdentifier().substring(28, requestPayload.getUniqueIdentifier().length());

                localFTRequestPayload.setChargeTypes(charges);
                localFTRequestPayload.setCreditAccount(requestPayload.getCustomerBillerId());
                localFTRequestPayload.setDebitAccount(agent.getAgentAccountNumber());
                localFTRequestPayload.setInputter("");
                localFTRequestPayload.setMobileNumber(agent.getAgentMobile());
                localFTRequestPayload.setNarration(narration.toString());
                localFTRequestPayload.setNoOfAuthorizer("0");
                localFTRequestPayload.setRequestId(requestPayload.getUniqueIdentifier()); //prefix + subString
                localFTRequestPayload.setTransType(genericService.getTransactionType(channel, "NIP"));
                localFTRequestPayload.setToken(token);
                localFTRequestPayload.setHash(genericService.hashLocalFundsTransferWithChargesValidationRequest(localFTRequestPayload));
                //Create the request payload JSON
                String localTransferRequestJson = gson.toJson(localFTRequestPayload);

                //Log the request payload
                genericService.generateLog("Grupp Funds Transfer", token, localTransferRequestJson, "OMNIX Request", "INFO", requestPayload.getUniqueIdentifier());

                //Call the Funds Transfer microservices
//                String localTransferResponseJson = ftService.localTransferWithCharges(token, localTransferRequestJson);
                //Call the Funds Transfer microservices locally
                String localTransferResponseJson = internalLocalTransferWithInternalDebitCharges(localFTRequestPayload, appUser, branch, token);

                //Log the request payload
                genericService.generateLog("Grupp Funds Transfer", token, localTransferResponseJson, "OMNIX Response", "INFO", requestPayload.getUniqueIdentifier());

                localTransferResponsePayload = gson.fromJson(localTransferResponseJson, FundsTransferResponsePayload.class);

                //Check if the transaction failed at this point
                if (localTransferResponsePayload == null || localTransferResponsePayload.getResponseCode() == null || !localTransferResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    responsePayload.setStatus("FAILED");
                    OmnixResponsePayload errorResponse = gson.fromJson(localTransferResponseJson, OmnixResponsePayload.class);
                    responsePayload.setMessage(errorResponse.getResponseMessage());
                    return gson.toJson(responsePayload);
                }

                //Transaction is successful at this point
                responsePayload.setStatus("SUCCESS");
                responsePayload.setTransactionReference(requestPayload.getUniqueIdentifier());
                //Log the request payload
                genericService.generateLog("Grupp Funds Transfer", token, responsePayload.getStatus(), "OMNIX Response", "INFO", responsePayload.getTransactionReference());
                return gson.toJson(responsePayload);
            } else {
                log.info("this is an INTER BANK TRANSACTION, now lets check agent's balance");
                String agentBalance = checkAccountBalance(agent.getAgentAccountNumber(), token);
                AccountBalanceResponsePayload accountResponse = gson.fromJson(agentBalance, AccountBalanceResponsePayload.class);
                log.info("account balance response  ------------->>>>>>> {}", accountResponse);
                String availableBalance = accountResponse.getAvailableBalance().replace(",", "");
                if (Double.parseDouble(requestPayload.getAmount()) > Double.parseDouble(availableBalance)) {
                    log.info("this is an overdraft");
                    responsePayload.setStatus("FAILED");
                    responsePayload.setMessage("Insufficient funds");

                    return gson.toJson(responsePayload);
                }

                //The transaction is Interbank (NIP)
                String narration = "FT TO " + requestPayload.getCustomerBillerId() + " " + requestPayload.getUniqueIdentifier();

                NIPTransferPayload nipRequestPayload = new NIPTransferPayload();
                nipRequestPayload.setMobileNumber(agent.getAgentMobile());
                nipRequestPayload.setDebitAccount(agent.getAgentAccountNumber());
                nipRequestPayload.setBeneficiaryAccount(requestPayload.getCustomerBillerId());
                nipRequestPayload.setBeneficiaryAccountName("NA"); //Pseudo name
                nipRequestPayload.setBeneficiaryBankCode(requestPayload.getBankCode());
                nipRequestPayload.setBeneficiaryKycLevel("1"); //Pseudo name
                nipRequestPayload.setBeneficiaryBvn("22200000065"); //Pseudo BVN
                nipRequestPayload.setNameEnquiryRef("NA"); //Pseudo name
                nipRequestPayload.setAmount(requestPayload.getAmount());
                nipRequestPayload.setNarration(narration);
                nipRequestPayload.setRequestId(requestPayload.getUniqueIdentifier()); //prefix + subString
                nipRequestPayload.setHash("47848SDMDVNFWJUU43I4N33");

                String nipResponseJson = processNIPTransfer(token, nipRequestPayload, agent);

                NIPResponsePayload nipResponsePayload = gson.fromJson(nipResponseJson, NIPResponsePayload.class);

                //Check if the transaction failed at this point
                if (nipResponsePayload != null && nipResponsePayload.getResponseCode() != null && nipResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    //Transaction is successful at this point
                    responsePayload.setStatus("SUCCESS");
                    responsePayload.setTransactionReference(requestPayload.getUniqueIdentifier());
                    return gson.toJson(responsePayload);
                }
                responsePayload.setStatus("FAILED");
                OmnixResponsePayload errorResponse = gson.fromJson(nipResponseJson, OmnixResponsePayload.class);
                responsePayload.setMessage(errorResponse.getResponseMessage());
                return gson.toJson(responsePayload);

            }
        } catch (JsonProcessingException | JsonSyntaxException | NumberFormatException | NoSuchMessageException ex) {
            //Log the response
            genericService.generateLog("Grupp Funds Transfer", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getUniqueIdentifier());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    private String processInterbankTransfer(String token, NIPTransferPayload requestPayload, AccionAgent agent, FundsTransferResponsePayload fundsTransferResponsePayload) {
        log.info("in the method to make the transfer");
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        String requestBy = this.jwtToken.getUsernameFromToken(token);
        String channel = this.jwtToken.getChannelFromToken(token);
        String requestJson = this.gson.toJson(requestPayload);
        String pin = "xxxxxx";
        String imei = "xxxxxx";
        requestPayload.setAmount(requestPayload.getAmount().replaceAll(",", ""));

        this.genericService.generateLog("NIP", token, requestJson.replaceAll(pin, "******").replaceAll(imei, "******"), "API Request", "INFO", requestPayload.getRequestId());
        try {

            AppUser appUser = this.ftRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                this.genericService.generateLog("NIP", token, this.messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                this.genericService.createUserActivity(requestPayload.getDebitAccount(), "NIP", requestPayload.getAmount(), channel, this.messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(this.messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return this.gson.toJson(errorResponse);
            }

            NIPNameEnquiryPayload nameEnquiryPayload = new NIPNameEnquiryPayload();
            nameEnquiryPayload.setBeneficiaryAccount(requestPayload.getBeneficiaryAccount());
            nameEnquiryPayload.setBeneficiaryBankCode(requestPayload.getBeneficiaryBankCode());
            String nameEnquiryResponseJson = processNIPNameEnquiry(token, nameEnquiryPayload);
            log.info("Name from name enquiry -------->>> {}", nameEnquiryResponseJson);
            NIPNameEnquiryResponsePayload nipNameEnquiryResponsePayload = this.gson.fromJson(nameEnquiryResponseJson, NIPNameEnquiryResponsePayload.class);
            if (nipNameEnquiryResponsePayload == null || !nipNameEnquiryResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                this.genericService.generateLog("NIP Name Enquiry", token, nameEnquiryResponseJson, "API Response", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(this.messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
                return this.gson.toJson(errorResponse);
            }

            FundsTransfer newFT = new FundsTransfer();
            newFT.setAmount(requestPayload.getAmount());
            newFT.setAppUser(appUser);
            newFT.setCreditCurrency("NGN");
            newFT.setCreatedAt(LocalDateTime.now());
            newFT.setCreditAccount(requestPayload.getBeneficiaryAccount());
            newFT.setCreditAccountName(nipNameEnquiryResponsePayload.getBeneficiaryAccountName());
            newFT.setCreditAccountName(nipNameEnquiryResponsePayload.getBeneficiaryAccountName());
            newFT.setCreditAccountKyc(nipNameEnquiryResponsePayload.getBeneficiaryKycLevel());
//            newFT.setCustomer(customer);
            newFT.setDebitAccount(agent.getAgentAccountNumber());
            newFT.setDebitAccountName(agent.getAgentName());
            newFT.setDebitAccountKyc("3");
            newFT.setDebitCurrency("NGN");
            newFT.setDestinationBank("ACCION");
            newFT.setGateway("NIBSS");
            newFT.setMobileNumber(requestPayload.getMobileNumber());
            newFT.setNarration(requestPayload.getNarration());
            newFT.setRequestId(requestPayload.getRequestId());
            newFT.setStatus("Pending");
            newFT.setSourceBank("ACCION");
            newFT.setT24TransRef("");
            newFT.setTimePeriod(this.genericService.getTimePeriod());
            newFT.setTransType("ATI");
            newFT.setDebitAccountType("Individual");
            newFT.setDestinationBankCode(nipNameEnquiryResponsePayload.getBeneficiaryBankCode());
            newFT.setCreditAccountType("");
            FundsTransfer createFT = this.ftRepository.createFundsTransfer(newFT);

            FTSingleCreditResponse nipResponse = new FTSingleCreditResponse();

            // call NIP APIs to send out the funds
            FTSingleCreditRequest nipRequest = new FTSingleCreditRequest();
            nipRequest.setNameEnquiryRef(nipNameEnquiryResponsePayload.getNameEnquiryRef());
            nipRequest.setDestinationInstitutionCode(requestPayload.getBeneficiaryBankCode());
            nipRequest.setChannelCode(2);
            nipRequest.setBeneficiaryAccountName(requestPayload.getBeneficiaryAccountName());
            nipRequest.setBeneficiaryAccountNumber(requestPayload.getBeneficiaryAccount());

            String benBvn = nipNameEnquiryResponsePayload.getBeneficiaryBvn() == null ? "22000000035" : nipNameEnquiryResponsePayload.getBeneficiaryBvn();
            nipRequest.setBeneficiaryBankVerificationNumber(benBvn);

            String benKycLevel = nipNameEnquiryResponsePayload.getBeneficiaryKycLevel() == null ? "3" : nipNameEnquiryResponsePayload.getBeneficiaryKycLevel();
            nipRequest.setBeneficiaryKYCLevel(benKycLevel);

            nipRequest.setOriginatorAccountName(createFT.getDebitAccountName());
            nipRequest.setOriginatorAccountNumber(createFT.getDebitAccount());
            String bvn = "22000000035";
            nipRequest.setOriginatorBankVerificationNumber(bvn);
            nipRequest.setOriginatorKYCLevel(createFT.getDebitAccountKyc());
            nipRequest.setTransactionLocation("");
            nipRequest.setNarration(requestPayload.getNarration() == null ? "FUNDS TRANSFER" : requestPayload.getNarration());
            nipRequest.setPaymentReference(fundsTransferResponsePayload.getT24TransRef());
            nipRequest.setAmount(requestPayload.getAmount());
            nipRequest.setSessionId(generateSessionId());
            nipRequest.setNameEnquiryRef(nipNameEnquiryResponsePayload.getNameEnquiryRef());
            nipRequest.setBeneficiaryAccountName(nipNameEnquiryResponsePayload.getBeneficiaryAccountName());

            if (ftEnvironment.equalsIgnoreCase("Production")) {
                nipResponse = nipUtil.doNipFundsTransfer(nipRequest);

                return gson.toJson(nipResponse);
            } else {
                nipResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                nipResponse.setResponseDecription("SUCCESS");
                nipResponse.setPaymentReference("TEST123456");
                nipResponse.setResponseMessage("Test Transaction is successful");
            }
            if (nipResponse.getResponseCode() != null && nipResponse.getResponseCode().trim().equals("00")) {
                createFT.setStatus("SUCCESS");
                createFT.setT24TransRef(fundsTransferResponsePayload.getT24TransRef());
                ftRepository.updateFundsTransfer(createFT);
                genericService.generateLog("NIP", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
                genericService.createUserActivity(agent.getAgentAccountNumber(), "NIP", requestPayload.getAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');
                return gson.toJson(nipResponse);
            } else {
                if (nipResponse.getResponseCode() != null && (nipResponse.getResponseCode().trim().equals("09")
                        || nipResponse.getResponseCode().trim().equals("96")
                        || nipResponse.getResponseCode().trim().equals("97"))) {
                    // do not reverse transaction. This is to save the bank in case of a successful transaction in other bank.
                    createFT.setStatus("FAILED");
                    ftRepository.updateFundsTransfer(createFT);
                }

                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(this.messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH));
                return this.gson.toJson(errorResponse);

            }

        } catch (Exception ex) {
            this.genericService.generateLog("Pay Attitude Withdrawal", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return this.gson.toJson(errorResponse);
        }

    }

    private String generateSessionId() {
        String institutionCode = "090134";
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
        df.applyPattern("####");
        double random = Math.random() * 1.0E18D;
        String randomNumber = df.format(random).substring(0, 12);

        SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
        String date = formatter.format(new Date());

        String res = institutionCode.concat(date).concat(randomNumber);
        return res;
    }

    public String processNIPNameEnquiry(String token, NIPNameEnquiryPayload requestPayload) {
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        String response = "";
        String channel = this.jwtToken.getChannelFromToken(token);
        String requestJson = this.gson.toJson(requestPayload, NIPNameEnquiryPayload.class);
        this.genericService.generateLog("NIP Name Enquiry", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            NIPPayload nameEnquiry = new NIPPayload();
            nameEnquiry.setAccountNumber(requestPayload.getBeneficiaryAccount());
            nameEnquiry.setChannelCode("1");
            nameEnquiry.setDestinationInstitutionCode(requestPayload.getBeneficiaryBankCode());
            log.info("name enquiry ------------->>>>>>> {}", nameEnquiry);
//            NIPPayload responsePayload = new NIPPayload();
//            if (this.omnixEnvironment.equalsIgnoreCase("production")) {
            String ofsRequest = this.gson.toJson(nameEnquiry);
//            String middlewareResponse = this.genericService.postToMiddleware("/nip/nameEnquiry", ofsRequest);
            NESingleRequest request = new NESingleRequest();
            request.setAccountNumber(requestPayload.getBeneficiaryAccount());
            request.setChannelCode("1");
            request.setDestinationInstitutionCode(requestPayload.getBeneficiaryBankCode());
            NESingleResponse oNESingleResponse = nipUtil.doNipNameEnquiry(request);
//            String middlewareResponse = gson.toJson(nipResponse);
            log.info("raw response from name enquiry =====>>>> {}", gson.toJson(oNESingleResponse));
            if (oNESingleResponse == null || oNESingleResponse.getSessionId() == null || oNESingleResponse.getAccountName() == null) {
                genericService.generateLog("NIP Name Enquiry", token, gson.toJson(oNESingleResponse), "API Response", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            if (!oNESingleResponse.getResponseCode().equals("00")) {
                genericService.generateLog("NIP Name Enquiry", token, gson.toJson(oNESingleResponse), "API Response", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
//            String validationResponse = this.genericService.validateT24Response(middlewareResponse);
//            if (validationResponse != null) {
//                this.genericService.generateLog("NIP Name Enquiry", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());
//                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                errorResponse.setResponseMessage(middlewareResponse);
//                return this.gson.toJson(errorResponse);
//            }
//            responsePayload = this.gson.fromJson(middlewareResponse, NIPPayload.class);
//            if (responsePayload.getSessionId() == null || responsePayload.getAccountName() == null) {
//                this.genericService.generateLog("NIP Name Enquiry", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());
//                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                errorResponse.setResponseMessage(this.messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
//                return this.gson.toJson(errorResponse);
//            }
//            if (!responsePayload.getResponseCode().equals("00")) {
//                this.genericService.generateLog("NIP Name Enquiry", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());
//                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                errorResponse.setResponseMessage(this.messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
//                return this.gson.toJson(errorResponse);
//            }
//            }

            NIPNameEnquiryResponsePayload nameEnquiryResponse = new NIPNameEnquiryResponsePayload();
            nameEnquiryResponse.setBeneficiaryAccountName(oNESingleResponse.getAccountName());
            nameEnquiryResponse.setBeneficiaryBankCode(oNESingleResponse.getDestinationInstitutionCode());
            String beneficiaryBVN = "22222222222";
            try {
                if (oNESingleResponse.getBankVerificationNumber().length() == 11) {
                    beneficiaryBVN = oNESingleResponse.getBankVerificationNumber();
                }
            } catch (Exception exception) {
            }
            nameEnquiryResponse.setBeneficiaryBvn(beneficiaryBVN);
            String beneficiaryKyc = "1";
            try {
                if (oNESingleResponse.getKycLevel().length() == 1) {
                    beneficiaryKyc = oNESingleResponse.getKycLevel();
                }
            } catch (Exception ignored) {
            }
            nameEnquiryResponse.setBeneficiaryKycLevel(beneficiaryKyc);
            nameEnquiryResponse.setNameEnquiryRef(oNESingleResponse.getSessionId());
            nameEnquiryResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
            String responseJson = this.gson.toJson(nameEnquiryResponse);
            this.genericService.generateLog("NIP Name Enquiry", token, responseJson, "API Response", "INFO", requestPayload.getRequestId());
            this.genericService.createUserActivity(requestPayload.getBeneficiaryAccount(), "NIP Name Enquiry", "", channel, "Success", "", 'S');
            return responseJson;
        } catch (Exception ex) {
            this.genericService.generateLog("NIP Name Enquiry", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return this.gson.toJson(errorResponse);
        }
    }

    @Override
    public String processGruppAirtime(String token, GruppDisbursementRequestPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        genericService.generateLog("Grupp Airtime", token, requestJson, "API Request", "INFO", requestPayload.getUniqueIdentifier());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Airtime", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Airtime", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getTerminalId(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Airtime", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Airtime", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Airtime", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Airtime", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Airtime", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getUniqueIdentifier());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Airtime", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getUniqueIdentifier(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getTerminalId()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Generate the airtime request payload to Omnix
            OmnixRequestPayload airtimeRequestPayload = new OmnixRequestPayload();
            airtimeRequestPayload.setMobileNumber(agent.getAgentMobile());
            airtimeRequestPayload.setDebitAccount(agent.getAgentAccountNumber());
            airtimeRequestPayload.setThirdPartyMobileNumber(requestPayload.getCustomerBillerId());
            airtimeRequestPayload.setThirdPartyTelco(requestPayload.getProductCategory().toUpperCase(Locale.ENGLISH));
            airtimeRequestPayload.setAmount(requestPayload.getAmount());
            airtimeRequestPayload.setRequestId(requestPayload.getUniqueIdentifier());
            airtimeRequestPayload.setToken(token);
            airtimeRequestPayload.setHash(genericService.hashAirtimeValidationRequest(airtimeRequestPayload));

            //Create the request payload JSON
            String airtimeRequestJson = gson.toJson(airtimeRequestPayload);

            //Log the request payload
            genericService.generateLog("Grupp Airtime", token, airtimeRequestJson, "OMNIX Request", "INFO", requestPayload.getUniqueIdentifier());

            //Call the airtime microservices
            String airtimeResponseJson = airtimeService.airtimeOthers(token, airtimeRequestJson);

            //Log the request payload
            genericService.generateLog("Grupp Airtime", token, airtimeResponseJson, "OMNIX Response", "INFO", requestPayload.getUniqueIdentifier());
            AirtimeResponsePayload airtimeResponsePayload = gson.fromJson(airtimeResponseJson, AirtimeResponsePayload.class);

            //Check if the transaction failed
            if (!airtimeResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                responsePayload.setStatus("FAILED");
                OmnixResponsePayload errorResponse = gson.fromJson(airtimeResponseJson, OmnixResponsePayload.class);
                responsePayload.setMessage(errorResponse.getResponseMessage());
                return gson.toJson(responsePayload);
            }
            log.info("---------------AIRTIME PURCHASE WAS SUCCESSFUL----------------");
            //Transaction was successful at this point
            responsePayload.setStatus("SUCCESS");
            responsePayload.setTransactionReference(requestPayload.getUniqueIdentifier());
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Grupp Airtime", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getUniqueIdentifier());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Async
    public FundsTransferResponsePayload posNotificationSplit(String agentAccountNumber, String t24TransRef, String requestBy, String amount, String accionFee, String narration, String channel, AppUser appUser, Branch branch, String customerName) throws Exception {
        log.info("------------------------ About to split from split account to Agent account ------------------");
        FundsTransfer ft = agencyRepository.getFundsTransferUsingTransRef(t24TransRef);
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJydWJ5eCIsInJvbGVzIjoiW1JVQllYLCBDUkVESVRfQlVSRUFVX1ZBTElEQVRJT04sIEFDQ09VTlRfQkFMQU5DRSwgU01TX05PVElGSUNBVElPTiwgVFJBTlNBQ1RJT05fRU1BSUxfQUxFUlQsIExPQU5fT0ZGRVJfRU1BSUxfQUxFUlRdIiwiYXV0aCI6IlZSNmtONHdHamFDYVZtNzBpVkR1WEE9PSIsIkNoYW5uZWwiOiJNb2JpbGUiLCJJUCI6IjA6MDowOjA6MDowOjA6MSIsImlzcyI6IkFjY2lvbiBNaWNyb2ZpbmFuY2UgQmFuayIsImlhdCI6MTY1NTQ2ODI2MywiZXhwIjo2MjUxNDI4NDQwMH0.Pw0TKpXLbV1ny59TD2RXfclAHn_ZdYYDuyCNTQCZFCI";
        FundsTransferResponsePayload ftResponsePayload = new FundsTransferResponsePayload();
        if (ft != null) {
            //The transaction was successful. Initiate the spliting of the fees
            LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
            String transRef = generateRequestId();
            ftRequestPayload.setMobileNumber(ft.getMobileNumber());
            ftRequestPayload.setDebitAccount(gruppSplitAccount);
            ftRequestPayload.setCreditAccount(agentAccountNumber);
            ftRequestPayload.setAmount(amount);
            ftRequestPayload.setNarration(narration);
            ftRequestPayload.setTransType(genericService.getTransactionType(channel, "LOCAL FT"));
            ftRequestPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
            ftRequestPayload.setInputter(requestBy + "-" + ft.getMobileNumber() + "-" + t24TransRef);
            ftRequestPayload.setAuthorizer(requestBy + "-" + ft.getMobileNumber());
            ftRequestPayload.setNoOfAuthorizer("0");
            ftRequestPayload.setRequestId(transRef);
            ftRequestPayload.setToken(token);
            ftRequestPayload.setHash("1234567890");

            //Create the string request payload JSON
            String ftRequestJson = gson.toJson(ftRequestPayload);

            //Log the request payload
            genericService.generateLog("Grupp Fee Splitting", token, ftRequestJson, "OMNIX Request", "INFO", transRef);

            //Call the funds transfer microservices
            String ftResponseJson = internalLocalTransfer(ftRequestPayload, appUser, branch, token, customerName);

            //Log the request payload
            genericService.generateLog("Grupp Fee Splitting", token, ftResponseJson, "OMNIX Response", "INFO", transRef);

            ftResponsePayload = gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
            log.info("response from log ------->>>>>> {}", ftResponsePayload);

            //Check if the transaction failed
            if (!ftResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                OmnixResponsePayload errorResponse = gson.fromJson(ftResponseJson, OmnixResponsePayload.class);
                ft.setStatus("FAILED");
                ft.setFailureReason("FEE SPLITTING FAILED." + errorResponse.getResponseMessage());

                agencyRepository.updateFundsTransfer(ft);
            } else {
                //Update the status
                ft.setStatus("SUCCESS");
                agencyRepository.updateFundsTransfer(ft);
            }
        }
        return ftResponsePayload;
    }

    private String internalLocalTransferWithInternalDebitCharges(LocalTransferWithChargesPayload requestPayload, AppUser appUser, Branch branch, String token) throws JsonProcessingException {

        log.info("request payload for internal Local Transfer With Internal Debit Charges ---------->>>> {}", requestPayload);
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();

        Customer debitCustomer = ftRepository.getCustomerUsingMobileNumber(requestPayload.getMobileNumber());
        log.info("found debit customer ---->>> {}", debitCustomer);

        log.info("About to check debit account");

        log.info("wawuu.... moved");
        Account creditAccount = ftRepository.getAccountUsingAccountNumber(requestPayload.getCreditAccount());
        log.info("credit account ------->>> {}", creditAccount);

        String requestBy = jwtToken.getUsernameFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);

        String ofsBase;

        log.info("setting payload for funds transfer");

        FundsTransfer newFT = new FundsTransfer();
//        newFT.setCustomer(debitCustomer);
        newFT.setAmount(requestPayload.getAmount());
        newFT.setAppUser(appUser);
        newFT.setBranch(branch);
        newFT.setCreditCurrency("NGN");
        newFT.setCreatedAt(LocalDateTime.now());
        newFT.setCreditAccount(requestPayload.getCreditAccount());
        newFT.setCreditAccountName(requestPayload.getCreditAccount().matches("[0-9]{10}")
                ? Objects.requireNonNull(creditAccount).getCustomer().getLastName() + ", " + creditAccount.getCustomer().getOtherName()
                : requestPayload.getCreditAccount().matches("^PL([5-6])([0-9]{4})$") ? "P & L" : "INTERNAL ACCOUNT");
        newFT.setCreditAccountKyc(requestPayload.getCreditAccount().matches("[0-9]{10}") ? creditAccount.getCustomer().getKycTier() : "3");
//        newFT.setCustomer(requestPayload.getCreditAccount().matches("[0-9]{10}")
//                //                ? creditAccount.getCustomer() : requestPayload.getDebitAccount().matches("[0-9]{10}") ? debitAccount.getCustomer() : null);
//                ? creditAccount.getCustomer() : requestPayload.getDebitAccount().matches("[0-9]{10}") ? creditAccount.getCustomer() : null);
        newFT.setDebitAccount(requestPayload.getDebitAccount());
        newFT.setDebitAccountName(requestPayload.getDebitAccount().matches("[0-9]{10}")
                //                ? debitAccount.getCustomer().getLastName() + ", " + debitAccount.getCustomer().getOtherName()
                ? creditAccount.getCustomer().getLastName() + ", "
                : requestPayload.getCreditAccount().matches("^PL([5-6])([0-9]{4})$") ? "P & L" : "INTERNAL ACCOUNT");
//        newFT.setDebitAccountKyc(requestPayload.getDebitAccount().matches("[0-9]{10}") ? debitAccount.getCustomer().getKycTier() : "3");
        newFT.setDebitAccountKyc(requestPayload.getDebitAccount().matches("[0-9]{10}") ? creditAccount.getCustomer().getKycTier() : "3");
        newFT.setDebitCurrency("NGN");
        newFT.setDestinationBank("ACCION");
        newFT.setGateway("ACCION");
        newFT.setMobileNumber(requestPayload.getMobileNumber());
        newFT.setNarration(requestPayload.getNarration());
        newFT.setRequestId(requestPayload.getRequestId());
        newFT.setStatus("PENDING");
        newFT.setSourceBank("ACCION");
        newFT.setT24TransRef("");
        newFT.setTimePeriod(genericService.getTimePeriod());
        newFT.setTransType("ATI");
//        newFT.setDebitAccountType(debitAccount == null ? "SYSTEM" : debitAccount.getCustomer().getCustomerType());
        newFT.setDebitAccountType(creditAccount == null ? "SYSTEM" : creditAccount.getCustomer().getCustomerType());
        newFT.setDestinationBankCode("090134"); //Accion Bank Code
        newFT.setCreditAccountType("S");
        FundsTransfer createFT = ftRepository.createFundsTransfer(newFT);

        log.info("CREATED FUND TRANSFER IN THE DB");
        //Generate Funds Transfer OFS
        String transRef = genericService.generateTransRef("FT");
        StringBuilder narration = new StringBuilder();
        String accountName = "INTERNAL ACCOUNT";

        if (creditAccount != null) {
            accountName = creditAccount.getCustomer().getLastName() + " " + creditAccount.getCustomer().getOtherName();
        }
        narration.append(requestPayload.getNarration())
                .append("/").append(accountName)
                .append("/").append(branch.getBranchName().toUpperCase(Locale.ENGLISH))
                .append(" BCH").append("/")
                .append(transRef);
        ofsBase = genericService.generateFTOFS(transRef, requestPayload.getDebitAccount(), requestPayload.getCreditAccount(),
                requestPayload.getAmount(), narration.toString(), requestPayload.getTransType(), requestPayload.getInputter().equalsIgnoreCase("") ? "grupp-" : requestPayload.getInputter(),
                requestPayload.getAuthorizer().equalsIgnoreCase("") ? requestBy + "-" + requestPayload.getMobileNumber() : requestPayload.getAuthorizer());

        //Check if the charge type is specified
        if (requestPayload.getChargeTypes() != null) {
            log.info("charge type is specified");
            List<ChargeTypes> charges = requestPayload.getChargeTypes();
            System.out.println("Charges: " + charges);
            int index = 1;
            double totalCharges = 0;
            if (requestPayload.isUseCommissionAsChargeType()) {
                for (ChargeTypes chg : charges) {
                    ofsBase = ofsBase.concat(",COMMISSION.TYPE:").concat(String.valueOf(index)).concat(":1::=").concat(chg.getChargeType())
                            .concat(",COMMISSION.CODE:").concat(String.valueOf(index)).concat(":1::=DEBIT PLUS CHARGES").concat(",");
                    ofsBase = ofsBase.concat("COMMISSION.AMT:").concat(String.valueOf(index)).concat(":1::=NGN").concat(chg.getChargeAmount().replace(",", ""));
                    index++;
                    totalCharges += Double.parseDouble(chg.getChargeAmount().replace(",", ""));
                }
            } else {
                for (ChargeTypes chg : charges) {
                    System.out.println("Charge: " + gson.toJson(chg));
                    ofsBase = ofsBase.concat(",CHARGE.TYPE:").concat(String.valueOf(index)).concat(":1::=").concat(chg.getChargeType())
                            .concat(",CHARGE.CODE:").concat(String.valueOf(index)).concat(":1::=DEBIT PLUS CHARGES").concat(",");
                    ofsBase = ofsBase.concat("CHARGE.AMT:").concat(String.valueOf(index)).concat(":1::=NGN").concat(chg.getChargeAmount().replace(",", ""));
                    index++;
                    totalCharges += Double.parseDouble(chg.getChargeAmount().replace(",", ""));
                }
            }
        } else {
            ofsBase = ofsBase.concat(",COMMISSION.CODE::=WAIVE");
        }

        log.info("-------------Charge type not specified-----------");
        log.info("----------About to post to t24-----------");

        String ofsRequest = ftVersion.trim() + requestPayload.getNoOfAuthorizer() + "," + userCredentials
                + "/" + createFT.getBranch().getBranchCode() + ",," + ofsBase;
        String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
        String middlewareResponse = genericService.postToT24(ofsRequest);

        System.out.println("Middleware - Response: " + middlewareResponse);

        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
        String validationResponse = genericService.validateT24Response(middlewareResponse);
        System.out.println("Validation Response: " + validationResponse);
        if (validationResponse != null) {
            if (middlewareResponse.toUpperCase(Locale.ENGLISH).contains("WITHDRAWL MAKES A/C BAL LESS THAN MIN BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("A/C BALANCE STILL LESS THAN MINIMUM BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("UNAUTHORISED OVERDRAFT")) {

                createFT.setStatus("FAILED");
                createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
                ftRepository.updateFundsTransfer(createFT);

                //Log the error
                genericService.generateLog("", token, messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(" ", "Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            //Update the Funds Transfer request
            createFT.setStatus("FAILED");
            createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
            ftRepository.updateFundsTransfer(createFT);

            //Log the response
            genericService.generateLog("Transaction With PL", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage(validationResponse);
            return gson.toJson(errorResponse);
        }

        System.out.println("Passed the OFS validation");

        //Update the Funds Transfer request
        createFT.setStatus("SUCCESS");
        createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        ftRepository.updateFundsTransfer(createFT);

        //Check if the debit account is a customer account
        log.info("checking if the debit account is a customer account");
        if (requestPayload.getDebitAccount().matches("[0-9]{10}")) {
            log.info("<<<<<------------ matches regex------------->>>>>>");
            //Call the Account microservices
            AccountNumberPayload drAccBalRequest = new AccountNumberPayload();
            drAccBalRequest.setAccountNumber(requestPayload.getDebitAccount());
            drAccBalRequest.setRequestId(requestPayload.getRequestId());
            drAccBalRequest.setToken(token);
            drAccBalRequest.setHash(genericService.hashAccountBalanceRequest2(drAccBalRequest));
//                String drAccBalRequestJson = gson.toJson(drAccBalRequest);

            log.info("now we've set drAccBalRequest -------------->>>>>>>> {}", drAccBalRequest);

            log.info("Trying to call account balance service");
            //Call the account microservices
//                String drAccBalResponseJson = accountService.accountBalance(token, drAccBalRequestJson);
            String acctBalresponse = checkAccountBalance(drAccBalRequest.getAccountNumber(), token);
            log.info("<<<<<------------------------------RESPONSE FROM ACCOUNT BALANCE REQUEST ----------------------------->> {} ", acctBalresponse);

            AccountBalanceResponsePayload drAccBalResponse = gson.fromJson(acctBalresponse, AccountBalanceResponsePayload.class);
            if (drAccBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                log.info("<<<--------- successful response -------->>>>");
                NotificationPayload drSmsPayload = new NotificationPayload();
                drSmsPayload.setAccountBalance(drAccBalResponse.getAvailableBalance());
                drSmsPayload.setAccountNumber(requestPayload.getDebitAccount());
                drSmsPayload.setAmount(requestPayload.getAmount());
                drSmsPayload.setBranch("DIGITAL BRANCH");
                drSmsPayload.setMobileNumber(requestPayload.getMobileNumber());
                drSmsPayload.setNarration(requestPayload.getNarration());
                drSmsPayload.setRequestId(requestPayload.getRequestId());
                drSmsPayload.setSmsFor("FUNDS TRANSFER");
                drSmsPayload.setToken(token);
                drSmsPayload.setTransDate(LocalDate.now().toString());
                drSmsPayload.setTransTime(LocalDateTime.now().getHour() + ":" + LocalDateTime.now().getMinute());
                genericService.sendDebitSMS(drSmsPayload);
                log.info("<<<-------- sent debit message successful ------->>> ");
            } else {
                log.info("-------------response for account balance was not successful ---------------- {}", drAccBalResponse);
            }
        }

        //Log the error.
        genericService.generateLog("Transaction With PL", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
        //Create User Activity log
        genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');

        FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
        ftResponse.setAmount(createFT.getAmount());
        ftResponse.setCreditAccount(createFT.getCreditAccount());
        ftResponse.setCreditAccountName(createFT.getCreditAccountName());
        ftResponse.setDebitAccount(createFT.getDebitAccount());
        ftResponse.setDebitAccountName(createFT.getDebitAccountName());
        ftResponse.setNarration(createFT.getNarration());
        ftResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        ftResponse.setStatus(createFT.getStatus());
        ftResponse.setTransRef(requestPayload.getRequestId());
        ftResponse.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        return gson.toJson(ftResponse);

    }

    public AccountDetailsFromT24 accountDetailsFromT24(String accountNumber) {
        DefaultJConnectionFactory connectionFactory = new DefaultJConnectionFactory();
        connectionFactory.setPort(Integer.parseInt(t24Port));
        connectionFactory.setHost(t24Host);
        JConnection conn;
        AccountDetailsFromT24 accountResponse = new AccountDetailsFromT24();
        Set<String> setObject = new HashSet<>();
        try {
            conn = connectionFactory.getConnection();
            JStatement stmt = conn.createStatement();
            stmt.setFetchSize(10);
            String command = "LIST FBNK.ACCOUNT WITH @ID EQ ".concat(accountNumber.trim()).concat(" CUSTOMER");
            JResultSet rst = stmt.execute(command);
            accountResponse = new AccountDetailsFromT24();

            JDynArray row;
            while (rst.next()) {
                row = rst.getRow();
                boolean rowAdded = setObject.add(row.get(1));
                if (!rowAdded) {
                    continue;
                }
                try {
                    accountResponse.setAccountNumber(row.get(1));
                    accountResponse.setCustomerId(row.get(2));
                } catch (Exception e) {
                }
            }

        } catch (JRemoteException ex) {
            System.out.println(ex);
        }
        log.info("Account Details from T24 {}", accountResponse);
        return accountResponse;
    }

    public CustomerDetailsFromT24 customerDetailsFromT24(String customerId) {
        DefaultJConnectionFactory connectionFactory = new DefaultJConnectionFactory();
        connectionFactory.setPort(Integer.parseInt(t24Port));
        connectionFactory.setHost(t24Host);
        JConnection conn;
        CustomerDetailsFromT24 customerResponse = new CustomerDetailsFromT24();
        Set<String> setObject = new HashSet<>();
        try {
            conn = connectionFactory.getConnection();
            JStatement stmt = conn.createStatement();
            stmt.setFetchSize(10);
            String command = "LIST FBNK.CUSTOMER WITH @ID EQ ".concat(customerId.trim()).concat(" SHORT.NAME NAME.1 KYC.LEVEL CUST.TYPE");
            JResultSet rst = stmt.execute(command);
            customerResponse = new CustomerDetailsFromT24();

            JDynArray row;
            while (rst.next()) {
                row = rst.getRow();
                boolean rowAdded = setObject.add(row.get(1));
                if (!rowAdded) {
                    continue;
                }
                try {

                    customerResponse.setLastName(row.get(2));
                    customerResponse.setOtherName(row.get(3));
                    customerResponse.setKycTier(row.get(4));
                    customerResponse.setCustomerType(row.get(5));

                } catch (Exception e) {
                }
            }

        } catch (JRemoteException ex) {
            System.out.println(ex);
        }
        log.info("Customer Details from T24 {}", customerResponse);

        return customerResponse;
    }

    private Account getAccountFromT24(String accountNumber) {
        log.info("getting account from T24");
        String customerId = "";
        AccountDetailsFromT24 accountDetailsFromT24 = accountDetailsFromT24(accountNumber);
        // TODO: 10/4/2023 Add field to distinguish accion
        if (accountDetailsFromT24.getCustomerId() == null || accountDetailsFromT24.getCustomerId().isEmpty() || accountDetailsFromT24.getCustomerId().isBlank()) {
            customerId = "9999999";
        } else {
            customerId = accountDetailsFromT24.getCustomerId();
        }
        CustomerDetailsFromT24 customerDetailsFromT24 = customerDetailsFromT24(customerId);

        Account account = new Account();
        Customer customer = new Customer();

        customer.setKycTier(customerDetailsFromT24.getKycTier());
        customer.setLastName(customerDetailsFromT24.getLastName());
        customer.setOtherName(customerDetailsFromT24.getOtherName());
        customer.setCustomerType(customerDetailsFromT24.getCustomerType());
        account.setCustomer(customer);
        log.info("retrieved account from T24 successfully");
        return account;
    }

    private Account getAccountFromT24ForInternalAccount(String accountNumber) {
        log.info("getting account from T24 for internal account");

        Account account = new Account();
        Customer customer = new Customer();

        customer.setKycTier("1");
        customer.setLastName("ACCION MICROFINANCE BANK");
        customer.setOtherName("po1");
        customer.setCustomerType("NI");
        account.setCustomer(customer);
        log.info("retrieved account from T24 successfully");
        return account;
    }

    @Override
    public String internalLocalTransfer(LocalTransferWithInternalPayload requestPayload, AppUser appUser, Branch branch, String token, String customerName) throws Exception {
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();

        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        log.info("user credentialss ----------------------------------------------------------------------------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", userCredentials);

        log.info("fund transfer request payload ------>>>>> {}", requestPayload);

        String creditAccountName = "";
        if (customerName.isEmpty() || customerName.isBlank()) {
            creditAccountName = customerName;
        } else {
            creditAccountName = "INTERNAL ACCOUNT";
        }
        log.info("we've checked debit account");

        log.info("about to check credit account");
        log.info("credit account is ------->>>> {}", requestPayload.getCreditAccount());
        log.info("............... account checks done...........");

        FundsTransfer newFT = new FundsTransfer();
        newFT.setAmount(requestPayload.getAmount());
        newFT.setAppUser(appUser);
        newFT.setBranch(branch);
        newFT.setCreditCurrency("NGN");
        newFT.setCreatedAt(LocalDateTime.now());
        newFT.setCreditAccount(requestPayload.getCreditAccount());
        newFT.setCreditAccountName(creditAccountName);
        newFT.setCreditAccountKyc("3");
        newFT.setCustomer(null);
        newFT.setDebitAccount(requestPayload.getDebitAccount());
        newFT.setDebitAccountName("INTERNAL ACCOUNT");
        newFT.setDebitAccountKyc("3");
        newFT.setDebitCurrency("NGN");
        newFT.setDestinationBank("ACCION");
        newFT.setGateway("ACCION");
        newFT.setMobileNumber(requestPayload.getMobileNumber());
        newFT.setNarration(requestPayload.getNarration());
        newFT.setRequestId(requestPayload.getRequestId());
        newFT.setStatus("PENDING");
        newFT.setSourceBank("ACCION");
        newFT.setT24TransRef(requestPayload.getRequestId());
        newFT.setTimePeriod(genericService.getTimePeriod());
        newFT.setTransType("ATI");
        newFT.setDebitAccountType("SYSTEM");
        newFT.setDestinationBankCode("090134"); //Accion Bank Code
        newFT.setCreditAccountType("S");

        log.info("new FT ------>> {}", newFT);
        FundsTransfer createFT = ftRepository.createFundsTransfer(newFT);

        log.info("CREATED FUND TRANSFER IN THE DB");

        log.info("-------------Charge type not specified-----------");
        log.info("----------About to post to t24-----------");

        String OFS = "FUNDS.TRANSFER,/I/PROCESS//0,";
        OFS = OFS + userCredentials + ",";
        OFS = OFS + "FT2022" + requestPayload.getRequestId() + "/";
        OFS = OFS + ",TRANSACTION.TYPE::=" + "AC";
        OFS = OFS + ",DEBIT.ACCT.NO::=" + newFT.getDebitAccount();
        OFS = OFS + ",DEBIT.CURRENCY::=" + "NGN";
        OFS = OFS + ",DEBIT.AMOUNT::=" + newFT.getAmount();
        OFS = OFS + ",CREDIT.ACCT.NO::=" + newFT.getCreditAccount();
        OFS = OFS + ",ORDERING.CUST::=" + requestPayload.getNarration();
        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, OFS, "OFS Request", "INFO", requestPayload.getRequestId());
        log.info("ofsRequest to check ---------------------------------------------->>>>>>>>>>>>>>>>>>>>>>> {}", OFS);
        String middlewareResponse = genericService.postToT24(OFS);

        log.info("Middleware - Response: ------------------------------------->>>>>>>>>>>>>>>>>>>> {}", middlewareResponse);

        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
        String validationResponse = genericService.validateT24Response(middlewareResponse);
        System.out.println("Validation Response: " + validationResponse);
        if (validationResponse != null) {
            if (middlewareResponse.toUpperCase(Locale.ENGLISH).contains("WITHDRAWL MAKES A/C BAL LESS THAN MIN BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("A/C BALANCE STILL LESS THAN MINIMUM BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("UNAUTHORISED OVERDRAFT")) {

                createFT.setStatus("FAILED");
                createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
                ftRepository.updateFundsTransfer(createFT);

                //Log the error
                genericService.generateLog(requestPayload.getDebitAccount(), token, messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), "GRUPP", messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            //Update the Funds Transfer request
            createFT.setStatus("FAILED");
            createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
            ftRepository.updateFundsTransfer(createFT);

            //Log the response
            genericService.generateLog("Transaction With PL", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage(validationResponse);
            return gson.toJson(errorResponse);
        }

        System.out.println("Passed the OFS validation");

        //Update the Funds Transfer request
        createFT.setStatus("SUCCESS");
        createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        ftRepository.updateFundsTransfer(createFT);

        //Check if the debit account is a customer account
        log.info("checking if the debit account is a customer account");
        if (requestPayload.getDebitAccount() != null && (requestPayload.getDebitAccount().matches("[0-9]{10}") || requestPayload.getDebitAccount().startsWith("NGN") || requestPayload.getDebitAccount().startsWith("PL"))) {
            log.info("<<<<<------------ matches regex------------->>>>>>");
            //Call the Account microservices
            AccountNumberPayload drAccBalRequest = new AccountNumberPayload();
            drAccBalRequest.setAccountNumber(requestPayload.getDebitAccount());
            drAccBalRequest.setRequestId(requestPayload.getRequestId());
            drAccBalRequest.setToken(token);
            drAccBalRequest.setHash(genericService.hashAccountBalanceRequest2(drAccBalRequest));

//            log.info("now we've set drAccBalRequest -------------->>>>>>>> {}", drAccBalRequest);
        }

        //Log the error.
        genericService.generateLog("Transaction With PL", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
        //Create User Activity log
        genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), "GRUPP", "Success", requestPayload.getMobileNumber(), 'S');

        FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
        ftResponse.setAmount(createFT.getAmount());
        ftResponse.setCreditAccount(createFT.getCreditAccount());
        ftResponse.setCreditAccountName(createFT.getCreditAccountName());
        ftResponse.setDebitAccount(createFT.getDebitAccount());
        ftResponse.setDebitAccountName(createFT.getDebitAccountName());
        ftResponse.setNarration(createFT.getNarration());
        ftResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        ftResponse.setStatus(createFT.getStatus());
        ftResponse.setTransRef(requestPayload.getRequestId());
        ftResponse.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        return gson.toJson(ftResponse);
    }

    @Override
    public String internalLocalTransfer2(LocalTransferWithInternalPayload requestPayload, AppUser appUser, Branch branch, String token, String customerName) throws Exception {
        OmnixResponsePayload errorResponse = new OmnixResponsePayload();

        log.info("fund transfer request payload ------>>>>> {}", requestPayload);

        String creditAccountName = "";
        if (customerName.isEmpty() || customerName.isBlank()) {
            creditAccountName = customerName;
        } else {
            creditAccountName = "INTERNAL ACCOUNT";
        }
        log.info("we've checked debit account");

        log.info("about to check credit account");
        log.info("credit account is ------->>>> {}", requestPayload.getCreditAccount());
        log.info("............... account checks done...........");

        String requestBy = jwtToken.getUsernameFromToken(token);

        String ofsBase;

        FundsTransfer newFT = new FundsTransfer();
        newFT.setAmount(requestPayload.getAmount());
        newFT.setAppUser(appUser);
        newFT.setBranch(branch);
        newFT.setCreditCurrency("NGN");
        newFT.setCreatedAt(LocalDateTime.now());
        newFT.setCreditAccount(requestPayload.getCreditAccount());
        newFT.setCreditAccountName(creditAccountName);
        newFT.setCreditAccountKyc("3");
        newFT.setCustomer(null);
        newFT.setDebitAccount(requestPayload.getDebitAccount());
        newFT.setDebitAccountName("INTERNAL ACCOUNT");
        newFT.setDebitAccountKyc("3");
        newFT.setDebitCurrency("NGN");
        newFT.setDestinationBank("ACCION");
        newFT.setGateway("ACCION");
        newFT.setMobileNumber(requestPayload.getMobileNumber());
        newFT.setNarration(requestPayload.getNarration());
        newFT.setRequestId(requestPayload.getRequestId());
        newFT.setStatus("PENDING");
        newFT.setSourceBank("ACCION");
        newFT.setT24TransRef(requestPayload.getRequestId());
        newFT.setTimePeriod(genericService.getTimePeriod());
        newFT.setTransType("ATI");
        newFT.setDebitAccountType("SYSTEM");
        newFT.setDestinationBankCode("090134"); //Accion Bank Code
        newFT.setCreditAccountType("S");

        log.info("new FT ------>> {}", newFT);
        FundsTransfer createFT = ftRepository.createFundsTransfer(newFT);

        log.info("CREATED FUND TRANSFER IN THE DB");
        //Generate Funds Transfer OFS
        String transRef = genericService.generateTransRef("FT");
        String accountName = "INTERNAL ACCOUNT";

        String narration = requestPayload.getNarration()
                + "/" + accountName
                + "/" + branch.getBranchName().toUpperCase(Locale.ENGLISH)
                + " BCH" + "/"
                + transRef;
        ofsBase = genericService.generateFTOFS(transRef, requestPayload.getDebitAccount(), requestPayload.getCreditAccount(),
                requestPayload.getAmount(), narration, requestPayload.getTransType(), "grupp -",
                requestPayload.getAuthorizer().equalsIgnoreCase("") ? requestBy + "-" + requestPayload.getMobileNumber() : requestPayload.getAuthorizer());
        log.info("ofs base ------------------------>>>>>>>>>>>>>>>>>>>> {}", ofsBase);
        ofsBase = ofsBase.concat(",COMMISSION.CODE::=WAIVE");
        log.info("Generated off base request, sort off");

        //Check if the charge type is specified
        if (requestPayload.getChargeTypes() != null) {
            log.info("charge type is specified");
            List<ChargeTypes> charges = requestPayload.getChargeTypes();
            System.out.println("Charges: " + charges);
            int index = 1;
            double totalCharges = 0;
            if (requestPayload.isUseCommissionAsChargeType()) {
                for (ChargeTypes chg : charges) {
                    ofsBase = ofsBase.concat(",COMMISSION.TYPE:").concat(String.valueOf(index)).concat(":1::=").concat(chg.getChargeType())
                            .concat(",COMMISSION.CODE:").concat(String.valueOf(index)).concat(":1::=DEBIT PLUS CHARGES").concat(",");
                    ofsBase = ofsBase.concat("COMMISSION.AMT:").concat(String.valueOf(index)).concat(":1::=NGN").concat(chg.getChargeAmount().replace(",", "")).concat(",");
                    index++;
                    totalCharges += Double.parseDouble(chg.getChargeAmount().replace(",", ""));
                }
            } else {
                for (ChargeTypes chg : charges) {
                    System.out.println("Charge: " + gson.toJson(chg));
                    ofsBase = ofsBase.concat(",CHARGE.TYPE:").concat(String.valueOf(index)).concat(":1::=").concat(chg.getChargeType())
                            .concat(",CHARGE.CODE:").concat(String.valueOf(index)).concat(":1::=DEBIT PLUS CHARGES").concat(",");
                    ofsBase = ofsBase.concat("CHARGE.AMT:").concat(String.valueOf(index)).concat(":1::=NGN").concat(chg.getChargeAmount().replace(",", "")).concat(",");
                    index++;
                    totalCharges += Double.parseDouble(chg.getChargeAmount().replace(",", ""));
                }
            }
        }

        log.info("-------------Charge type not specified-----------");
        log.info("----------About to post to t24-----------");
        String userCredentials = "CHANNELUSER/DncXB507z2H";
        String ofsRequest = ftVersion.trim() + requestPayload.getNoOfAuthorizer() + "," + userCredentials
                + "/" + createFT.getBranch().getBranchCode() + "," + requestPayload.getRequestId() + "," + ofsBase;
        String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
        log.info("ofsRequest ---------------------------------------------->>>>>>>>>>>>>>>>>>>>>>> {}", ofsRequest);
        String middlewareResponse = genericService.postToT24(ofsRequest);

        log.info("Middleware - Response: ------------------------------------->>>>>>>>>>>>>>>>>>>> {}", middlewareResponse);

        //Generate the OFS Response log
        genericService.generateLog("Transaction With PL", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
        String validationResponse = genericService.validateT24Response(middlewareResponse);
        System.out.println("Validation Response: " + validationResponse);
        if (validationResponse != null) {
            if (middlewareResponse.toUpperCase(Locale.ENGLISH).contains("WITHDRAWL MAKES A/C BAL LESS THAN MIN BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("A/C BALANCE STILL LESS THAN MINIMUM BAL")
                    || middlewareResponse.toUpperCase(Locale.ENGLISH).contains("UNAUTHORISED OVERDRAFT")) {

                createFT.setStatus("FAILED");
                createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
                ftRepository.updateFundsTransfer(createFT);

                //Log the error
                genericService.generateLog(requestPayload.getDebitAccount(), token, messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), "GRUPP", messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');

                errorResponse.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            //Update the Funds Transfer request
            createFT.setStatus("FAILED");
            createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
            ftRepository.updateFundsTransfer(createFT);

            //Log the response
            genericService.generateLog("Transaction With PL", token, middlewareResponse, "API Response", "DEBUG", requestPayload.getRequestId());

            errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            errorResponse.setResponseMessage(validationResponse);
            return gson.toJson(errorResponse);
        }

        System.out.println("Passed the OFS validation");

        //Update the Funds Transfer request
        createFT.setStatus("SUCCESS");
        createFT.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        ftRepository.updateFundsTransfer(createFT);

        //Check if the debit account is a customer account
        log.info("checking if the debit account is a customer account");
        if (requestPayload.getDebitAccount().matches("[0-9]{10}")) {
            log.info("<<<<<------------ matches regex------------->>>>>>");
            //Call the Account microservices
            AccountNumberPayload drAccBalRequest = new AccountNumberPayload();
            drAccBalRequest.setAccountNumber(requestPayload.getDebitAccount());
            drAccBalRequest.setRequestId(requestPayload.getRequestId());
            drAccBalRequest.setToken(token);
            drAccBalRequest.setHash(genericService.hashAccountBalanceRequest2(drAccBalRequest));

            log.info("now we've set drAccBalRequest -------------->>>>>>>> {}", drAccBalRequest);

//            log.info("Trying to call account balance service");
//            //Call the account microservices
//            String accBalResponse = checkAccountBalance(drAccBalRequest.getAccountNumber(), token);
//            log.info("<<<<<------------------------------RESPONSE FROM ACCT BALANCE ----------------------------->> {} ", accBalResponse);
//            AccountBalanceResponsePayload drAccBalResponse = gson.fromJson(accBalResponse, AccountBalanceResponsePayload.class);
//
//            if (drAccBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
//                log.info("<<<--------- successful response -------->>>>");
//                NotificationPayload drSmsPayload = new NotificationPayload();
//                drSmsPayload.setAccountBalance(drAccBalResponse.getAvailableBalance());
//                drSmsPayload.setAccountNumber(requestPayload.getDebitAccount());
//                drSmsPayload.setAmount(requestPayload.getAmount());
//                drSmsPayload.setBranch("DIGITAL BRANCH");
//                drSmsPayload.setMobileNumber(requestPayload.getMobileNumber());
//                drSmsPayload.setNarration(requestPayload.getNarration());
//                drSmsPayload.setRequestId(requestPayload.getRequestId());
//                drSmsPayload.setSmsFor("FUNDS TRANSFER");
//                drSmsPayload.setToken(token);
//                drSmsPayload.setTransDate(LocalDate.now().toString());
//                drSmsPayload.setTransTime(LocalDateTime.now().getHour() + ":" + LocalDateTime.now().getMinute());
//                genericService.sendDebitSMS(drSmsPayload);
//            }
            log.info("-------------response for account balance was not successful ----------------");
        }

        //Log the error.
        genericService.generateLog("Transaction With PL", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
        //Create User Activity log
        genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), "GRUPP", "Success", requestPayload.getMobileNumber(), 'S');

        FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
        ftResponse.setAmount(createFT.getAmount());
        ftResponse.setCreditAccount(createFT.getCreditAccount());
        ftResponse.setCreditAccountName(createFT.getCreditAccountName());
        ftResponse.setDebitAccount(createFT.getDebitAccount());
        ftResponse.setDebitAccountName(createFT.getDebitAccountName());
        ftResponse.setNarration(createFT.getNarration());
        ftResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        ftResponse.setStatus(createFT.getStatus());
        ftResponse.setTransRef(requestPayload.getRequestId());
        ftResponse.setT24TransRef(genericService.getT24TransIdFromResponse(middlewareResponse));
        return gson.toJson(ftResponse);
    }

    @Override
    public GruppResponsePayload validateGruppCashout(GruppCashoutNotificationPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        if (Objects.equals(requestPayload.getStatus(), "success") && Objects.equals(requestPayload.getStatusCode(), "00")) {
            responsePayload.setStatus("Success");
            responsePayload.setMessage("Validation Successful");
        } else {
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage("Validation Error");
        }

        return responsePayload;
    }

    @Override
    public GruppResponsePayload validateGruppDisbursementForTest(GruppDisbursementRequestPayload requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        if (Objects.equals(requestPayload.getTerminalId(), "2LUXA556") || Objects.equals(requestPayload.getTerminalId(), "2033JLST")) {
            log.info("Terminal match");
            responsePayload.setStatus("Success");
            responsePayload.setMessage("Validation successful");
            return responsePayload;
        }
        responsePayload.setStatus("FAILED");
        responsePayload.setMessage("Validation error");
        return responsePayload;
    }

    @Override
    public boolean validateAccountBalancePayload(String token, GruppAgentBalancePayload requestPayload) {
//        StringJoiner rawString = new StringJoiner("|");
//        rawString.add(gruppSeccretKey);
//        rawString.add(requestPayload.getAccountNumber().trim());

        //Default the account number
        return requestPayload.getAccountNumber().equalsIgnoreCase(requestPayload.getAccountNumber());
    }

    @Override
    public String processAccountBalance(String token, GruppAgentBalancePayload requestPayload) {
        log.info("in the method to check account balance");
        GruppResponsePayload responsePayload = new GruppResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);

        //Log the request
        String requestJson = gson.toJson(requestPayload);
        String requestId = genericService.generateTransRef("GR");
        requestPayload.setRequestId(requestId);

        genericService.generateLog("Grupp Agent Account Balance", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Grupp Agent Account Balance", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Account Balance", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

//            Check if an agent exist with the account number
            AccionAgent agent = agencyRepository.getAgentUsingAccountNumber(requestPayload.getAccountNumber(), "Grupp");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Grupp Agent Account Balance", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Account Balance", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

//            Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Agent Account Balance", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Account Balance", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

//            Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Grupp Agent Account Balance", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Grupp Agent Account Balance", "", channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getRequestId(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getAccountNumber()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            log.info("All checks passed");
            // call account balance from core
            String accountBalance = checkAccountBalance(requestPayload.getAccountNumber(), token);
            log.info("raw account balance from core bank directly =======>>> {}", accountBalance);
            AccountBalanceResponsePayload accBalResponse = gson.fromJson(accountBalance, AccountBalanceResponsePayload.class);
            if (!Objects.equals(accBalResponse.getResponseCode(), ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(accBalResponse.getResponseMessage());
                return gson.toJson(responsePayload);
            }

            //call was successful at this point
            responsePayload.setStatus("SUCCESS");
            responsePayload.setBalance(accBalResponse.getAvailableBalance());
            responsePayload.setTransactionReference(requestPayload.getRequestId());
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Grupp Agent Account Balance", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public GruppResponsePayload validateTerminalId(String terminalId) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();

        if (!Objects.equals(terminalId, "2LUXA556")) {
            log.info("Terminal id did not match");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage("Terminal id did not match");
        } else {
            log.info("Terminal id is a match");
            responsePayload.setStatus("SUCCESS");
            responsePayload.setMessage("Terminal id valid");
        }
        return responsePayload;
    }

    @Override
    public String getCashoutReport(HttpServletRequest httpRequest, StatusReportRequest requestPayload) {

        List<CashoutStatus> result = new ArrayList<>();
        result = cashoutStatusRepository.getReport(requestPayload.getTranRef());
        if (result.isEmpty()) {
            StatusReportResponse response = StatusReportResponse.builder().responseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode())
                    .responseMessage("No record found")
                    .build();
            return gson.toJson(response);
        }
        String status = "";
        List<String> statuses = new ArrayList<>();
        for (CashoutStatus cashoutStatus : result) {
            statuses.add(cashoutStatus.getStatus());
        }
        if (statuses.contains("Transaction completed")) {
            status = "Transaction completed";
        } else {
            status = "Transaction not completed";
        }
        StatusReportResponse response = StatusReportResponse.builder().responseCode(ResponseCodes.SUCCESS_CODE.getResponseCode())
                .responseMessage("Successful")
                .status(status)
                .cashoutStatuses(result)
                .build();
        return gson.toJson(response);
    }

    @Override
    public String checkAccountBalance(String accountNumber, String token) {

        OmnixResponsePayload errorResponse = new OmnixResponsePayload();
        String response = "";
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);

        log.info("Account number to check with ------------>> {}", accountNumber);
        log.info("User credentials gotten from token ------------>> {}", jwtToken.getUserCredentialFromToken(token));

        log.info("account check for acc balance done");
        String branchCode = "NG0010068";
        StringBuilder ofsBase = new StringBuilder("");
        ofsBase.append("ENQUIRY.SELECT,,")
                .append(userCredentials)
                .append("/")
                .append(branchCode)
                .append(",ACCION.ACCOUNT.DETAIL,ACCOUNT.NUMBER:EQ=")
                .append(accountNumber);

        String newOfsRequest = genericService.formatOfsUserCredentials(ofsBase.toString(), userCredentials);
        //Generate the OFS Request log
        genericService.generateLog("Account Balance", token, newOfsRequest, "OFS Request", "INFO", "");
        response = genericService.postToT24(ofsBase.toString());
        //Generate the OFS Response log
        genericService.generateLog("Account Balance", token, response, "OFS Response", "INFO", "");
        log.info("rew response ---------------->>>>>> {}", response);
        String validationResponse = genericService.validateT24Response(response);
        log.info("validation response -===========>>>> {}", validationResponse);
        if (validationResponse != null) {
            log.info("validation is null !!!");
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(validationResponse);
            //Log the error
            genericService.generateLog("Account Balance", token, validationResponse, "API Error", "DEBUG", "");
            //Create User Activity log
            genericService.createUserActivity(accountNumber, "Account Balance", "", channel, validationResponse, "", 'F');
            return gson.toJson(errorResponse);
        }

        //Log the error
        genericService.generateLog("Account Balance", token, response, "API Response", "INFO", "");
        //Create User Activity log
        genericService.createUserActivity(accountNumber, "Account Balance", "", channel, "Success", accountNumber, 'S');
        AccountBalanceResponsePayload accountResponse = parseAccountDetails(response, accountNumber);
        DecimalFormat df = new DecimalFormat("###,###,###.00");
        double value = Double.parseDouble(accountResponse.getAvailableBalance() == null ? "0" : accountResponse.getAvailableBalance());
        accountResponse.setAvailableBalance(df.format(value).equalsIgnoreCase(".00") ? "0.00" : df.format(value));
        value = Double.parseDouble(accountResponse.getLedgerBalance() == null ? "0" : accountResponse.getLedgerBalance());
        accountResponse.setLedgerBalance(df.format(value).equalsIgnoreCase(".00") ? "0.00" : df.format(value));

        return gson.toJson(accountResponse);
    }

    public AccountBalanceResponsePayload getAccountDetailsFromEnquiry(String response) {
        log.info("ofs response ------------>>>>>>> {}", response);

        String[] accountDetailsOfsResponseSplit = response.split(",");
        String[] accountDetailsSplit = accountDetailsOfsResponseSplit[1].split(",");
        String accountDetailsHeaders = accountDetailsSplit[0];
        String accountDetailsBody = accountDetailsOfsResponseSplit[2];
        String[] accountDetailsHeadersSplit = accountDetailsHeaders.split("/");
        String[] accountDetailsBodySplit = accountDetailsBody.split("\\*");

        AccountBalanceResponsePayload accountPayload = new AccountBalanceResponsePayload();

        accountPayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
        accountPayload.setAccountNumber(accountDetailsBodySplit[16]);
        accountPayload.setAccountName(accountDetailsBodySplit[1]);
        accountPayload.setAvailableBalance(accountDetailsBodySplit[6]);
        accountPayload.setCategoryCode(accountDetailsBodySplit[22].replace("\"", "").replace("\\", ""));
        return accountPayload;
    }

    private AccountBalanceResponsePayload parseAccountDetails(String sResponse, String accountNumber) {
        sResponse = sResponse.replaceFirst(",\"", "^");
        String[] items = getMessageTokens(sResponse, "^");
        ArrayList list = new ArrayList();
        int count = 0;
        String testParam = "";
        for (int k = 0; k < items.length; k++) {
            testParam = items[0];
            if (k > 0) {
                list.add(items[k]);
                count++;
            }
        }

        if (testParam.contains("SECURITY VIOLATION DURING SIGN ON PROCESS")) {

            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            accountResponse.setAccountNumber(accountNumber);
            accountResponse.setAvailableBalance("0.00".replace("\"", "").trim());
            accountResponse.setLedgerBalance("0.00".replace("\"", "").trim());
            accountResponse.setResponseCode(ResponseCodes.FORMAT_EXCEPTION.getResponseCode());
            return accountResponse;
        }
        if (testParam.contains("No records were found that matched the selection criteria")) {
            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            accountResponse.setAccountNumber(accountNumber);
            accountResponse.setAvailableBalance("0.00".replace("\"", "").trim());
            accountResponse.setLedgerBalance("0.00".replace("\"", "").trim());
            accountResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
            return accountResponse;
        }
        for (int k = 0; k < count; k++) {
            DecimalFormat formatter = new DecimalFormat("###.##");
            AccountBalanceResponsePayload accountResponse = new AccountBalanceResponsePayload();
            String val = ((String) list.get(k)).replaceAll("\"", "");
            items = val.split("\t");
            accountResponse.setAccountNumber(accountNumber);

            accountResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());

            String lastItem = items[items.length - 1];
            String[] lastItemSplit = lastItem.split("\\*");
            String categoryCode = lastItemSplit[lastItemSplit.length - 1].replaceAll("\\*", "");

            String accountName = items[1];
            String cif = items[2];
            String productName = items[3];
            String branchCode = items[13];
            String oldAccountNumber = items[17];

            accountResponse.setAccountName(accountName);
            accountResponse.setCif(cif);
            accountResponse.setProductName(productName);
            accountResponse.setBranchCode(branchCode);
            accountResponse.setOldAccountNumber(oldAccountNumber);

            double lockedAmount = 0;
            try {
                if (items.length > 22) {
                    lockedAmount = Double.parseDouble(items[22].trim().replaceAll(",", ""));
                }
            } catch (NumberFormatException numberFormatException) {
            }
            try {
                double availBal = Double.parseDouble(items[5].trim()) - lockedAmount;
                accountResponse.setAvailableBalance(formatter.format(availBal).replace("\"", "").trim());
            } catch (NumberFormatException numberFormatException) {
            }
            try {
                double LedgerBal = Double.parseDouble(items[8].trim());
                accountResponse.setLedgerBalance(formatter.format(LedgerBal).replace("\"", "").trim());
            } catch (NumberFormatException numberFormatException) {
            }

            accountResponse.setCategoryCode(categoryCode);
            return accountResponse;
        }
        return null;
    }

    public static String[] getMessageTokens(String msg, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(msg, delim);
        ArrayList tokens = new ArrayList(10);
        for (; tokenizer.hasMoreTokens(); tokens.add(tokenizer.nextToken())) {
        }
        return (String[]) tokens.toArray(new String[1]);
    }

    // added by Daniel Ofoleta on Feb 13, 2024
    // To re-implement inter bank transfers
    public String processNIPTransfer(String token, NIPTransferPayload requestPayload, AccionAgent agent) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String sNipResponse = "";
        String requestJson = gson.toJson(requestPayload);
        requestPayload.setAmount(requestPayload.getAmount().replaceAll(",", ""));

        genericService.generateLog("NIP Transfer", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
        try {

            AppUser appUser = ftRepository.getAppUserUsingUsername(requestBy);
            if (appUser == null) {
                genericService.generateLog("NIP", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                genericService.createUserActivity(requestBy, "NIP", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }

            String debitAccountNumber = agent.getAgentAccountNumber();
            String kycTier = agent.getKycLevel();
            String branchCode = agent.getBranchCode();

            // Perform name enquiry.
            NIPNameEnquiryPayload nameEnquiryPayload = new NIPNameEnquiryPayload();
            nameEnquiryPayload.setBeneficiaryAccount(requestPayload.getBeneficiaryAccount());
            nameEnquiryPayload.setBeneficiaryBankCode(requestPayload.getBeneficiaryBankCode());

            String nameEnquiryResponseJson = processNIPNameEnquiry(token, nameEnquiryPayload);

            NIPPayload oNESingleResponse = gson.fromJson(nameEnquiryResponseJson, NIPPayload.class);
            if (oNESingleResponse == null || !oNESingleResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                genericService.generateLog("NIP Name Enquiry", token, nameEnquiryResponseJson, "API Response", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.account.beneficiary.notexist", new Object[0], Locale.ENGLISH));
                return gson.toJson(errorResponse);
            }
            String sessionId = generateSessionId();
            // Log transfer request
            FundsTransfer newFT = new FundsTransfer();
            newFT.setAmount(requestPayload.getAmount());
            newFT.setAppUser(appUser);
            newFT.setCreditCurrency("NGN");
            newFT.setCreatedAt(LocalDateTime.now());
            newFT.setCreditAccount(requestPayload.getBeneficiaryAccount());
            newFT.setCreditAccountName(oNESingleResponse.getAccountName());
            newFT.setCreditAccountKyc(oNESingleResponse.getKycLevel());
            newFT.setDebitAccount(debitAccountNumber);
            String fullName = agent.getAgentName();
            newFT.setDebitAccountName(fullName);
            newFT.setDebitAccountKyc(kycTier);
            newFT.setDebitCurrency("NGN");
            newFT.setDestinationBank("ACCION");
            newFT.setGateway("NIBSS");
            newFT.setMobileNumber(requestPayload.getMobileNumber());
            newFT.setNarration(requestPayload.getNarration());
            newFT.setRequestId(sessionId);
            newFT.setStatus("Pending");
            newFT.setSourceBank("ACCION");
            newFT.setT24TransRef("");
            newFT.setTimePeriod(genericService.getTimePeriod());
            newFT.setTransType("NIP");
            newFT.setDebitAccountType("Individual");
            newFT.setDestinationBankCode(oNESingleResponse.getDestinationInstitutionCode());
            newFT.setCreditAccountType("");
            newFT.setSettelemtAccount(nipOutboundSettlementAccount);

            FundsTransfer oFundsTransfer = ftRepository.createFundsTransfer(newFT);

            // perform debit transaction on senders account
            double charge = 20.00;
            FundsTransferResponsePayload ftResponse = performAccountDebit(requestPayload, oFundsTransfer, token,
                    channel, requestBy, requestBy, charge, userCredentials, branchCode, debitAccountNumber);

            if (ftResponse == null) {
                // return error
                genericService.generateLog("Outbound NIP", token, "System Malfunction", "API Error", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.SYSTEM_MALFUNCTION.getResponseCode());
                errorResponse.setResponseMessage("System Malfunction");
                return gson.toJson(errorResponse);
            } else {
                if (!ftResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    // return error
                    genericService.generateLog("Outbound NIP", token, "System Malfunction", "API Error", "DEBUG", requestPayload.getRequestId());
                    errorResponse.setResponseCode(ftResponse.getResponseCode());
                    errorResponse.setResponseMessage(ftResponse.getResponseMessage());
                    return gson.toJson(errorResponse);
                }
            }

            if (ftResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                // call NIP APIs to send out the funds
                FTSingleCreditRequest nipRequest = new FTSingleCreditRequest();
                nipRequest.setNameEnquiryRef(oNESingleResponse.getNameEnquiryRef());
                nipRequest.setDestinationInstitutionCode(requestPayload.getBeneficiaryBankCode());
                nipRequest.setChannelCode(2);
                nipRequest.setBeneficiaryAccountName(oNESingleResponse.getBeneficiaryAccountName());
                nipRequest.setBeneficiaryAccountNumber(requestPayload.getBeneficiaryAccount());

                String benBvn = oNESingleResponse.getBeneficiaryBankVerificationNumber() == null ? "22000000035" : oNESingleResponse.getBeneficiaryBankVerificationNumber();
                nipRequest.setBeneficiaryBankVerificationNumber(benBvn);

                String benKycLevel = oNESingleResponse.getBeneficiaryKYCLevel() == null ? "3" : oNESingleResponse.getBeneficiaryKYCLevel();
                nipRequest.setBeneficiaryKYCLevel(benKycLevel);

                nipRequest.setOriginatorAccountName(oFundsTransfer.getDebitAccountName());
                nipRequest.setOriginatorAccountNumber(oFundsTransfer.getDebitAccount());

                nipRequest.setOriginatorBankVerificationNumber(agent.getBvn() == null ? "22000000035" : agent.getBvn());
                nipRequest.setOriginatorKYCLevel(agent.getKycLevel());
                nipRequest.setTransactionLocation("");
                nipRequest.setNarration(requestPayload.getNarration() == null ? "FUNDS TRANSFER" : requestPayload.getNarration());
                nipRequest.setPaymentReference(ftResponse.getT24TransRef());
                nipRequest.setAmount(requestPayload.getAmount());
                nipRequest.setSessionId(sessionId);

                String outwardNipRequest = gson.toJson(nipRequest);
                FTSingleCreditResponse nipResponse;// = new FTSingleCreditResponse();
                genericService.generateLog("NIP", token, outwardNipRequest, "API Request", "INFO", requestPayload.getRequestId());
                // call NIP Transfer API
                nipResponse = nipUtil.doNipFundsTransfer(nipRequest);
                if (nipResponse != null && nipResponse.getResponseCode() != null && nipResponse.getResponseCode().trim().equals("00")) {
                    oFundsTransfer.setStatus("SUCCESS");
                    oFundsTransfer.setT24TransRef(ftResponse.getT24TransRef());
                    ftRepository.updateFundsTransfer(oFundsTransfer);
                    genericService.generateLog("NIP", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
                    genericService.createUserActivity(debitAccountNumber, "NIP", requestPayload.getAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');
                    return gson.toJson(ftResponse);
                } else {
                    if (nipResponse != null && nipResponse.getResponseCode() != null && (nipResponse.getResponseCode().trim().equals("09")
                            || nipResponse.getResponseCode().trim().equals("96")
                            || nipResponse.getResponseCode().trim().equals("97"))) {
                        // do not reverse transaction. This is to save the bank in case of a successful transaction in other bank.
                        oFundsTransfer.setStatus("FAILED");
                    } else {
                        // reverse transaction
                        //Get the transaction status from T24
                        String reveresalVersion = "FUNDS.TRANSFER,PHB.GENERIC.ACTR.INFLOW/I/PROCESS/0/0";
                        String ofsRequest = reveresalVersion + "," + userCredentials
                                + "/" + agent.getBranchCode() + "," + oFundsTransfer.getT24TransRef() + ",";
                        String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                        //Generate the OFS Response log
                        genericService.generateLog("Transaction Reversal", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                        genericService.postToT24(ofsRequest);
                        oFundsTransfer.setStatus("REVERSED");

                    }
                }

                if (nipResponse != null && nipResponse.getResponseCode().trim().equals("51")) {
//                oFundsTransfer.setStatus("FAILED");
                    oFundsTransfer.setT24TransRef(nipResponse.getPaymentReference());
                    oFundsTransfer.setFailureReason(nipResponse.getResponseDescription());
                    ftRepository.updateFundsTransfer(oFundsTransfer);
                    genericService.generateLog("NIP", token, gson.toJson(nipResponse), "API Response", "DEBUG", requestPayload.getRequestId());
                    errorResponse.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.insufficient.balance", new Object[0], Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
                if (nipResponse != null && "01".equals(nipResponse.getResponseCode()) || "03"
                        .equals(nipResponse.getResponseCode()) || "05"
                        .equals(nipResponse.getResponseCode()) || "06"
                        .equals(nipResponse.getResponseCode()) || "07"
                        .equals(nipResponse.getResponseCode()) || "08"
                        .equals(nipResponse.getResponseCode()) || "09"
                        .equals(nipResponse.getResponseCode()) || "12"
                        .equals(nipResponse.getResponseCode()) || "13"
                        .equals(nipResponse.getResponseCode()) || "14"
                        .equals(nipResponse.getResponseCode()) || "15"
                        .equals(nipResponse.getResponseCode()) || "16"
                        .equals(nipResponse.getResponseCode()) || "17"
                        .equals(nipResponse.getResponseCode()) || "18"
                        .equals(nipResponse.getResponseCode()) || "26"
                        .equals(nipResponse.getResponseCode()) || "34"
                        .equals(nipResponse.getResponseCode()) || "35"
                        .equals(nipResponse.getResponseCode()) || "57"
                        .equals(nipResponse.getResponseCode()) || "58"
                        .equals(nipResponse.getResponseCode()) || "92"
                        .equals(nipResponse.getResponseCode()) || "94"
                        .equals(nipResponse.getResponseCode())) {
                    oFundsTransfer.setStatus("FAILED");

                    // reverse transaction
                    //Get the transaction status from T24
                    String reveresalVersion = "FUNDS.TRANSFER,PHB.GENERIC.ACTR.INFLOW/I/PROCESS/0/0";
                    String ofsRequest = reveresalVersion + "," + userCredentials
                            + "/" + agent.getBranchCode() + "," + oFundsTransfer.getT24TransRef() + ",";
                    String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                    //Generate the OFS Response log
                    genericService.generateLog("Transaction Reversal", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                    String middlewareResponse = genericService.postToT24(ofsRequest);
                    oFundsTransfer.setStatus("REVERSED");

                    oFundsTransfer.setT24TransRef(nipResponse.getPaymentReference());
                    oFundsTransfer.setFailureReason(nipResponse.getResponseDescription());
                    ftRepository.updateFundsTransfer(oFundsTransfer);
                    genericService.generateLog("NIP", token, gson.toJson(nipResponse), "API Response", "DEBUG", requestPayload.getRequestId());
                    errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                    errorResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.norepeat", new Object[0], Locale.ENGLISH) + " - " + messageSource.getMessage("appMessages.transaction.norepeat", new Object[0], Locale.ENGLISH));
                    return gson.toJson(errorResponse);
                }
                oFundsTransfer.setStatus("FAILED");

                // reverse transaction
                //Get the transaction status from T24
                String reveresalVersion = "FUNDS.TRANSFER,PHB.GENERIC.ACTR.INFLOW/I/PROCESS/0/0";
                String ofsRequest = reveresalVersion + "," + userCredentials
                        + "/" + agent.getBranchCode() + "," + oFundsTransfer.getT24TransRef() + ",";
                String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
                //Generate the OFS Response log
                genericService.generateLog("Transaction Reversal", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
                genericService.postToT24(ofsRequest);
                oFundsTransfer.setStatus("REVERSED");

                oFundsTransfer.setT24TransRef(nipResponse.getPaymentReference());
                oFundsTransfer.setFailureReason(nipResponse.getResponseDescription());
                ftRepository.updateFundsTransfer(oFundsTransfer);
                genericService.generateLog("NIP", token, gson.toJson(nipResponse), "API Response", "DEBUG", requestPayload.getRequestId());
                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
                errorResponse.setResponseMessage(nipResponse.getResponseDescription());
                return gson.toJson(errorResponse);
            }
        } catch (JsonSyntaxException | NumberFormatException | NoSuchMessageException ex) {

        }
        genericService.generateLog("NIP", token, "Internal server error", "API Error", "DEBUG", requestPayload.getRequestId());
        errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
        errorResponse.setResponseMessage("Internal server error");
        return gson.toJson(errorResponse);
    }

    private FundsTransferResponsePayload performAccountDebit(NIPTransferPayload requestPayload, FundsTransfer oFundsTransfer, String token,
            String channel, String requetedBy, String authBy, double charge, String userCredentials, String branchCode, String debitAccount) {
        FundsTransferResponsePayload omniResponse = new FundsTransferResponsePayload();
        try {
            // Validate the amount.
            BigDecimal maxAmountDecimal = new BigDecimal("500000");
            BigDecimal requestBigDecimal = new BigDecimal(requestPayload.getAmount());
            if (requestBigDecimal.compareTo(BigDecimal.ZERO) < 0) {
                //Log the error
                genericService.generateLog("Funds Transfer", token, messageSource.getMessage("appMessages.transaction.negative-amount", new Object[]{nipMaximumAmount}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.transaction.negative-amount", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                omniResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
                omniResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.negative-amount", new Object[]{nipMaximumAmount}, Locale.ENGLISH));
                return omniResponse;
            }

            if (requestBigDecimal.compareTo(maxAmountDecimal) > 0) {
                //Log the error
                genericService.generateLog("Funds Transfer", token, messageSource.getMessage("appMessages.transaction.max-amount", new Object[]{nipMaximumAmount}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
                //Create User Activity log
                genericService.createUserActivity("", "Funds Transfer", requestPayload.getAmount(), channel, messageSource.getMessage("appMessages.transaction.max-amount", new Object[0], Locale.ENGLISH), requestPayload.getMobileNumber(), 'F');
                omniResponse.setResponseCode(ResponseCodes.FAILED_MODEL.getResponseCode());
                omniResponse.setResponseMessage(messageSource.getMessage("appMessages.transaction.max-amount", new Object[]{nipMaximumAmount}, Locale.ENGLISH));
                return omniResponse;
            }

            //Generate Funds Transfer OFS
            String transRef = oFundsTransfer.getRequestId();
            String ofsBase = genericService.generateFTOFS(transRef, debitAccount, nipOutboundSettlementAccount,
                    requestPayload.getAmount(), requestPayload.getNarration(), tranType, requetedBy + "-" + requestPayload.getMobileNumber(),
                    authBy + "-" + requestPayload.getMobileNumber());

            String commissionCode = "";
            String commissionType = "";
            if (charge > 0) {
                double commToPayBank = charge;
                commissionCode = ",COMMISSION.CODE:1:1::=DEBIT PLUS CHARGES";
                switch (channel.toUpperCase().trim()) {
                    case "MOBILE":
                        commissionType += ",COMMISSION.TYPE:1:1::=".concat(mobileCode);
                        break;
                    case "AGENCY":
                        commissionType += ",COMMISSION.TYPE:1:1::=".concat(agencyCode);
                        break;
                    default:
                        commissionType += ",COMMISSION.TYPE:1:1::=".concat(tranCode);
                        break;
                }
                DecimalFormat formatter = new DecimalFormat("###.##");
                commissionType += ",COMMISSION.AMT:1:1::=NGN" + formatter.format(commToPayBank);

                if (!channel.toUpperCase().trim().equals("AGENCY")) {
                    if (vatCode == null) {
                        commissionType += ",COMMISSION.TYPE:2:1::=TILNIPVAT";
                    } else {
                        commissionType += ",COMMISSION.TYPE:2:1::=" + vatCode;
                    }
                    commissionCode += ",COMMISSION.CODE:2:1::=DEBIT PLUS CHARGES";
                    double vatCharged = charge * vatRate;
                    commissionType += ",COMMISSION.AMT:2:1::=NGN" + formatter.format(vatCharged);
                }
            }

            ofsBase = ofsBase.concat(commissionType).concat(commissionCode);
            String noOfAuthorizers = "0";
            String ofsRequest = ftVersion.trim() + noOfAuthorizers + "," + userCredentials + "/" + branchCode + ",," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Funds Transfer", token, newOfsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
            String sOfsResponse = genericService.postToT24(ofsRequest);
            //Generate the OFS Response log
            genericService.generateLog("Funds Transfer", token, sOfsResponse, "OFS Response", "INFO", requestPayload.getRequestId());

            //Update the Funds Transfer request
            Pair<String, String> ftResp = genericService.getResponseMessage(sOfsResponse);
            String responseCode = ftResp.item1;
            String responseMessage = ftResp.item2;
            String sataus = "FAILED";
            String ftId;
            if (responseCode.equals(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                sataus = "SUCCESS";
                ftId = genericService.getT24TransIdFromResponse(sOfsResponse);
            } else {
                ftId = "";
            }
            oFundsTransfer.setFailureReason(responseMessage);
            oFundsTransfer.setStatus(sataus);
            oFundsTransfer.setT24TransRef(ftId);
            ftRepository.updateFundsTransfer(oFundsTransfer);

            //Log the error. 
            genericService.generateLog("Funds Transfer", token, "Success", "API Response", "INFO", requestPayload.getRequestId());
            //Create User Activity log
            genericService.createUserActivity(requestPayload.getDebitAccount(), "Funds Transfer", requestPayload.getAmount(), channel, "Success", requestPayload.getMobileNumber(), 'S');

            FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
            ftResponse.setAmount(oFundsTransfer.getAmount());
            ftResponse.setCreditAccount(oFundsTransfer.getCreditAccount());
            ftResponse.setCreditAccountName(oFundsTransfer.getCreditAccountName());
            ftResponse.setDebitAccount(oFundsTransfer.getDebitAccount());
            ftResponse.setDebitAccountName(oFundsTransfer.getDebitAccountName());
            ftResponse.setNarration(oFundsTransfer.getNarration());
            ftResponse.setResponseCode(responseCode);
            ftResponse.setResponseMessage(responseMessage);
            ftResponse.setStatus(oFundsTransfer.getStatus());
            ftResponse.setTransRef(requestPayload.getRequestId());
            ftResponse.setT24TransRef(ftId);
            return ftResponse;

        } catch (NoSuchMessageException ex) {
            System.out.println("Error: " + ex.getMessage());
            //Log the response
            genericService.generateLog("Funds Transfer", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
            omniResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            omniResponse.setResponseMessage(ex.getMessage());
            return omniResponse;
        }

    }
}
