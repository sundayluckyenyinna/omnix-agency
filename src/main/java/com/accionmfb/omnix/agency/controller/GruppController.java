/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.controller;

import static com.accionmfb.omnix.agency.constant.ApiPaths.*;
import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.*;
import com.accionmfb.omnix.agency.service.AgencyService;
import com.accionmfb.omnix.agency.service.GruppService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "agency", description = "Agency Microservice REST API")
@RefreshScope
@Slf4j
public class GruppController {

    @Autowired
    AgencyService agencyService;
    @Autowired
    GruppService gruppService;
    @Autowired
    MessageSource messageSource;
    private Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.nip.environment}")
    private String ftEnvironment;
    Logger logger = LoggerFactory.getLogger(GruppController.class);

    GruppController() {
        gson = new Gson();
    }

    @PostMapping(value = GRUPP_CASHOUT_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Grupp Agency Banking")
    public ResponseEntity<Object> gruppCashoutNotification(@Valid @RequestBody GruppCashoutNotificationPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        log.info("request payload ---->>> {}", requestPayload);
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";
        //Defaulting the token for Grupp Agency Integration

        GruppResponsePayload validation = gruppService.validateGruppCashout(requestPayload);
        log.info("validation payload ---->>> {}", validation);
        if (Objects.equals(validation.getStatus(), "FAILED")) {
            log.info("request failed");
            log.info("____________________validation is NOT successfullllll____________________");

            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(validation.getMessage());

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        log.info("____________________validation is successfullllll____________________");

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "GRUPP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
            log.info("is production");
            boolean payloadValid = gruppService.validateGruppCashoutNotificationPayload(token, requestPayload);
            if (payloadValid) {
//            if (true) {
                //Valid request
                log.info("valid request");
                String response = gruppService.processGruppCashoutNotification(token, requestPayload);
                String requestJson = gson.toJson(response);
                log.info("Response from processing Grupp Cashout Notification ----->>> {}", requestJson);
                return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
            } else {
                log.info("not valid request");
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
//                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));
                exResponse.setResponseMessage("Invalid request");

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            log.info("testing purpose");
            String response = agencyService.localFundsTransferTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = GRUPP_AGENT_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Grupp Agent Details")
    public ResponseEntity<Object> gruppAgentDetails(@Valid @RequestBody GruppAgentDetailsPayload requestPayload, HttpServletRequest httpRequest) {
        log.info("------------------- grupp Agent details called -------------------");
        String requestJson1 = gson.toJson(requestPayload);
        log.info("request --------->>> {}", requestJson1);
        log.info("GruppAgentDetailsPayload ------>>> {}", requestPayload.toString());
        //Defaulting the token for Grupp Agency Integration
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";
        ExceptionResponse exResponse = new ExceptionResponse();

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "GRUPP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
            boolean payloadValid = gruppService.validateGruppAgentDetailsPayload(token, requestPayload);
            if (payloadValid) {
//            if (true) {
                String response = gruppService.processGruppAgentDetails(token, requestPayload);
                String responseJson = gson.toJson(response);
                log.info("Response from processing Grupp Agent Details ----->>> {}", responseJson);
                return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = agencyService.localFundsTransferTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = GRUPP_DISBURSEMENT_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Grupp Cable TV, Transfer, PHCN and Airtime")
    public ResponseEntity<Object> gruppDisbursementNotification(@Valid @RequestBody GruppDisbursementRequestPayload requestPayload, HttpServletRequest httpRequest) {
        log.info("------------------- grupp Disbursement Notification called");
        String requestJson1 = gson.toJson(requestPayload);
        log.info("request --------->>> {}", requestJson1);
        log.info("payload for gruppDisbursementNotification ------>>> {}", requestPayload);
        ExceptionResponse exResponse = new ExceptionResponse();

        //Defaulting the token for Grupp Agency Integration
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "GRUPP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
            boolean payloadValid = gruppService.validateGruppDisbursementPayload(token, requestPayload);
            if (payloadValid) {
//            if (true) {
                //Valid request. Check tye type of notification
                if ("TRANSFER".equalsIgnoreCase(requestPayload.getTransactionType())) {
                    String response = gruppService.processGruppFundsTransfer(token, requestPayload);
                    String responseJson = gson.toJson(response);
                    log.info("response from transfer --------->>> {}", responseJson);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }

                if ("AIRTIME".equalsIgnoreCase(requestPayload.getTransactionType())) {
                    String response = gruppService.processGruppAirtime(token, requestPayload);
                    String responseJson = gson.toJson(response);
                    log.info("response from airtime --------->>> {}", responseJson);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }

                if ("PHCN".equalsIgnoreCase(requestPayload.getTransactionType())) {
                    String response = gruppService.processGruppElectricityBill(token, requestPayload);
                    String responseJson = gson.toJson(response);
                    log.info("response from PHCN --------->>> {}", responseJson);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }

                String response = gruppService.processGruppCableTV(token, requestPayload);
                String responseJson = gson.toJson(response);
                log.info("response from Cable tv --------->>> {}", responseJson);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = agencyService.localFundsTransferTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = GRUPP_AGENT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get agent account balance")
    public ResponseEntity<Object> accountBalance(@Valid @RequestBody GruppAgentBalancePayload requestPayload, HttpServletRequest httpRequest) {
        log.info("---------------account balance endpoint called-------------------- ");
        log.info("payload for Grupp Agent Balance Payload ------>>> {}", requestPayload);

        //Defaulting the token for Grupp Agency Integration
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "GRUPP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
            boolean payloadValid = gruppService.validateAccountBalancePayload(token, requestPayload);
            if (payloadValid) {
                //Valid request
                String response = gruppService.processAccountBalance(token, requestPayload);
                log.info("Response from processing Account Balance ----->>>> {}", response);
//                return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            /* This is for test purpose */
            String response = agencyService.accountBalanceTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping(value = GRUPP_CASHOUT_STATUS_REPORT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getTransactionStatus(@RequestBody StatusReportRequest requestPayload, HttpServletRequest httpRequest) {
        String response = gruppService.getCashoutReport(httpRequest, requestPayload);
        log.info("Response from processing Account Balance ----->>>> {}", response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = STATISTICS_MEMORY, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch JVM statistics")
    public MemoryStats getMemoryStatistics() {
        MemoryStats stats = new MemoryStats();
        stats.setHeapSize(Runtime.getRuntime().totalMemory());
        stats.setHeapMaxSize(Runtime.getRuntime().maxMemory());
        stats.setHeapFreeSize(Runtime.getRuntime().freeMemory());
        return stats;
    }
}
