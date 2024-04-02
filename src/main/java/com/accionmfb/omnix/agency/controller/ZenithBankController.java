/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.controller;

import static com.accionmfb.omnix.agency.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TOKEN_PREFIX;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ZENITH_BANK_ACCOUNT_VALIDATION;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ZENITH_BANK_DEPOSIT;
import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.AccountValidation;
import com.accionmfb.omnix.agency.payload.TransactionPosting;
import com.accionmfb.omnix.agency.service.ZenithBankService;
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
@Tag(name = "Zenith", description = "Zenith Bank Services")
@RefreshScope
public class ZenithBankController {

    @Autowired
    ZenithBankService zenithService;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    MessageSource messageSource;
    private Gson gson;

    ZenithBankController() {
        gson = new Gson();
    }

    @PostMapping(value = ZENITH_BANK_ACCOUNT_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Zenith Bank Account Validation")
    public ResponseEntity<Object> accountValidation(@Valid @RequestBody AccountValidation requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ZENITH");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        String response = zenithService.getAccountDetails(token, requestPayload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = ZENITH_BANK_DEPOSIT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Zenith Bank Deposit Transaction")
    public ResponseEntity<Object> triftaFundsTransferLocal(@Valid @RequestBody TransactionPosting requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "ZENITH");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        String response = zenithService.processDepositTransaction(token, requestPayload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

//    @PostMapping(value = ZENITH_BANK_TRANSACTION_QUERY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Operation(summary = "Zenith Bank Transaction Query")
//    public ResponseEntity<Object> transactionQuery(@Valid @RequestBody TransactionPayload requestPayload, HttpServletRequest httpRequest) {
//        ExceptionResponse exResponse = new ExceptionResponse();
//        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
//        //Check if the user has role
//        boolean userHasRole = jwtToken.userHasRole(token, "ZENITH");
//        if (!userHasRole) {
//            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
//            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));
//
//            String exceptionJson = gson.toJson(exResponse);
//            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
//        }
//        String response = zenithService.processTransactionQuery(token, requestPayload);
//        return new ResponseEntity<>(response, HttpStatus.OK);
//    }
}
