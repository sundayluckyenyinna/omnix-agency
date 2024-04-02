package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.*;
import com.accionmfb.omnix.agency.payload.*;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.repository.NotificationHistoryRepo;
import com.accionmfb.omnix.agency.repository.TransactionReferenceRepository;
import com.accionmfb.omnix.agency.service.AccountService;
import com.accionmfb.omnix.agency.service.ErrandPayService;
import com.accionmfb.omnix.agency.service.GenericService;
import com.accionmfb.omnix.agency.service.GruppService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class ErrandPayServiceImpl implements ErrandPayService {

    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    private NotificationHistoryRepo notificationHistoryRepo;

    @Autowired
    private TransactionReferenceRepository transactionReferenceRepository;
    @Autowired
    private GenericService genericService;
    @Autowired
    MessageSource messageSource;
    @Autowired
    AgencyRepository agencyRepository;

    @Autowired
    AccountService accountService;

    @Autowired
    GruppService gruppService;

    Gson gson = new Gson();


    @Value("${app.errandPay.fee}")
    private String errandPayFee;

    @Value("${app.errandPay.debitAccount}")
    private String debitAccount;

    @Value("${app.errandPay.receivableCommission}")
    private String receivableCommission;

    @Value("${app.errandPay.incomeAccount}")
    private String incomeAccount;

    @Value("${app.errandPay.vatPayableAccount}")
    private String vatPayableAccount;

    @Override
    public GruppResponsePayload validateErrandPayCashout(ErrandPayCashoutNotification requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();

        if (!Objects.equals(requestPayload.getStatusDescription(), "Successful") || !Objects.equals(requestPayload.getStatusCode(), "00")) {
            log.info("An error occurred");
            responsePayload.setStatus("FAILED");
            responsePayload.setMessage("Validation Error");
        }

        else {
            responsePayload.setStatus("Success");
            responsePayload.setMessage("Validation successful");
        }

        return responsePayload;
    }

    @Override
    public boolean validateErrandPayCashoutNotificationPayload(String token, ErrandPayCashoutNotification requestPayload) {
        return true;
    }

    @Override
    public String processErrandPayCashoutNotification(String token, ErrandPayCashoutNotification requestPayload) {
        GruppResponsePayload responsePayload = new GruppResponsePayload();



        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        log.info("before request json ----->>>> {}", requestPayload.toString());
        String requestJson = gson.toJson(requestPayload);
        log.info("after request json ----->>>> {}", requestJson);
        genericService.generateLog("Errand pay Cashout Notification", token, requestJson, "API Request", "INFO", requestPayload.getTransactionReference());
        log.info("--------saved transaction-----");
        try {
            //Check if the transaction is coming from GRUPP
            log.info("entering try block----->>");
            if (!requestPayload.getTransactionType().equalsIgnoreCase("Purchase")) {
                log.info("Not purchase ---------------");
                genericService.generateLog("Errand pay Cashout Notification", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getTransactionReference());
                //Create User Activity log
                genericService.createUserActivity("", "Errand pay Cashout Notification", String.valueOf(requestPayload.getAmount()), "", messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getTransactionReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            log.info("is purchase");

            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                log.info("grupp is not what it is");
                genericService.generateLog("Errand pay Cashout Notification", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getTransactionReference());
                //Create User Activity log
                genericService.createUserActivity("", "Errand pay Cashout Notification", String.valueOf(requestPayload.getAmount()), channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestPayload.getTransactionReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.transaction.disallowed", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getSerialNumber(), "ErrandPay");
            log.info("found agent ----->>> {}", agent);
            if (agent == null || agent.getTerminalId() == null) {
                //Log the error
                log.info("terminal id is null");
                genericService.generateLog("Errand pay Cashout Notification", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getTransactionReference());
                //Create User Activity log
                genericService.createUserActivity("", "Errand pay Cashout Notification", String.valueOf(requestPayload.getAmount()), channel, messageSource.getMessage("appMessages.agent.notexist", new Object[0], Locale.ENGLISH), requestPayload.getTransactionReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                log.info("account number null");
                genericService.generateLog("Errand pay Cashout Notification", token, messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getTransactionReference());
                //Create User Activity log
                genericService.createUserActivity("", "Errand pay Cashout Notification", String.valueOf(requestPayload.getAmount()), channel, messageSource.getMessage("appMessages.agent.account.notexist", new Object[0], Locale.ENGLISH), requestPayload.getTransactionReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                log.info("customer is not active");
                genericService.generateLog("Errand pay Cashout Notification", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getTransactionReference());
                //Create User Activity log
                genericService.createUserActivity("", "Errand Pay Cashout Notification", String.valueOf(requestPayload.getAmount()), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestPayload.getTransactionReference(), 'F');
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            log.info("agent not null");
            log.info("about to check for customer");

//            Customer customer = agencyRepository.getCustomerUsingMobileNumber(agent.getAgentMobile());
//            log.info("found customer ------>>> {}", customer);
//            if(customer.getId() == null) {
//                responsePayload.setStatus("FAILED");
//                responsePayload.setMessage(messageSource.getMessage("Customer not found", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
//                return gson.toJson(responsePayload);
//            }

            TransactionReference foundReferenceNumber = transactionReferenceRepository.getByReferenceNumber(requestPayload.getTransactionReference());
            if (foundReferenceNumber != null) {
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(messageSource.getMessage("Reference number already exists", new Object[]{requestPayload.getAdditionalDetails().getTerminalID()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }
            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
            log.info("found app user ------->>>> {}", appUser.toString());

            Branch branch = genericService.getBranchUsingBranchCode("NG0010068"); //Defaulted to Digital Branch
            log.info("found branch ------->>>> {}", branch);

            log.info("good to go------");
            BigDecimal transAmount = BigDecimal.valueOf(requestPayload.getAmount());
            //Fee is 0.6% of the transaction amount. This equals 0.006
            BigDecimal transFee;
            transFee = transAmount.divide(new BigDecimal(100)).multiply(new BigDecimal("0.54"));
            //Check to cap it at N150 max
            if (transFee.doubleValue() > 150) {
                log.info("Capped transfer fee");
                transFee = new BigDecimal("150");
            }
            log.info("transaction fee -------->>>> {}", transFee);

            // calculate errand pay income
            BigDecimal errandPayCommission;
            errandPayCommission = BigDecimal.valueOf(0.41).divide(BigDecimal.valueOf(100)).multiply(transAmount).setScale(2, RoundingMode.CEILING);
            if (errandPayCommission.doubleValue() > 54) {
                errandPayCommission = BigDecimal.valueOf(54);
            }
            log.info("errand pay commission --------->>>> {}", errandPayCommission);

            // calculate gross income
            BigDecimal grossIncome = transFee.subtract(errandPayCommission);
            log.info("gross income --------->>>>>> {}", grossIncome);

            // calculate VAT
            BigDecimal vat = BigDecimal.valueOf(7.5).multiply(grossIncome).divide(BigDecimal.valueOf(107.5), 2, RoundingMode.CEILING);
            log.info("VAT --------->>>>>> {}", vat);

            //calculate net income
            BigDecimal netIncome = grossIncome.subtract(vat).setScale(2, RoundingMode.CEILING);
            log.info("net income --------->>>>>> {}", netIncome);

            BigDecimal netAmount = (BigDecimal.valueOf(requestPayload.getAmount())).subtract(transFee).setScale(2, RoundingMode.CEILING);
            log.info("net amount --------->>>>>> {}", netAmount);

            String narration ="Epay-WD" +
                            "/" + agent.getAgentAccountNumber() +
                            "/" + requestPayload.getTransactionReference();

            String requestId = generateRequestId();

            //Generate the funds transfer request payload to Omnix
            LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
            ftRequestPayload.setMobileNumber(agent.getAgentMobile());
            ftRequestPayload.setDebitAccount(debitAccount);
            ftRequestPayload.setCreditAccount(agent.getAgentAccountNumber());
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

            // call funds transfer to agent
            String ftResponseJson = gruppService.internalLocalTransfer(ftRequestPayload, appUser, branch, token, "");
            TransactionReference transactionReference = TransactionReference.builder().referenceNumber(requestPayload.getTransactionReference()).build();
            log.info("the transaction reference we are trying to save is --------->>> {}", transactionReference);
            TransactionReference transactionReference1 = transactionReferenceRepository.createTransactionReference(transactionReference);

            FundsTransferResponsePayload ftResponsePayload = gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
            log.info("response from fund transfer  for agent --->> {}", ftResponsePayload);

            //Check if the transaction failed
            if (!ftResponsePayload.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !ftResponsePayload.getStatus().equalsIgnoreCase("SUCCESS")) {
                log.info("transaction failed");
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(ftResponsePayload.getResponseMessage());

                return gson.toJson(responsePayload);
            }
            log.info("Transfer to agent is successful");

            FundsTransferResponsePayload vatTransfer = vatLocalTransfer(agent, vat, branch, requestPayload, requestBy, token, appUser);
            TransactionReference vatTransactionReference = TransactionReference.builder().referenceNumber(requestPayload.getTransactionReference()).build();
            transactionReferenceRepository.createTransactionReference(vatTransactionReference);

            if (!vatTransfer.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !vatTransfer.getStatus().equalsIgnoreCase("SUCCESS")) {
                log.info("transaction failed");
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(ftResponsePayload.getResponseMessage());

                return gson.toJson(responsePayload);
            }
            log.info("Transfer to VAT is successful");

            FundsTransferResponsePayload netIncomeTransfer = netIncomeLocalTransfer(agent, netIncome, branch, requestPayload, requestBy, token, appUser);
            TransactionReference netIncomeTransactionReference = TransactionReference.builder().referenceNumber(requestPayload.getTransactionReference()).build();
            transactionReferenceRepository.createTransactionReference(netIncomeTransactionReference);

            if (!netIncomeTransfer.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode()) || !netIncomeTransfer.getStatus().equalsIgnoreCase("SUCCESS")) {
                log.info("transaction failed");
                responsePayload.setStatus("FAILED");
                responsePayload.setMessage(ftResponsePayload.getResponseMessage());

                return gson.toJson(responsePayload);
            }
            else {
                log.info("Transfer to netIncome is successful");
                responsePayload.setStatus("SUCCESS");
                responsePayload.setTransactionReference(transactionReference1.getReferenceNumber());
                return gson.toJson(responsePayload);
            }
        }
        catch (Exception ex) {
            //Log the response
            log.info("An error occurred");
            genericService.generateLog("Errand Pay Cashout Notification", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getTransactionReference());
            responsePayload.setStatus(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            responsePayload.setMessage(ex.getMessage());
            log.info("exception message ---->>> {}", ex.getMessage());
            ex.printStackTrace();
            return gson.toJson(responsePayload);
        }
    }

    private FundsTransferResponsePayload netIncomeLocalTransfer(AccionAgent agent, BigDecimal netIncome, Branch branch, ErrandPayCashoutNotification requestPayload, String requestBy, String token, AppUser appUser) throws Exception {
        String narration = "EPay-Inc" +
                "/" + agent.getAgentAccountNumber() +
                "/" + requestPayload.getTransactionReference();

        log.info("net income to be transferred ---------->>>>>>>>>> {}", netIncome);

        //Generate the funds transfer request payload to Omnix
        LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
        ftRequestPayload.setMobileNumber(agent.getAgentMobile());
        ftRequestPayload.setDebitAccount(receivableCommission);
        ftRequestPayload.setCreditAccount(incomeAccount);
        ftRequestPayload.setAmount(String.valueOf(netIncome));
        ftRequestPayload.setNarration(narration);
        ftRequestPayload.setTransType("ACAL");
        ftRequestPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        ftRequestPayload.setInputter(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setAuthorizer(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setNoOfAuthorizer("0");
        ftRequestPayload.setRequestId(generateRequestId());
        ftRequestPayload.setToken(token);
        ftRequestPayload.setHash("42873198472t3grfvf3r87t2831");

        String ftResponseJson = gruppService.internalLocalTransfer(ftRequestPayload, appUser, branch, token, "");
        FundsTransferResponsePayload ftResponsePayload = gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
        log.info("response from processing NET income  transfer transfer --->> {}", ftResponsePayload);
        return ftResponsePayload;
    }

    private FundsTransferResponsePayload vatLocalTransfer(AccionAgent agent, BigDecimal vat, Branch branch, ErrandPayCashoutNotification requestPayload, String requestBy, String token, AppUser appUser) throws Exception {

        String narration = "EPay-VAT" +
                "/" + agent.getAgentAccountNumber() +
                "/" + requestPayload.getTransactionReference();

        log.info("amount for VAT transfer -------------->>>>>>>>>>> {}", vat);
        //Generate the funds transfer request payload to Omnix
        LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
        ftRequestPayload.setMobileNumber(agent.getAgentMobile());
        ftRequestPayload.setDebitAccount(receivableCommission);
        ftRequestPayload.setCreditAccount(vatPayableAccount);
        ftRequestPayload.setAmount(String.valueOf(vat));
        ftRequestPayload.setNarration(narration);
        ftRequestPayload.setTransType("ACA");
        ftRequestPayload.setBranchCode("NG0010068"); // Defaulted to the Digital Branch
        ftRequestPayload.setInputter(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setAuthorizer(requestBy + "-" + agent.getAgentMobile());
        ftRequestPayload.setNoOfAuthorizer("0");
        ftRequestPayload.setRequestId(generateRequestId());
        ftRequestPayload.setToken(token);

        String ftResponseJson = gruppService.internalLocalTransfer(ftRequestPayload, appUser, branch, token, "");
        FundsTransferResponsePayload ftResponsePayload = gson.fromJson(ftResponseJson, FundsTransferResponsePayload.class);
        log.info("response from processing VAT transfer transfer --->> {}", ftResponsePayload);
        return ftResponsePayload;
    }

    @Override
    public boolean validateAccountBalancePayload(String token, ErrandPayBalanceRequest requestPayload) {
        return true;
    }

    @Override
    public String processAccountBalance(String token, ErrandPayBalanceRequest requestPayload) {
        ErrandPayBalanceResponse responsePayload = new ErrandPayBalanceResponse();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        //Log the request
        String requestJson = gson.toJson(requestPayload);
        String requestId = genericService.generateTransRef("GR");
//            requestPayload.setRequestId(requestId);
        genericService.generateLog("Errand Pay Agent Account Balance", token, requestJson, "API Request", "INFO", requestId);
        try {
            //Check if the transaction is coming from GRUPP
            if (!"GRUPP".equalsIgnoreCase(requestBy)) {
                //Log the error
                genericService.generateLog("Errand Pay Agent Account Balance", token, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Errand Pay Agent Account Balance", "", channel, messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH), requestId, 'F');
                responsePayload.setBalance(0L);
                responsePayload.setAgent_status(messageSource.getMessage("appMessages.transaction.disallowed", new Object[0], Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(requestPayload.getSerial_number(), "ErrandPay");
            if (agent == null) {
                //Log the error
                genericService.generateLog("Errand Pay Agent Account Balance", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Errand Pay Agent Account Balance", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH), requestId, 'F');
                responsePayload.setBalance(0L);
                responsePayload.setAgent_status(messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the account number for the agent is mapped
            if (agent.getAgentAccountNumber() == null || agent.getAgentAccountNumber().equalsIgnoreCase("")) {
                //Log the error
                genericService.generateLog("Grupp Agent Account Balance", token, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Errand Pay Agent Account Balance", "", channel, messageSource.getMessage("appMessages.agent.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH), requestId, 'F');
                responsePayload.setBalance(0L);
                responsePayload.setAgent_status(messageSource.getMessage("appMessages.agent.account.notexist", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Check if the agent is enabled
            if (!"ACTIVE".equalsIgnoreCase(agent.getStatus())) {
                //Log the error
                genericService.generateLog("Errand Pay Agent Account Balance", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH), "API Response", "INFO", requestId);
                //Create User Activity log
                genericService.createUserActivity("", "Errand Pay Agent Account Balance", "", channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), requestId, 'F');
                responsePayload.setBalance(0L);
                responsePayload.setAgent_status(messageSource.getMessage("appMessages.customer.inactive", new Object[]{requestPayload.getSerial_number()}, Locale.ENGLISH));
                return gson.toJson(responsePayload);
            }

            //Call the account microservices
            String accBalResponseJson = gruppService.checkAccountBalance(agent.getAgentAccountNumber(), token);
            AccountBalanceResponsePayload accBalResponse = gson.fromJson(accBalResponseJson, AccountBalanceResponsePayload.class);
            log.info("accBalResponse ----->>> {}", accBalResponse);
            if (!accBalResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                log.info("call was not successful");
                responsePayload.setBalance(0L);
                OmnixResponsePayload errorResponse = gson.fromJson(accBalResponseJson, OmnixResponsePayload.class);
                responsePayload.setAgent_status(errorResponse.getResponseMessage());
                return gson.toJson(responsePayload);
            }

            //Transaction was successful at this point
            log.info("call was successful");
            log.info("account balance ====>> {}", accBalResponse.getAvailableBalance());
            String accountBalance = accBalResponse.getAvailableBalance();
            String newAccountBalance = accountBalance.replace(",", "");
            log.info("new account balance ====>> {}", newAccountBalance);

            responsePayload.set_pin_valid(true);
            responsePayload.setBalance(Double.parseDouble(newAccountBalance));
            responsePayload.setAgent_status("Active");
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            //Log the response
            genericService.generateLog("Errand Pay Agent Account Balance", token, ex.getMessage(), "API Error", "DEBUG", requestId);
            responsePayload.setBalance(0L);
            responsePayload.setAgent_status(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    public static String generateRequestId() {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int ID_LENGTH = 12;
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder uniqueId = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            uniqueId.append(CHARACTERS.charAt(randomIndex));
        }
        return uniqueId.toString();
    }
}