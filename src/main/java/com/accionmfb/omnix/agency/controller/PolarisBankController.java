/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.controller;

import static com.accionmfb.omnix.agency.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.agency.constant.ApiPaths.POLARIS_BANK_ACCOUNT_VALIDATION;
import static com.accionmfb.omnix.agency.constant.ApiPaths.POLARIS_BANK_DEPOSIT;
import static com.accionmfb.omnix.agency.constant.ApiPaths.POLARIS_BANK_TRANSACTION_QUERY;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TOKEN_PREFIX;
import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;
import com.accionmfb.omnix.agency.service.PolarisBankService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Polaris", description = "Polaris Bank Services")
@RefreshScope
public class PolarisBankController {

    @Autowired
    PolarisBankService polarisService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    MessageSource messageSource;
    private Gson gson;

    PolarisBankController() {
        gson = new Gson();
    }

    @PostMapping(value = POLARIS_BANK_ACCOUNT_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Polaris Bank Account Validation")
    public ResponseEntity<Object> accountValidation(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POLARIS");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = polarisService.validateAccountNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = polarisService.getAccountDetails(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = POLARIS_BANK_DEPOSIT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Polaris Bank Transaction")
    public ResponseEntity<Object> triftaFundsTransferLocal(@Valid @RequestBody DepositRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POLARIS");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = polarisService.validateDepositTransactionPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = polarisService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = polarisService.processDepositTransaction(token, requestPayload);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            exResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            exResponse.setResponseMessage((String) recordExist);

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = POLARIS_BANK_TRANSACTION_QUERY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Polaris Bank Transaction Query")
    public ResponseEntity<Object> transactionQuery(@Valid @RequestBody TransactionPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "POLARIS");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = polarisService.validateTransactionPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = polarisService.processTransactionQuery(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }
}
