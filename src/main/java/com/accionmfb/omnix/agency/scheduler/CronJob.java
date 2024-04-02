package com.accionmfb.omnix.agency.scheduler;

import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.*;
import com.accionmfb.omnix.agency.payload.ExcelTransaction;
import com.accionmfb.omnix.agency.payload.GruppCashoutNotificationPayload;
import com.accionmfb.omnix.agency.payload.GruppResponsePayload;
import com.accionmfb.omnix.agency.payload.LocalTransferWithInternalPayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.repository.NotificationHistoryRepo;
import com.accionmfb.omnix.agency.service.GenericService;
import com.accionmfb.omnix.agency.service.GruppService;
import com.accionmfb.omnix.agency.service.impl.CsvReaderService;
import com.opencsv.exceptions.CsvException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class CronJob {

    @Autowired
    private NotificationHistoryRepo notificationHistoryRepo;
    @Autowired
    CsvReaderService csvReaderService;
    @Autowired
    GenericService genericService;
    @Autowired
    GruppService gruppService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    AgencyRepository agencyRepository;

    @Value("${omnix.agency.banking.grupp.receivable}")
    private String gruppReceivableAccount;

    @Value("${omnix.agency.banking.grupp.splitaccount}")
    private String gruppSplitAccount;
    @Value("${omnix.agency.banking.grupp.fee}")
    private String gruppFee;

    String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";

    private static final String AGENT_VENDOR = "Grupp";

    @Scheduled(fixedDelay = 600000) // runs every 10 minutes
    public void readCsvFileFromDirectory() throws Exception {
        log.info("---------------------------------------------- cron to read excel file from directory called ----------------------------------------------------");

        final String DIRECTORY_PATH = "C:\\Users\\Public\\agencyReconciliation";

        File directory = new File(DIRECTORY_PATH);

        if (directory.exists() && directory.isDirectory()) {
            processFilesInExcelSheet(directory);
        } else {
            log.info("Directory specified does not exist");
        }
        // check for pending transactions and process if limit is valid
        checkForPendingTransactionsDueForProcessing();

        // check for approved transactions and process if limit is valid
//        postApprovedTransactions();
    }

    private void processFilesInExcelSheet(File directory) throws Exception {
        File[] files = directory.listFiles();
        if (files != null) {
            log.info("files in the directory ---------->>> {}", (Object) Arrays.stream(files).toArray());
            for (File file : files) {
                if (csvReaderService.isCsvFile(file)) {
                    try {
                        List<ExcelTransaction> pojoList = csvReaderService.readCsvFile(file);
                        log.info("pojo size --------->>> {}", pojoList.size());
                        for (ExcelTransaction transaction : pojoList) {
                            log.info("Transactions terminal to check ----------->>>> {}", transaction.getTerminalId());
                            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(transaction.getTerminalId(), "Grupp");
                            log.info("found agent ======>>>>> {}", agent);
                            log.info("transaction due for processing ----->> {}", transaction);
                            AgentTranLog notificationHistory = notificationHistoryRepo.getTransactionByTranRef(transaction.getUniqueId());
                            log.info("Notification ------->>>>>>> {}", notificationHistory);

                            if (transaction.getStatus().trim().equalsIgnoreCase("SUCCESSFUL")) {

                                if (notificationHistory == null && agent != null) {
                                    log.info("notification History is null");
                                    // create a new notification history and process it immediately. Transaction was settled buth has no notification record.
                                    AgentTranLog notification = generateNotificationHistoryPayloadForNullRecords(transaction, agent);
                                    processNotificationRecord(token, notification);
                                } else {
                                    log.info("notification is not null");
                                    extendLimitForProcessedNotifications(transaction, agent, notificationHistory);
                                }
                            }
                        }
                        Thread.sleep(30000); // sleep for 30 seconds before moving file to another folder
                        processAndMoveFile(file);
                    } catch (IOException | CsvException e) {
                        log.error("Error processing CSV file: " + file.getName().concat(" ").concat(e.getMessage()));
                    }
                } else {
                    log.info("This file is not a CSV file: " + file.getName());
                }
            }
        } else {
            log.info("No file seen in the directory");
        }
    }

    public void extendLimitForProcessedNotifications(ExcelTransaction transaction, AccionAgent agent, AgentTranLog notificationHistory) {
        // reduce limit
        if (agent != null) {
            try {
                BigDecimal amount = new BigDecimal(Double.parseDouble(transaction.getAmount().trim().replaceAll(",", "")));
                BigDecimal extendedLimit = agent.getRemainingLimit().subtract(amount);

                if (extendedLimit.doubleValue() < 0) {
                    extendedLimit = BigDecimal.valueOf(0);  // cap the lowest limit to 0
                }
                if (extendedLimit.doubleValue() > 3000000) {
                    extendedLimit = BigDecimal.valueOf(3000000); // cap the highest limit to 3,000,000
                }
                agent.setRemainingLimit(extendedLimit);
                agencyRepository.updateAccionAgent(agent);

                notificationHistory.setSettled("true");
                notificationHistoryRepo.updateTransaction(notificationHistory);
            } catch (NumberFormatException numberFormatException) {
                log.info("Cannot increase limit because error occured: ".concat(numberFormatException.getMessage()));
            }
        } else {
            log.info("Cannot increase limit because Agent is not found");
        }
    }

    public void postApprovedTransactions() throws Exception {
        log.info("----------------------------------------------cron to post Approved Transaction----------------------------------------------------");
        List<AgentTranLog> approvedNotificationHistory = notificationHistoryRepo.findApprovedNotifications();
        log.info("Approved notifications ========>>> {}", approvedNotificationHistory);
        if (approvedNotificationHistory == null || approvedNotificationHistory.isEmpty()) {
            log.info("No Approved transactions today");
            return;
        }

        for (AgentTranLog transaction : approvedNotificationHistory) {
            log.info("in the loop");
            processNotificationRecord(token, transaction);
            transaction.setSettled("true");
            notificationHistoryRepo.updateTransaction(transaction);

            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(transaction.getTerminalId(), AGENT_VENDOR);

            BigDecimal tranAmount = new BigDecimal(Double.parseDouble(transaction.getAmount().trim().replaceAll(",", "")));
            BigDecimal limitAmount = agent.getRemainingLimit().add(tranAmount);
            agent.setRemainingLimit(limitAmount);
            agencyRepository.updateAccionAgent(agent);
        }
    }

    public void checkForPendingTransactionsDueForProcessing() throws Exception {
        log.info("----------------------------------------------Checking for Pending Transactions Due For Processing----------------------------------------------------");

        List<AgentTranLog> foundPendingNotificationHistory = notificationHistoryRepo.findPendingTransactions("Pending");

        if (CollectionUtils.isEmpty(foundPendingNotificationHistory)) {
            log.info("No pending transactions found");
            return;
        }
        log.info("Transactions to process ----->> {}", foundPendingNotificationHistory);
        processPendingNotificationHistory(foundPendingNotificationHistory);
    }

    private void processPendingNotificationHistory(List<AgentTranLog> pendingNotificationHistoryList) throws Exception {
        for (AgentTranLog notificationHistory : pendingNotificationHistoryList) {
            log.info("Per notification ----->> {}", notificationHistory);

            if (notificationHistory.getTerminalId() == null) {
                log.info("Agent with terminal id {} and agent vendor {} is not found ", notificationHistory.getTerminalId(), AGENT_VENDOR);
                continue;
            }

            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(notificationHistory.getTerminalId(), AGENT_VENDOR);

            if (agent == null) {
                log.info("Agent with terminal id {} and agent vendor {} is not found ", notificationHistory.getTerminalId(), AGENT_VENDOR);
                continue;
            }

            BigDecimal tranAmount = new BigDecimal(Double.parseDouble(notificationHistory.getAmount().trim().replaceAll(",", "")));
            BigDecimal limitAmount = agent.getRemainingLimit();//.subtract(tranAmount);
            log.info("limit amount ---->> {}", limitAmount);
            BigDecimal amountToCheck = limitAmount.add(tranAmount);
            boolean settled = false;
            if (!notificationHistory.getSettled().isBlank() && notificationHistory.getSettled() != null) {
                settled = notificationHistory.getSettled().equalsIgnoreCase("true");
            }
            if (amountToCheck.doubleValue() <= 3000000 && settled) {
                processNotificationRecord(token, notificationHistory);
//                agent.setRemainingLimit(limitAmount);
//                agencyRepository.updateAccionAgent(agent);
            } else {
                log.info("Record not due for processing because it will exceed limit");
            }

        }
    }

    private void processNotificationRecord(String token, AgentTranLog transaction) throws Exception {
        log.info("transaction -->> {}", transaction);
        LocalTransferWithInternalPayload ftRequestPayload = new LocalTransferWithInternalPayload();
        ftRequestPayload.setMobileNumber(transaction.getMobileNumber());
        ftRequestPayload.setDebitAccount(transaction.getDebitAccount());
        ftRequestPayload.setCreditAccount(transaction.getCreditAccount());
        ftRequestPayload.setAmount(transaction.getAmount());
        ftRequestPayload.setNarration(transaction.getNarration());
        ftRequestPayload.setTransType(transaction.getTransType());
        ftRequestPayload.setBranchCode(transaction.getBranchCode());
        ftRequestPayload.setInputter(transaction.getInputter());
        ftRequestPayload.setAuthorizer(transaction.getAuthorizer());
        ftRequestPayload.setNoOfAuthorizer(transaction.getNoOfAuthorizer());
        ftRequestPayload.setRequestId(transaction.getRequestId());
        ftRequestPayload.setToken(token);
        ftRequestPayload.setHash("47DFG43IUWDFNR3");
        log.info("ft payload --- >>> {}", ftRequestPayload);
        BigDecimal transAmount = new BigDecimal(transaction.getAmount());
        BigDecimal percent = new BigDecimal("100");
        BigDecimal grupFee = new BigDecimal(gruppFee).divide(percent).multiply(transAmount);
        if (grupFee.doubleValue() > 50) {
            grupFee = BigDecimal.valueOf(50);
        }
        BigDecimal netAmount = (new BigDecimal(transaction.getAmount())).subtract(grupFee).setScale(2, RoundingMode.CEILING);
        // Fee is 0.6% of the transaction amount. This equals 0.006
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
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        AccionAgent agent = agencyRepository.getAgentUsingTerminalId(transaction.getTerminalId(), "Grupp");
        if (agent.getRemainingLimit() == null) {
            agent.setRemainingLimit(BigDecimal.valueOf(0));
            agencyRepository.updateAccionAgent(agent);
        }
        Customer customer = agencyRepository.getCustomerUsingMobileNumber(agent.getAgentMobile());
        AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);
        Branch branch = genericService.getBranchUsingBranchCode("NG0010068");

        GruppCashoutNotificationPayload request = new GruppCashoutNotificationPayload();
        request.setTerminalId(transaction.getTerminalId());
        request.setReference(transaction.getTranRef());
        request.setAmount(transaction.getAmount());

        GruppResponsePayload responseFromTransfers = gruppService.performAllTransfers(token, transaction, request, requestBy, channel, agent, customer, appUser, branch, netAmount, accionFee, agentAmount, vatAmount, netIncomeAmount, agent.getRemainingLimit());
        log.info("<<<<<------------------------- Response from PERFORMING ALL TRANSFERS -------------------->>>>> {}", responseFromTransfers);
        if (Objects.equals(responseFromTransfers.getStatus(), "SUCCESS")) {
            transaction.setStatus("Closed");
            notificationHistoryRepo.updateTransaction(transaction);
        }
    }

    private AgentTranLog generateNotificationHistoryPayloadForNullRecords(ExcelTransaction transaction, AccionAgent agent) {
        String newTransactionAmount = transaction.getAmount().trim().replaceAll(",", "");
        log.info("the new value ooo ----->> {}", newTransactionAmount);
        BigDecimal transAmount = new BigDecimal(Double.parseDouble(newTransactionAmount));
        BigDecimal transFee;
        transFee = transAmount.multiply(new BigDecimal("0.006"));
        //Check to cap it at N100 max
        if (transFee.compareTo(new BigDecimal(100)) >= 0) {
            log.info("stuff happened");
            transFee = new BigDecimal("100");
        }
        BigDecimal percent = new BigDecimal("100");
        BigDecimal grupFee = new BigDecimal(gruppFee).divide(percent).multiply(transAmount);
        if (grupFee.doubleValue() > 50) {
            grupFee = BigDecimal.valueOf(50);
        }
        BigDecimal accionFee = transFee.subtract(grupFee).setScale(2, RoundingMode.CEILING);
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        BigDecimal netAmount = new BigDecimal(Double.parseDouble(newTransactionAmount)).subtract(grupFee).setScale(2, RoundingMode.CEILING);
        BigDecimal agentAmount = new BigDecimal(netAmount.toString()).subtract(accionFee).setScale(2, RoundingMode.CEILING);

        String narration = "GRP/" + agent.getAgentAccountNumber() + "/" + transaction.getRrn();

        String requestId = generateRequestId();

        AgentTranLog notificationHistory = AgentTranLog.builder().mobileNumber(transaction.getPhoneNumber())
                .debitAccount(gruppReceivableAccount).creditAccount(gruppSplitAccount).amount(String.valueOf(transaction.getAmount()))
                .narration(narration).transType("ACAL").branchCode("NG0010068").inputter(requestBy + "-" + transaction.getPhoneNumber())
                .authorizer(requestBy + "-" + transaction.getPhoneNumber()).requestId(requestId)
                .status("Settled").date(LocalDate.now()).accountNumber(agent.getAgentAccountNumber())
                .requestBy(requestBy).agentAmount(String.valueOf(agentAmount)).accionFee(String.valueOf(accionFee))
                .channel(channel).terminalId(transaction.getTerminalId()).settled("true")
                .tranRef(transaction.getUniqueId()).build();

        notificationHistoryRepo.createTransaction(notificationHistory);
        return notificationHistory;
    }

    private void processAndMoveFile(File file) throws Exception {
        try {
            // Move the file to the destination directory
            csvReaderService.moveFileToDestination(file);
            log.info("File processed and moved successfully: {}", file.getName());
        } catch (IOException e) {
            log.error("Error processing CSV file: " + file.getName(), e);
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
