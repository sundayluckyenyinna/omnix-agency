/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.controller;

import static com.accionmfb.omnix.agency.constant.ApiPaths.HEADER_STRING;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TOKEN_PREFIX;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_ACCOUNT_BALANCE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_ACCOUNT_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_ACCOUNT_OPENING;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_ACCOUNT_STATEMENT;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_AIRTIME_CALLS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_AIRTIME_DATA;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BIILS_ELECTRICITY_BILLERS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_CABLE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_CABLE_BILLERS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_CABLE_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_CABLE_LOOKUP;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_ELECTRICITY;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_ELECTRICITY_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_BILLS_ELECTRICITY_LOOKUP;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_CUSTOMER_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_CUSTOMER_WITHOUT_BVN;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_CUSTOMER_WITH_BVN;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_FUNDS_TRANSFER_LOCAL;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_FUNDS_TRANSFER_NIP;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_LOCAL_TRANSFER_REVERSE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_LOCAL_TRANSFER_STATUS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.TRIFTA_NIP_NAME_ENQUIRY;
import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.exception.ExceptionResponse;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.agency.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.agency.payload.AirtimeOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.BillerRequestPayload;
import com.accionmfb.omnix.agency.payload.CableTVPayload;
import com.accionmfb.omnix.agency.payload.CableTVRequestPayload;
import com.accionmfb.omnix.agency.payload.DataOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityBillerRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityPayload;
import com.accionmfb.omnix.agency.payload.ElectricityRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithoutBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.LocalTransferPayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.agency.payload.NIPNameEnquiryPayload;
import com.accionmfb.omnix.agency.payload.NIPTransferPayload;
import com.accionmfb.omnix.agency.payload.SmartcardRequestPayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;
import com.accionmfb.omnix.agency.service.AgencyService;
import com.accionmfb.omnix.agency.service.TriftaService;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 *
 * @author bokon
 */
@RestController
@Tag(name = "Trifta", description = "Agency Microservice REST API")
@RefreshScope
public class TriftaController {

    @Autowired
    AgencyService agencyService;
    @Autowired
    TriftaService triftaService;
    @Autowired
    MessageSource messageSource;
    private Gson gson;
    @Autowired
    JwtTokenUtil jwtToken;
    @Value("${omnix.nip.environment}")
    private String ftEnvironment;
    Logger logger = LoggerFactory.getLogger(TriftaController.class);

    TriftaController() {
        gson = new Gson();
    }

