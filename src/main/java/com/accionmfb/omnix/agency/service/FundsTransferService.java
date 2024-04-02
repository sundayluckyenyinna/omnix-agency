/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER;
import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER_INTERNAL_DEBIT_WITH_CHARGE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER_REVERSE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER_STATUS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER_WITH_CHARGE;
import static com.accionmfb.omnix.agency.constant.ApiPaths.LOCAL_TRANSFER_WITH_PL_INTERNAL;
import static com.accionmfb.omnix.agency.constant.ApiPaths.NIP_NAME_ENQUIRY;
import static com.accionmfb.omnix.agency.constant.ApiPaths.NIP_TRANSFER;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-fundstransfer", url = "${zuul.routes.fundstransferService.url}")
public interface FundsTransferService {

    @PostMapping(value = LOCAL_TRANSFER_WITH_PL_INTERNAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferWithInternal(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransfer(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_WITH_CHARGE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferWithCharges(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_WITH_PL_INTERNAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferWithInternalDebitCharges(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = NIP_TRANSFER, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String nipTransfer(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = NIP_NAME_ENQUIRY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String nipNameEnquiry(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_STATUS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String transactionQuery(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_REVERSE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String transactionReversal(@RequestHeader("Authorization") String bearerToken, String requestPayload);

}
