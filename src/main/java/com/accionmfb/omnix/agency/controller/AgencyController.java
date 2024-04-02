/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.controller;

import static com.accionmfb.omnix.agency.constant.ApiPaths.ACCION_AGENT_BOARD;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ACCION_AGENT_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TOKEN_PREFIX;
import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.AccionAgentBoardPayload;
import com.accionmfb.omnix.agency.payload.GruppResponsePayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;
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
@Tag(name = "agency", description = "Agency Microservice REST API")
@RefreshScope
@Slf4j
public class AgencyController {

    @Autowired
    AgencyService agencyService;
    @Autowired
    GruppService gruppService;
    @Autowired
    MessageSource messageSource;
    private Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    Logger logger = LoggerFactory.getLogger(GruppController.class);

    AgencyController() {
        gson = new Gson();
    }

    @PostMapping(value = ACCION_AGENT_BOARD, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Board Accion Agent")
    public ResponseEntity<Object> boardAccionAgent(@Valid @RequestBody AccionAgentBoardPayload requestPayload, HttpServletRequest httpRequest) {
        log.info("-------------- board accion agent endpoint called --------------------");
        String requestJson = gson.toJson(requestPayload);
        log.info("Accion AgentBoard Payload --------->>> {}", requestJson);

        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        GruppResponsePayload validation = gruppService.validateTerminalId(requestPayload.getAgentTerminalId());
        if (Objects.equals(validation.getStatus(), "FAILED")) {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(validation.getMessage());

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "AGENCY_BANKING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = agencyService.validateAccionAgentBoardPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = agencyService.processAccionAgentBoard(token, requestPayload);
            log.info("Response from processing Accion Agent board ----->>>> {}", response);
            return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = ACCION_AGENT_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accion Agent Details")
    public ResponseEntity<Object> accionAgentDetails(@Valid @RequestBody MobileNumberRequestPayload requestPayload, HttpServletRequest httpRequest) {
        log.info("-------------- get accion agent endpoint called --------------------");
        String requestJson = gson.toJson(requestPayload);
        log.info("accionAgentDetails ------>> {}", requestJson);
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();

        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "AGENCY_BANKING");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = agencyService.validateAccionAgentDetailsPayload(token, requestPayload);
        if (payloadValid) {
            String response = agencyService.processAccionAgentDetails(token, requestPayload);
            String responseJson = gson.toJson(requestPayload);
            log.info("Response from processing Accion Agent Details ----->>>> {}", responseJson);
            return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

}