    @PostMapping(value = TRIFTA_ACCOUNT_OPENING, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Account Opening")
    public ResponseEntity<Object> triftaAccountOpening(@Valid @RequestBody AccountOpeningRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateAccountOpeningRequestPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processAccountOpening(token, requestPayload);
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

    @PostMapping(value = TRIFTA_ACCOUNT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Account Balance")
    public ResponseEntity<Object> triftaAccountBalance(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateAccountNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = triftaService.processAccountBalance(token, requestPayload);
            return new ResponseEntity<>(response, response.contains("FAILED") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = TRIFTA_ACCOUNT_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Account Details")
    public ResponseEntity<Object> triftaAccountDetails(@Valid @RequestBody AccountNumberPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateAccountNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = triftaService.getAccountDetails(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = TRIFTA_ACCOUNT_STATEMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get account statement")
    public ResponseEntity<Object> accountStatement(@Valid @RequestBody AccountStatementRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        //Check if the request is valid
        boolean payloadValid = triftaService.validateAccountStatementPayload(token, requestPayload);
        if (payloadValid) {
            //Valid request
            String response = triftaService.getAccountStatement(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = TRIFTA_CUSTOMER_WITH_BVN, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Create Customer With BVN")
    public ResponseEntity<Object> triftaCustomerWithBVN(@Valid @RequestBody IndivCustomerWithBvnRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateCreateIndividualCustomerWithBvnPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processCreateIndividaulCustomerWithBvn(token, requestPayload);
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

    @PostMapping(value = TRIFTA_CUSTOMER_WITHOUT_BVN, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Create Customer Without BVN")
    public ResponseEntity<Object> triftaCustomerWithoutBVN(@Valid @RequestBody IndivCustomerWithoutBvnRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateCreateIndividualCustomerWithoutBvnPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processCreateIndividualCustomerWithoutBvn(token, requestPayload);
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

    @PostMapping(value = TRIFTA_CUSTOMER_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Customer Details")
    public ResponseEntity<Object> triftaCustomerDetails(@Valid @RequestBody MobileNumberRequestPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateMobileNumberPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processCustomerDetails(token, requestPayload);
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

    @PostMapping(value = TRIFTA_FUNDS_TRANSFER_LOCAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Funds Transfer Local")
    public ResponseEntity<Object> triftaFundsTransferLocal(@Valid @RequestBody LocalTransferPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateLocalFundsTransferPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processLocalFundsTransfer(token, requestPayload);
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

    @PostMapping(value = TRIFTA_LOCAL_TRANSFER_REVERSE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reverse Transaction")
    public ResponseEntity<Object> triftaReverseLocalFundsTransfer(@Valid @RequestBody TransactionPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateTransactionPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processLocalFundsTransferReversal(token, requestPayload);
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

    @PostMapping(value = TRIFTA_LOCAL_TRANSFER_STATUS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Transaction Query")
    public ResponseEntity<Object> transactionQuery(@Valid @RequestBody TransactionPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateTransactionPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processTransactionQuery(token, requestPayload);
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

    @PostMapping(value = TRIFTA_NIP_NAME_ENQUIRY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "NIP Name Enquiry")
    public ResponseEntity<Object> nipNameEnquiry(@Valid @RequestBody NIPNameEnquiryPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateNIPNameEnquiryPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processNIPNameEnquiry(token, requestPayload);
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

    @PostMapping(value = TRIFTA_FUNDS_TRANSFER_NIP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Funds Transfer NIP")
    public ResponseEntity<Object> triftaFundsTransferNIP(@Valid @RequestBody NIPTransferPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateNIPTransferPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processNIPTransfer(token, requestPayload);
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

    @PostMapping(value = TRIFTA_AIRTIME_CALLS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Airtime Calls")
    public ResponseEntity<Object> triftaAirtimeCalls(@Valid @RequestBody AirtimeOtherRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = triftaService.validateAirtimeOthersPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processAirtime(httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, ""), requestPayload);
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

    @PostMapping(value = TRIFTA_AIRTIME_DATA, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Airtime Data")
    public ResponseEntity<Object> triftaAirtimeData(@Valid @RequestBody DataOtherRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateDataOthersPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processData(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BILLS_CABLE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Bills Cable")
    public ResponseEntity<Object> triftaBillsCable(@Valid @RequestBody CableTVRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateCableTVPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processCableTVSubscription(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BILLS_CABLE_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cable TV Details")
    public ResponseEntity<Object> cableTVDetails(@Valid @RequestBody CableTVPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateCableTVPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.getCableTVDetails(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BILLS_CABLE_BILLERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch Billers")
    public ResponseEntity<Object> cableTVBillers(@Valid @RequestBody BillerRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = triftaService.validateBillerPayload(token, requestPayload);
        if (payloadValid) {
            String response = triftaService.getCableTVBiller(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = TRIFTA_BILLS_CABLE_LOOKUP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cable TV Smartcard Details")
    public ResponseEntity<Object> getSmartcardDetails(@Valid @RequestBody SmartcardRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = triftaService.validateSmartcardPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.getCableTVSmartcardDetails(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BILLS_ELECTRICITY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trifta Bills Electricity")
    public ResponseEntity<Object> triftaBillsElectricity(@Valid @RequestBody ElectricityRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = triftaService.validateElectricityPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.processElectricityPayment(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BILLS_ELECTRICITY_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Electricity Details")
    public ResponseEntity<Object> electricityDetails(@Valid @RequestBody ElectricityPayload requestPayload, HttpServletRequest httpRequest) {
        ExceptionResponse exResponse = new ExceptionResponse();
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateElectricityPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.getElectricityDetails(token, requestPayload);
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

    @PostMapping(value = TRIFTA_BIILS_ELECTRICITY_BILLERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fetch Billers")
    public ResponseEntity<Object> electricityBiller(@Valid @RequestBody ElectricityBillerRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
        boolean payloadValid = triftaService.validateBillerPayload(token, requestPayload);
        if (payloadValid) {
            String response = triftaService.getElectricityBiller(token, requestPayload);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.hash.failed", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }
    }

    @PostMapping(value = TRIFTA_BILLS_ELECTRICITY_LOOKUP, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Electricity Smartcard Details")
    public ResponseEntity<Object> getElectricitySmartCardDetails(@Valid @RequestBody SmartcardRequestPayload requestPayload, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, "");
        ExceptionResponse exResponse = new ExceptionResponse();
        //Check if the user has role
        boolean userHasRole = jwtToken.userHasRole(token, "TRIFTA");
        if (!userHasRole) {
            exResponse.setResponseCode(ResponseCodes.NO_ROLE.getResponseCode());
            exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.norole", new Object[0], Locale.ENGLISH));

            String exceptionJson = gson.toJson(exResponse);
            return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
        }

        boolean payloadValid = triftaService.validateSmartcardPayload(token, requestPayload);
        if (payloadValid) {
            //Check if the request contains the same Request ID
            Object recordExist = triftaService.checkIfSameRequestId(requestPayload.getRequestId());
            if (recordExist instanceof Boolean) {
                if (!(boolean) recordExist) {
                    exResponse.setResponseCode(ResponseCodes.BAD_REQUEST.getResponseCode());
                    exResponse.setResponseMessage(messageSource.getMessage("appMessages.request.sameid", new Object[]{requestPayload.getRequestId()}, Locale.ENGLISH));

                    String exceptionJson = gson.toJson(exResponse);
                    return new ResponseEntity<>(exceptionJson, HttpStatus.OK);
                }
                //Valid request
                String response = triftaService.getElectricitySmartcardDetails(token, requestPayload);
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

}
