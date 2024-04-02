/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.Branch;
import com.accionmfb.omnix.agency.model.UserActivity;
import com.accionmfb.omnix.agency.module.agency3Line.payload.request.WithdrawalRequestPayload;
import com.accionmfb.omnix.agency.payload.*;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.GenericService;
import com.accionmfb.omnix.agency.service.NotificationService;
import com.accionmfb.omnix.agency.service.TafjService;
import com.accionmfb.omnix.agency.service.utils.Pair;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
@Slf4j
public class GenericServiceImpl implements GenericService {

    @Autowired
    AgencyRepository agencyRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    NotificationService notificationService;
    @Value("${omnix.t24.host}")
    private String HOST_ADDRESS;
    @Value("${omnix.t24.port}")
    private String PORT_NUMBER;
    @Value("${omnix.t24.ofs.id}")
    private String OFS_ID;
    @Value("${omnix.t24.ofs.source}")
    private String OFS_STRING;
    @Value("${omnix.version.numbering.code}")
    private String accountNumberingCode;
    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;
    @Value("${omnix.channel.user.default}")
    private String t24Credentials;

    @Value("${omnix.middleware.host.ip}")
    private String middlewareHostIP;
    @Value("${omnix.middleware.host.port}")
    private String middlewareHostPort;
    @Value("${omnix.middleware.username}")
    private String middlewareUsername;
    @Value("${omnix.middleware.authorization}")
    private String middlewareAuthorization;
    @Value("${omnix.middleware.signature.method}")
    private String middlewareSignatureMethod;
    @Value("${omnix.middleware.user.secret}")
    private String middlewareUserSecretKey;

    @Autowired
    private TafjService tafjService;
    private static SecretKeySpec secretKey;
    private static byte[] key;
    @Autowired
    Gson gson;
    @Autowired
    MessageSource messageSource;
    Logger logger = LoggerFactory.getLogger(GenericServiceImpl.class);

    @Override
    public void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId) {
        try {
            String requestBy = jwtToken.getUsernameFromToken(token);
            String remoteIP = jwtToken.getIPFromToken(token);
            String channel = jwtToken.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase());
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase()).append(":").append(requestBy.toUpperCase()).append("]");
            strBuilder.append("[").append(app.toUpperCase().toUpperCase()).append(":").append(requestId.toUpperCase()).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            if ("INFO".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isInfoEnabled()) {
                    logger.info(strBuilder.toString());
                }
            }

            if ("DEBUG".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isDebugEnabled()) {
                    logger.error(strBuilder.toString());
                }
            }

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
    }

    @Override
    public void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status) {
        UserActivity newActivity = new UserActivity();
        newActivity.setCustomerId(accountNumber);
        newActivity.setActivity(activity);
        newActivity.setAmount(amount);
        newActivity.setChannel(channel);
        newActivity.setCreatedAt(LocalDateTime.now());
        newActivity.setMessage(message);
        newActivity.setMobileNumber(mobileNumber);
        newActivity.setStatus(status);
        agencyRepository.createUserActivity(newActivity);
    }

