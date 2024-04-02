package com.accionmfb.omnix.agency.controller;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.ErrandPayBalanceRequest;
import com.accionmfb.omnix.agency.payload.ErrandPayCashoutNotification;
import com.accionmfb.omnix.agency.payload.GruppResponsePayload;
import com.accionmfb.omnix.agency.service.AgencyService;
import com.accionmfb.omnix.agency.service.ErrandPayService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;
import java.util.Objects;

import static com.accionmfb.omnix.agency.constant.ApiPaths.ERRAND_PAY_AGENT_BALANCE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ERRAND_PAY_CASHOUT_NOTIFICATION;

@RestController
@Tag(name = "Errand pay", description = "Agency Microservice REST API")
@RefreshScope
@Slf4j
public class ErrandPayController {

    @Autowired
    private ErrandPayService errandPayService;

    @Autowired
    JwtTokenUtil jwtToken;

    @Autowired
    MessageSource messageSource;

    @Autowired
    AgencyService agencyService;

    @Value("${omnix.nip.environment}")
    private String ftEnvironment;

    private final Gson gson = new Gson();

    @PostMapping(value = ERRAND_PAY_CASHOUT_NOTIFICATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Errand pay Agency Banking")
    public ResponseEntity<Object> errandPayCashoutNotification(@Valid @RequestBody ErrandPayCashoutNotification requestPayload, HttpServletRequest httpRequest) {
        log.info("<<<<<<<------------------------- Errand pay cash out notification called ------------------------------>>>>>>>>");
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJncnVwcCIsInJvbGVzIjoiW0dSVVBQLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBFTEVDVFJJQ0lUWV9CSUxMX1BBWU1FTlQsIFNNU19OT1RJRklDQVRJT04sIElOVEVSX0JBTktfRlVORFNfVFJBTlNGRVIsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9CQUxBTkNFUywgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIEFDQ09VTlRfQkFMQU5DRSwgTklQX05BTUVfRU5RVUlSWV0iLCJhdXRoIjoibWsvdnQ2OVBXMUVVaEpTVUhnZE0rQT09IiwiQ2hhbm5lbCI6IkFHRU5DWSIsIklQIjoiMDowOjA6MDowOjA6MDoxIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjU5MzQ2NDMyLCJleHAiOjYyNTE0Mjg0NDAwfQ.Q6aeZeZtT6IeDNjFa5Sc7gAt0vKLqFjERPy02zS7aTg";
        //Defaulting the token for Grupp Agency Integration
        GruppResponsePayload validation = errandPayService.validateErrandPayCashout(requestPayload);
        log.info("Errand pay validation payload ---->>> {}", validation);
        if (Objects.equals(validation.getStatus(), "FAILED")) {
            log.info("request failed");
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(validation.getMessage());

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "GRUPP");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
            // TODO: 8/25/2023 Change back to ft environment
//            if ((true)) {
                log.info("is production");
                boolean payloadValid = errandPayService.validateErrandPayCashoutNotificationPayload(token, requestPayload);
                if (payloadValid) {
                    //Valid request
                    log.info("valid request");
                    String response = errandPayService.processErrandPayCashoutNotification(token, requestPayload);
                    String requestJson = gson.toJson(response);
                    log.info("Response from processing Errand pay Cashout Notification ----->>> {}", requestJson);
                    return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
                } else {
                    log.info("not valid request");
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
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
//    }

    @PostMapping(value = ERRAND_PAY_AGENT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get agent account balance")
    public ResponseEntity<Object> errandPayAccountBalance(@Valid @RequestBody ErrandPayBalanceRequest requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        log.info("---------------Errandpay account balance endpoint called-------------------- ");
        log.info("payload for Errand pay Agent Balance Payload ------>>> {}", requestPayload.toString());

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
        //        if ("Production".equalsIgnoreCase(ftEnvironment.trim())) {
        // TODO: 8/25/2023 Change back to ft environment
        if ((true)) {
            log.info("------in production----");
            boolean payloadValid = errandPayService.validateAccountBalancePayload(token, requestPayload);
            if (payloadValid) {
                //Valid request
                String response = errandPayService.processAccountBalance(token, requestPayload);
                log.info("Response from processing Account Balance ----->>>> {}", response);
                return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
            } else {
                exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

                String exceptionJson = gson.toJson(exResponse);
                return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
            }
        } else {
            log.info("we in test");
            /* This is for test purpose */
            String response = agencyService.accountBalanceTest(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

}