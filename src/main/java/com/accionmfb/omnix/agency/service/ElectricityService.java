/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import static com.accionmfb.omnix.agency.constant.ApiPaths.ELECTRICITY_BILLERS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ELECTRICITY_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ELECTRICITY_PAYMENT;
import static com.accionmfb.omnix.agency.constant.ApiPaths.ELECTRICITY_SMARTCARD_DETAILS;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-electricity", url = "${zuul.routes.electricityService.url}")
public interface ElectricityService {

    @PostMapping(value = ELECTRICITY_PAYMENT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String electricityPayment(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = ELECTRICITY_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String electricityDetails(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = ELECTRICITY_SMARTCARD_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String electricitySmartcardLookup(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = ELECTRICITY_BILLERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String electricityBillers(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}
