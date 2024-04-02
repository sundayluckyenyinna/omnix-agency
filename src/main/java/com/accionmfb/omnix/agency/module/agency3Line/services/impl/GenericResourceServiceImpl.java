package com.accionmfb.omnix.agency.module.agency3Line.services.impl;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.module.agency3Line.payload.request.FundsTransferRequestPayload;
import com.accionmfb.omnix.agency.module.agency3Line.payload.request.WithdrawalRequestPayload;
import com.accionmfb.omnix.agency.module.agency3Line.services.GenericResourceService;
import com.accionmfb.omnix.agency.module.agency3Line.services.TransactionLogService;
import com.accionmfb.omnix.agency.payload.AccountDetailsResponsePayload;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.FundsTransferResponsePayload;
import com.accionmfb.omnix.agency.payload.OfsResponse;
import com.accionmfb.omnix.agency.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Objects;

/**
 * @author Chikodi
 */
@Service
@Slf4j
public class GenericResourceServiceImpl implements GenericResourceService {
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private FundsTransferService fundsTransferService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TafjService tafjService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    Gson gson;
    TransactionLogService transactionLogService;

    @Override
    public String depositNotification(DepositRequestPayload depositRequestPayload) {
        try {
            String depositRequestJson = gson.toJson(depositRequestPayload);
            String bearerToken = "Bearer " + jwtToken.generateToken();
            String response = fundsTransferService.localTransferWithInternal(bearerToken, depositRequestJson);

            String validationMessage = validateResponse(response);
            if (validationMessage != null) {
                return validationMessage;
            }
            FundsTransferResponsePayload fundsTransferResponsePayload = convertJsonToObject(response, FundsTransferResponsePayload.class);

            if (Objects.equals(fundsTransferResponsePayload.getResponseCode(), ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                OfsResponse tafjResponse = tafjService.doTransaction(depositRequestPayload);

                transactionLogService.updateTransactionLog();

                return ResponseCodes.SUCCESS_CODE.getResponseCode();
            } else {
                return "Failed to process deposit notification: " + fundsTransferResponsePayload.getResponseMessage();
            }
        } catch (Exception ex) {
            log.error("Error occurred during deposit notification: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    @Override
    public String withdrawal(double withdrawalAmount, String accountNumber, String withdrawalReason, String withdrawalAuthorizationCode) {
        try {
            WithdrawalRequestPayload withdrawalRequestPayload = new WithdrawalRequestPayload();
            withdrawalRequestPayload.setAmount(String.valueOf(withdrawalAmount));
            withdrawalRequestPayload.setAccountNumber(accountNumber);

            String withdrawalRequestJson = gson.toJson(withdrawalRequestPayload);

            String bearerToken = "Bearer " + jwtToken.generateToken();

            String response = fundsTransferService.localTransferWithInternal(bearerToken, withdrawalRequestJson);

            String validationMessage = validateResponse(response);
            if (validationMessage != null) {
                return validationMessage;
            }

            FundsTransferResponsePayload fundsTransferResponsePayload = convertJsonToObject(response, FundsTransferResponsePayload.class);

            if (Objects.equals(fundsTransferResponsePayload.getResponseCode(), ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                OfsResponse tafjResponse = tafjService.sendOfsRequest(withdrawalRequestPayload);

                transactionLogService.updateTransactionLog();

                return ResponseCodes.SUCCESS_CODE.getResponseCode();
            } else {
                return "Failed to process withdrawal: " + fundsTransferResponsePayload.getResponseMessage();
            }
        } catch (Exception ex) {
            log.error("Error occurred during withdrawal: {}", ex.getMessage());

            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    private FundsTransferRequestPayload customizeWithdrawalRequest(String wsUser, double withdrawalAmount, String accountNumber, String withdrawalReason, String withdrawalAuthorizationCode) {
        FundsTransferRequestPayload request = new FundsTransferRequestPayload();
        request.setTransType("WITHDRAWAL");
        request.setAmount(String.valueOf(withdrawalAmount));
        request.setDebitAccount(accountNumber);
        request.setNarration(withdrawalReason);
        return request;
    }


    @Override
    public String verifyTransaction(String transactionID, String transactionType, String transactionDetails) {
        try {
            boolean verificationSuccessful = true;

            transactionLogService.updateTransactionLog(transactionID, transactionType, transactionDetails);

            String ofsResponse = String.valueOf(tafjService.sendOfsRequest());
            String tafjResponse = String.valueOf(tafjService.sendOfsRequest());

            if (ofsResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode()) &&
                    tafjResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                return ResponseCodes.SUCCESS_CODE.getResponseCode();
            } else {
                log.error("Error in processing transaction to T24 system: ofs={}, tafj={}", ofsResponse, tafjResponse);
                return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
            }
        } catch (Exception ex) {
            log.error("Error occurred during transaction verification: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    @Override
    public String openAccount(Map<String, String> customerInfo, double initialDepositAmount, String accountType, String openingBranch) {
        try {
            if (customerInfo.containsKey("firstName") && customerInfo.containsKey("lastName") &&
                    customerInfo.containsKey("email") && customerInfo.containsKey("phoneNumber")) {

                String ofsResponse = agencyService.openAccount(customerInfo, initialDepositAmount, accountType, openingBranch);
                String tafjResponse = String.valueOf(tafjService.sendOfsRequest());
                if (ofsResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode()) &&
                        tafjResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    transactionLogService.updateTransactionLog("OpenAccount", "AccountOpening", customerInfo.toString());
                    return ResponseCodes.SUCCESS_CODE.getResponseCode();
                } else {
                    log.error("Error in opening account: ofs={}, tafj={}", ofsResponse, tafjResponse);
                    return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
                }
            } else {
                return ResponseCodes.INVALID_TYPE.getResponseCode();
            }
        } catch (Exception ex) {
            log.error("Error occurred while opening account: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }


    @Override
    public String getCustomerDetailsByMobileNo(String mobileNumber, String customerPIN, String verificationMode) {
        try {
            if (validateCustomerPIN(customerPIN) && validateVerificationMode(verificationMode)) {
                String tafjResponse = customerService.getCustomerDetailsByMobileNo(mobileNumber);

                String ofsResponse = "";
                if (ofsResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode()) &&
                        tafjResponse.equals(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                    transactionLogService.updateTransactionLog("GetCustomerDetails", "CustomerDetails", mobileNumber);
                    return ResponseCodes.SUCCESS_CODE.getResponseCode();
                } else {
                    log.error("Error in retrieving customer details: ofs={}, tafj={}", ofsResponse, tafjResponse);
                    return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
                }
            } else {
                return ResponseCodes.INVALID_TYPE.getResponseCode();
            }
        } catch (Exception ex) {
            log.error("Error occurred while getting customer details: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    private boolean validateCustomerPIN(String customerPIN) {
        return (customerPIN != null) && !customerPIN.isEmpty() && customerPIN.matches("[0-9]+") && (customerPIN.length() == Integer.parseInt(customerPIN));
    }

    private boolean validateVerificationMode(String verificationMode) {
        return verificationMode != null && !verificationMode.isEmpty() && verificationMode.matches("[A-Za-z0-9]+");
    }

    @Override
    public String getAccountDetails(String accountNumber, Map<String, String> accessCredentials) {
        try {
            if (accountNumber == null || accountNumber.isEmpty() || accessCredentials == null || accessCredentials.isEmpty()) {
                return ResponseCodes.BAD_REQUEST.getResponseCode();
            }
            boolean isAuthenticated = authenticateUser(accessCredentials);
            if (!isAuthenticated) {
                return ResponseCodes.OUT_OF_RANGE.getResponseCode();
            }
            AccountDetailsResponsePayload accountDetails = fetchAccountDetailsFromT24(accountNumber);

            if (accountDetails == null) {
                return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
            }
            return ResponseCodes.SUCCESS_CODE.getResponseCode();
        } catch (Exception ex) {
            log.error("Error occurred while fetching account details: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    private boolean authenticateUser(Map<String, String> accessCredentials) {
        return true;
    }

    private AccountDetailsResponsePayload fetchAccountDetailsFromT24(String Nuban) {

        return null;
    }

    @Override
    public String getAccountBalance(String accountNumber, Map<String, String> accessCredentials) {
        try {
            if (accountNumber == null || accountNumber.isEmpty() || accessCredentials == null || accessCredentials.isEmpty()) {
                return ResponseCodes.BAD_REQUEST.getResponseCode();
            }
            boolean isAuthenticated = authenticateUser(accessCredentials);
            if (!isAuthenticated) {
                return ResponseCodes.BAD_REQUEST.getResponseCode();
            }
            double accountBalance = fetchAccountBalanceFromT24(accountNumber);
            if (accountBalance < 0) {
                return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
            }
            return ResponseCodes.SUCCESS_CODE.getResponseCode();
        } catch (Exception ex) {
            log.error("Error occurred while fetching account balance: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    private double fetchAccountBalanceFromT24(String Nuban) {
        return 1000.00;
    }


    @Override
    public String posCallback(String transactionResponse, String terminalID, String merchantID, String transactionType) {
        try {
            if (transactionResponse == null || transactionResponse.isEmpty() || terminalID == null || terminalID.isEmpty()
                    || merchantID == null || merchantID.isEmpty() || transactionType == null || transactionType.isEmpty()) {
                return ResponseCodes.BAD_REQUEST.getResponseCode();
            }
            boolean transactionProcessedSuccessfully = processTransactionToT24(transactionResponse, terminalID, merchantID, transactionType);
            if (!transactionProcessedSuccessfully) {
                return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
            }
            return ResponseCodes.SUCCESS_CODE.getResponseCode();
        } catch (Exception ex) {
            log.error("Error occurred while processing POS callback: {}", ex.getMessage());
            return ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode();
        }
    }

    private boolean processTransactionToT24(String transactionResponse, String terminalID, String merchantID, String transactionType) {
        return true;
    }


    private String validateResponse(String response) {
        if (response == null || response.isEmpty()) {
            return ResponseCodes.INVALID_TYPE.getResponseCode();
        }
        return null;
    }

    private <T> T convertJsonToObject(String response, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response, clazz);
        } catch (Exception ex) {
            log.error("Error occurred while converting JSON to object: {}", ex.getMessage());
            return null;
        }
    }

    private String convertObjectToJson(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            log.error("Error occurred while converting object to JSON: {}", ex.getMessage());
            return null;
        }
    }
}