//    @Override
//    public String postToT24(String requestBody) {
//        try {
//            T24DefaultConnectionFactory connectionFactory = new T24DefaultConnectionFactory();
//            connectionFactory.setHost(HOST_ADDRESS);
//            connectionFactory.setPort(Integer.valueOf(PORT_NUMBER));
//            connectionFactory.enableCompression();
//
//            Properties properties = new Properties();
//            properties.setProperty("allow input", "true");
//            properties.setProperty(OFS_STRING, OFS_ID);
//            connectionFactory.setConnectionProperties(properties);
//
//            T24Connection t24Connection = connectionFactory.getConnection();
//            String ofsResponse = t24Connection.processOfsRequest(requestBody);
//
//            t24Connection.close();
//            return ofsResponse;
//        } catch (Exception ex) {
//            return ex.getMessage();
//        }
//    }
    @Override
    public String postToT24(String ofsRequest) {
        WithdrawalRequestPayload request = OfsRequest.builder()
                .ofsRequest(ofsRequest)
                .build();
        OfsResponse ofsResponse = tafjService.sendOfsRequest(request);
        return ofsResponse.getOfsResponse();
    }

    @Override
    public String hash(String plainText, String algorithm) {
        StringBuilder hexString = new StringBuilder();
        if (algorithm.equals("SHA512")) {
            algorithm = "SHA-512";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase();
    }

    public static SecretKeySpec setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
            return secretKey;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String validateT24Response(String responseString) {
        String responsePayload = null;
        if (responseString.contains("Authentication failed")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.authfailed", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Maximum T24 users")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.maxuser", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Failed to receive message")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.failedmessage", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No records were found") || responseString.contains("No entries for the period")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.norecord", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("INVALID COMPANY SPECIFIED")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.invalidcoy", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("java.lang.OutOfMemoryError")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.outofmem", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Failed to connect to host")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.failedhost", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No Cheques found")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nocheque", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Unreadable")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.unreadable", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("MANDATORY INPUT")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.inputman", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Some errors while encountered")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.someerrors", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Some override conditions have not been met")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.override", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("don't have permissions to access this data")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nopermission", new Object[0], Locale.ENGLISH);
        }

        if ("<Unreadable>".equalsIgnoreCase(responseString)) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.unreadable", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("User has no id")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.noid", new Object[0], Locale.ENGLISH);
        }

        if ("java.net.SocketException: Unexpected end of file from server".equalsIgnoreCase(responseString)) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.endoffile", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No Cash available")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nocash", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("INVALID ACCOUNT")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.invalidaccount", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("MISSING") && !responseString.substring(0, 4).equals("\"//1")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.net.SocketException")
                || responseString.contains("java.net.ConnectException")
                || responseString.contains("java.net.NoRouteToHostException")
                || responseString.contains("Connection timed out")
                || responseString.contains("Connection refused")) {
            responsePayload = responseString;
        }

        if (responseString.contains("SECURITY VIOLATION")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NOT SUPPLIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NO EN\\\"\\t\\\"TRIES FOR PERIOD")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.norecord", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("CANNOT ACCESS RECORD IN ANOTHER COMPANY")) {
            responsePayload = responseString;
        }

        if ("NO DATA PRESENT IN MESSAGE".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("//-1") || responseString.contains("//-2")) {
            responsePayload = responseString;
        }

        if ("RECORD MISSING".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID/ NO SIGN ON NAME SUPPLIED DURING SIGN ON PROCESS")) {
            responsePayload = responseString;
        }

        return responsePayload == null ? null : responsePayload;
    }

    @Override
    public String generateTransRef(String transType) {
        long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return transType + number;
    }

    @Override
    public String getT24TransIdFromResponse(String response) {
        String[] splitString = response.split("/");
        return splitString[0].replace("\"", "");
    }

    @Override
    public String getTextFromOFSResponse(String ofsResponse, String textToExtract) {
        try {
            String[] splitOfsResponse = ofsResponse.split(",");
            for (String str : splitOfsResponse) {
                String[] splitText = str.split("=");
                if (splitText[0].equalsIgnoreCase(textToExtract)) {
                    return splitText[1].isBlank() ? "" : splitText[1].trim();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    @Override
    public String formatDateWithHyphen(String dateToFormat) {
        StringBuilder newDate = new StringBuilder(dateToFormat);
        if (dateToFormat.length() == 8) {
            newDate.insert(4, "-").insert(7, "-");
            return newDate.toString();
        }

        return "";
    }

    @Override
    public String generateAccountNumbering(String customerNumber, String userCredentials, String branchCode, String productId, String token, String requestId) {
        OmnixResponsePayload responsePayload = new OmnixResponsePayload();
        try {
            StringBuilder ofsBase = new StringBuilder();
            ofsBase.append("CUSTOMER.NO::=").append(customerNumber).append(",");
            ofsBase.append("PDT.CODE::=").append(productId).append(",");
            ofsBase.append("CREATED.Y.N::=Y");
            String ofsRequest = accountNumberingCode.trim() + "," + userCredentials + "/" + branchCode.trim() + ",," + ofsBase;
            //Generate the OFS log
            generateLog("Account Opening", token, ofsRequest, "OFS Request", "INFO", requestId);
            String response = postToT24(ofsRequest);
            //Generate the OFS Response log
            generateLog("Account Opening", token, response, "OFS Response", "INFO", requestId);
            if (response.contains("//1")) {
                String accountNumber = getTextFromOFSResponse(response, "ACCT.CODE:1:1");
                responsePayload.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
                responsePayload.setResponseMessage(accountNumber);
                return gson.toJson(responsePayload);
            }
            responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            responsePayload.setResponseMessage(response);
            return gson.toJson(responsePayload);
        } catch (Exception ex) {
            responsePayload.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
            responsePayload.setResponseMessage(ex.getMessage());
            return gson.toJson(responsePayload);
        }
    }

    @Override
    public String generateMnemonic(int max) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder mnemonic = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            while (max-- != 0) {
                int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
                mnemonic.append(ALPHA_NUMERIC_STRING.charAt(character));
            }

            if (!Character.isDigit(mnemonic.toString().charAt(0))) {
                return mnemonic.toString();
            }
        }
        return mnemonic.toString();
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        log.info("trying to encrypt");
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.valueOf(startMorning);
        int morningEnd = Integer.valueOf(endMorning);
        int afternoonStart = Integer.valueOf(startAfternoon);
        int afternoonEnd = Integer.valueOf(endAfternoon);
        int eveningStart = Integer.valueOf(startEvening);
        int eveningEnd = Integer.valueOf(endEvening);
        int nightStart = Integer.valueOf(startNight);
        int nightEnd = Integer.valueOf(endNight);
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }

    @Override
    public Branch getBranchUsingBranchCode(String branchCode) {
        return agencyRepository.getBranchUsingBranchCode(branchCode);
    }

    @Override
    public String hashCableTVValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getSmartCard().trim());
        rawString.add(requestPayload.getBillerId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashElectricityValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getMeterNumber().trim());
        rawString.add(requestPayload.getBillerId().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashNIPValidationRequest(OmnixRequestPayload requestPayload) {
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
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAirtimeValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getThirdPartyMobileNumber().trim());
        rawString.add(requestPayload.getThirdPartyTelco().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashLocalTransferValidationRequest(LocalTransferWithInternalPayload requestPayload) {
        log.info("trying to hash");
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        log.info("set raw string ");
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAccountDetailsValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAccountBalanceRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAccountBalanceRequest2(AccountNumberPayload requestPayload) {
        log.info("------------- about to Hash ------------------");
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashLocalFundsTransferWithChargesValidationRequest(LocalTransferWithChargesPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        for (ChargeTypes ch : requestPayload.getChargeTypes()) {
            rawString.add(ch.getChargeType());
            rawString.add(ch.getChargeAmount());
        }
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashLocalFundsTransferValidationRequest(LocalTransferWithChargesPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashNIPNameEnquiryValidationRequest(NIPNameEnquiryPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBeneficiaryAccount().trim());
        rawString.add(requestPayload.getBeneficiaryBankCode().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAccountStatementRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getStartDate().trim());
        rawString.add(requestPayload.getEndDate().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashCustomerDetailsRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashAccountOpeningRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getCustomerNumber().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getProductCode().trim());
        rawString.add(requestPayload.getAccountOfficer().trim());
        rawString.add(requestPayload.getOtherOfficer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashCableTVTransRefValidationRequest(OmnixRequestPayload requestPayload) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String hashCableTVBillerValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBiller().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashElectricityDetailsValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBiller().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashElectricityBillersValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBiller().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashElectricityBillerValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getSmartCard().trim());
        rawString.add(requestPayload.getBiller().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashCustomerWithBvnRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getMaritalStatus().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getSector().trim());
        rawString.add(requestPayload.getStateOfResidence().trim());
        rawString.add(requestPayload.getCityOfResidence().trim());
        rawString.add(requestPayload.getResidentialAddress().trim());
        rawString.add(requestPayload.getAccountOfficer().trim());
        rawString.add(requestPayload.getOtherOfficer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashCustomerWithoutBvnRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getLastName().trim());
        rawString.add(requestPayload.getOtherName().trim());
        rawString.add(requestPayload.getDob().trim());
        rawString.add(requestPayload.getGender().trim());
        rawString.add(requestPayload.getMaritalStatus().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getSector().trim());
        rawString.add(requestPayload.getStateOfResidence().trim());
        rawString.add(requestPayload.getCityOfResidence().trim());
        rawString.add(requestPayload.getResidentialAddress().trim());
        rawString.add(requestPayload.getAccountOfficer().trim());
        rawString.add(requestPayload.getOtherOfficer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashTransactionQueryRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getTransRef().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashNIPNameEnquiryRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBeneficiaryAccount().trim());
        rawString.add(requestPayload.getBeneficiaryBankCode().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashDataValidationRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getThirdPartyMobileNumber().trim());
        rawString.add(requestPayload.getThirdPartyTelco().trim());
        rawString.add(requestPayload.getDataPlanId().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());

    }

    @Override
    public String formatOfsUserCredentials(String ofs, String userCredentials) {
        String[] userCredentialsSplit = userCredentials.split("/");
        String newUserCredentials = userCredentialsSplit[0] + "/#######";
        String newOfsRequest = ofs.replace(userCredentials, newUserCredentials);
        return newOfsRequest;
    }

    @Override
    public String getTransactionType(String channel, String transType) {
        if (channel.equalsIgnoreCase("USSD")) {
            if (transType.equalsIgnoreCase("LOCAL FT")) {
                return "ACLT";
            } else if (transType.equalsIgnoreCase("NIP")) {
                return "ACIT";
            } else if (transType.equalsIgnoreCase("AIRTIME")) {
                return "ACMA";
            } else if (transType.equalsIgnoreCase("CABLE TV")) {
                return "ACMB";
            } else if (transType.equalsIgnoreCase("ELECTRICITY")) {
                return "ACMB";
            } else {
                return "ACTF";
            }
        }

        if (channel.equalsIgnoreCase("IBANKING")) {
            if (transType.equalsIgnoreCase("LOCAL FT")) {
                return "ACIL";
            } else if (transType.equalsIgnoreCase("NIP")) {
                return "ACII";
            } else if (transType.equalsIgnoreCase("AIRTIME")) {
                return "ACIA";
            } else if (transType.equalsIgnoreCase("CABLE TV")) {
                return "ACIB";
            } else if (transType.equalsIgnoreCase("ELECTRICITY")) {
                return "ACIB";
            } else {
                return "ACTF";
            }
        }

        if (channel.equalsIgnoreCase("MOBILE")) {
            if (transType.equalsIgnoreCase("LOCAL FT")) {
                return "ACML";
            } else if (transType.equalsIgnoreCase("NIP")) {
                return "ACMI";
            } else if (transType.equalsIgnoreCase("AIRTIME")) {
                return "ACMA";
            } else if (transType.equalsIgnoreCase("CABLE TV")) {
                return "ACMB";
            } else if (transType.equalsIgnoreCase("ELECTRICITY")) {
                return "ACMB";
            } else {
                return "ACTF";
            }
        }

        if (channel.equalsIgnoreCase("AGENCY")) {
            if (transType.equalsIgnoreCase("LOCAL FT")) {
                return "ACAL";
            } else if (transType.equalsIgnoreCase("NIP")) {
                return "ACAI";
            } else if (transType.equalsIgnoreCase("AIRTIME")) {
                return "ACAA";
            } else if (transType.equalsIgnoreCase("CABLE TV")) {
                return "ACAB";
            } else if (transType.equalsIgnoreCase("ELECTRICITY")) {
                return "ACAB";
            } else {
                return "ACTF";
            }
        }

        if (channel.equalsIgnoreCase("DEFAULT")) {
            if (transType.equalsIgnoreCase("LOCAL FT")) {
                return "ACTF";
            } else if (transType.equalsIgnoreCase("NIP")) {
                return "ACTF";
            } else if (transType.equalsIgnoreCase("AIRTIME")) {
                return "ACTF";
            } else if (transType.equalsIgnoreCase("CABLE TV")) {
                return "ACTF";
            } else if (transType.equalsIgnoreCase("ELECTRICITY")) {
                return "ACTF";
            } else {
                return "ACTF";
            }
        }

        //The channel is unknown at this point.
        return "ACTF";  //This is defaulted to Accion Credit Funds Transfer of AIS
    }

    @Override
    public String decryptTriftaString(String textToDecrypt, String encryptionKey) {
        try {
            String iv = "VR6kN4wGjaCaVm70";
            encryptionKey = "N40D3NZJLSNTU180";
            IvParameterSpec initialVector = new IvParameterSpec(iv.getBytes());
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, initialVector);
            byte[] encryptedByteArray = (new org.apache.commons.codec.binary.Base64()).decode(textToDecrypt.getBytes());
            byte[] decryptedByteArray = cipher.doFinal(encryptedByteArray);
            String decryptedData = new String(decryptedByteArray, "UTF8");
            return decryptedData;

        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String encryptTriftaString(String textToEncrypt, String encryptionKey) {
        try {
            String iv = encryptionKey.length() < 16 ? "0" + encryptionKey : encryptionKey;
            String keys = encryptionKey.length() < 16 ? encryptionKey + "0" : encryptionKey;
            SecretKeySpec skeySpec = new SecretKeySpec(keys.getBytes("UTF-8"), "AES");
            IvParameterSpec initialVector = new IvParameterSpec(iv.getBytes());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, initialVector);

            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }

    }

    private static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }

        return hex.toString();
    }

    @Override
    public String generateFTOFS(String transRef, String debitAccount, String creditAccount, String amount, String narration, String transType, String inputter, String authorizer) {
        String t24Date = getPostingDate();
        StringBuilder ofsBase = new StringBuilder("");
        ofsBase.append("DR.ADVICE.REQD.Y.N::=N").append(",");
        ofsBase.append("CR.ADVICE.REQD.Y.N::=N").append(",");
        ofsBase.append("TXN.SOURCE::=BRANCH").append(",");
        ofsBase.append("DEBIT.CURRENCY::=NGN").append(",");
        ofsBase.append("CREDIT.CURRENCY::=NGN").append(",");
        ofsBase.append("TRANSACTION.TYPE::=").append(transType).append(",");
        ofsBase.append("CREDIT.THEIR.REF::=").append(creditAccount).append(",");
        ofsBase.append("CREDIT.VALUE.DATE::=").append(t24Date).append(",");
        ofsBase.append("PAY.TO::=").append(transRef).append(",");
        ofsBase.append("DEBIT.ACCT.NO::=").append(debitAccount).append(",");
        ofsBase.append("CREDIT.ACCT.NO::=").append(creditAccount).append(",");
        ofsBase.append("DEBIT.AMOUNT::=").append(amount.replace(",", "")).append(",");
        String paymentDetailsString = generateMultivalue("PAYMENT.DETAILS", narration);
        String ordeingDetailsString = generateMultivalue("ORDERING.CUST", narration);
        ofsBase.append(paymentDetailsString).append(",");
        ofsBase.append(ordeingDetailsString).append(",");
        ofsBase.append("AIS.INPUTTER::=").append("grupp-").append(",");
        ofsBase.append("AIS.AUTHORISER::=").append(authorizer);
        return ofsBase.toString();
    }

    public String generateMultivalue(String fieldName, String stringToParse) {
        StringBuilder strBuilder = new StringBuilder();
        int strLen = stringToParse.trim().length();
        String firstString = "", secondString = "", thirdString = "", fourthString = "";
        if (strLen <= 30) {
            firstString = stringToParse.substring(0, strLen).toUpperCase(Locale.ENGLISH);
            if (!firstString.matches("^[A-Za-z].*$")) {
                firstString = "'" + firstString;
            }
            strBuilder.append(fieldName).append(":1:1::=").append(firstString);
        } else if (strLen > 30 && strLen <= 60) {
            firstString = stringToParse.substring(0, 30).toUpperCase(Locale.ENGLISH);
            if (!firstString.matches("^[A-Za-z].*$")) {
                firstString = "'" + firstString;
            }
            secondString = stringToParse.substring(30, strLen).toUpperCase(Locale.ENGLISH);
            if (!secondString.matches("^[A-Zaa-z].*$")) {
                secondString = "'" + secondString;
            }
            strBuilder.append(fieldName).append(":1:1::=").append(firstString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":2:1::=").append(secondString.toUpperCase(Locale.ENGLISH));
        } else if (strLen > 60 && strLen <= 90) {
            firstString = stringToParse.substring(0, 30).toUpperCase(Locale.ENGLISH);
            if (!firstString.matches("^[A-Za-z].*$")) {
                firstString = "'" + firstString;
            }
            secondString = stringToParse.substring(30, 60).toUpperCase(Locale.ENGLISH);
            if (!secondString.matches("^[A-Za-z].*$")) {
                secondString = "'" + secondString;
            }
            thirdString = stringToParse.substring(60, strLen).toUpperCase(Locale.ENGLISH);
            if (!firstString.matches("^[A-Za-z].*$")) {
                thirdString = "'" + thirdString;
            }
            strBuilder.append(fieldName).append(":1:1::=").append(firstString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":2:1::=").append(secondString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":3:1::=").append(thirdString.toUpperCase(Locale.ENGLISH));
        } else if (strLen > 90) {
            firstString = stringToParse.substring(0, 30).toUpperCase(Locale.ENGLISH);
            if (!firstString.matches("^[A-Za-z].*$")) {
                firstString = "'" + firstString;
            }
            secondString = stringToParse.substring(30, 60).toUpperCase(Locale.ENGLISH);
            if (!secondString.matches("^[A-Za-z].*$")) {
                secondString = "'" + secondString;
            }
            thirdString = stringToParse.substring(60, 90).toUpperCase(Locale.ENGLISH);
            if (!thirdString.matches("^[A-Za-z].*$")) {
                thirdString = "'" + thirdString;
            }
            fourthString = stringToParse.substring(90, strLen).toUpperCase(Locale.ENGLISH);
            if (!fourthString.matches("^[A-Za-z].*$")) {
                fourthString = "'" + fourthString;
            }
            strBuilder.append(fieldName).append(":1:1::=").append(firstString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":2:1::=").append(secondString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":3:1::=").append(thirdString.toUpperCase(Locale.ENGLISH)).append(",");
            strBuilder.append(fieldName).append(":4:1::=").append(fourthString.toUpperCase(Locale.ENGLISH));
        }

        return strBuilder.toString();
    }

    public String getPostingDate() {
        //This returns the current posting date
        String ofsRequest = "DATES,INPUT/S/PROCESS," + t24Credentials + "/,NG0010001";
        String response = postToT24(ofsRequest);
        if (response == null) {
            return "Invalid Posting Date";
        }
        return getTextFromOFSResponse(response, "TODAY:1:1");
    }

    @Async
    @Override
    public CompletableFuture<String> sendDebitSMS(NotificationPayload requestPayload) {
        String smsMessage = "Debit of N"
                + requestPayload.getAmount()
                + " on " + requestPayload.getAccountNumber()
                + " at " + requestPayload.getBranch()
                + " on " + requestPayload.getTransDate()
                + " by " + requestPayload.getTransTime() + ". " + requestPayload.getNarration()
                + ". Balance N" + requestPayload.getAccountBalance()
                + ". Info 07000222466, #StaySafe.";

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage);
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("D");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setToken(requestPayload.getToken());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
//        String smsResponse = notificationService.smsNotification(requestPayload.getToken(), requestJson);  //Disabled as requested by Emmanuel Akhigbe 2022-06-06
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    public String postToMiddleware(String requestEndpoint, String requestBody) {
        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndpoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String postToMiddleware(String requestEndPoint, String requestBody, String middlewareAuthorization, String middlewareUsername) {
        try {
            String middlewareEndpoint = "http://" + this.middlewareHostIP + ":" + this.middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", new Object[]{NONCE, TIMESTAMP, middlewareUsername, this.middlewareUserSecretKey});
            String SIGNATURE = hash(SignaturePlain, this.middlewareSignatureMethod);
            Unirest.setTimeouts(0L, 0L);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint).header("Authorization", middlewareAuthorization).header("SignatureMethod", this.middlewareSignatureMethod).header("Accept", "application/json").header("Timestamp", TIMESTAMP).header("Nonce", NONCE).header("Content-Type", "application/json").header("Signature", SIGNATURE).body(requestBody).asString();
            return (String) httpResponse.getBody();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public String hashSMSNotificationRequest(SMSRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getMessage().trim());
        rawString.add(requestPayload.getSmsFor().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public Pair<String, String> getResponseMessage(String sResponse) {
        if (sResponse.contains("DUPLICATE.TRAP:1:1=TRUE") || sResponse.contains("POSSIBLE DUPLICATE CONTRACT")) {
            return new Pair<>(ResponseCodes.DUPLICATE_TRANSACTION.getResponseCode(), "POSSIBLE DUPLICATE TRANSACTION");
        } else if (sResponse.contains("Maximum T24 users already signed on (120) DURING SIGN ON PROCESS")) {
            return new Pair<>(ResponseCodes.SYSTEM_MALFUNCTION.getResponseCode(), "Sorry, transaction not completed. Please try again");
        } else if (sResponse.contains("Post NO Debit Transaction")) {
            return new Pair<>(ResponseCodes.FAILED_TRANSACTION.getResponseCode(), "Your account has restrictions debit transaction.");
        } else if (sResponse.contains("is inactive - Transaction code 146")) {
            return new Pair<>(ResponseCodes.FAILED_TRANSACTION.getResponseCode(), "Your is either inactive or dormarnt.");
        } else if (sResponse.contains("Failed to connect to host")) {
            return new Pair<>(ResponseCodes.SYSTEM_MALFUNCTION.getResponseCode(), "Sorry, transaction not completed. Please try again");
        } else if (sResponse.contains("/1") && sResponse.split("/")[0] != null && !(sResponse.split("/")[0]).isEmpty()) {
            return new Pair<>(ResponseCodes.SUCCESS_CODE.getResponseCode(), "Approved Successfully.");
        } else if (sResponse.toUpperCase().contains("WILL FALL BELOW LOCKED")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have insufficient funds for this transaction.");
        } else if (sResponse.toUpperCase().contains("DEPOSIT EXCEEDS MAXIMUM BALANCE")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have exeeded maximum account balance limit.");
        } else if (sResponse.toUpperCase().contains("WITHDRAWL MAKES A/C BAL LESS THAN MIN BAL")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have insufficient funds for this transaction.");
        } else if (sResponse.toUpperCase().contains("ACCOUNT RESTRICTION")) {
            return new Pair<>(ResponseCodes.FAILED_TRANSACTION.getResponseCode(), "Your account has restrictions debit transaction.");
        } else if (sResponse.toUpperCase().contains("A/C BALANCE STILL LESS THAN MINIMUM BAL")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have insufficient funds for this transaction.");
        } else if (sResponse.toUpperCase().contains("WITHDRAWL MAKES A/C BAL LESS THAN MIN BAL")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have insufficient funds for this transaction.");
        } else if (sResponse.toUpperCase().contains("SAME DEBIT AND CREDIT ACCOUNT")) {
            return new Pair<>(ResponseCodes.SAME_ACCOUNT.getResponseCode(), "Credit account is the same as debit account.");
        } else if (sResponse.toUpperCase().contains("ACCT MUST BE IN OUR COMPANY") || sResponse.toUpperCase().contains("CREDIT.ACCT.NO:1:1=RECORD NOT FOUND") || sResponse.toUpperCase().contains("DEBIT.ACCT.NO:1:1=MISSING ACCOUNT - RECORD")) {
            return new Pair<>(ResponseCodes.INVALID_ACCOUNT.getResponseCode(), "Invalid Account.");
        } else if (sResponse.toUpperCase().contains("UNAUTHORISED OVERDRAFT")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "You have insufficient funds for this transaction.");
        } else if (sResponse.toUpperCase().contains("CREDIT ACCT CCY NOT EQ CREDIT CCY") || sResponse.toUpperCase().contains("DEBIT ACCT CCY NOT EQ DEBIT CCY")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "Cross Currency Transaction is not allowed.");
        } else if (sResponse.toUpperCase().contains("AMOUNT NOT NUMERIC")) {
            return new Pair<>(ResponseCodes.INSUFFICIENT_BALANCE.getResponseCode(), "AMOUNT NOT NUMERIC");
        } else {
            log.info("Transalte this error: ".concat(sResponse));
            return new Pair<>(ResponseCodes.FAILED_TRANSACTION.getResponseCode(), "Unknown error has occured.");
        }
    }
}
