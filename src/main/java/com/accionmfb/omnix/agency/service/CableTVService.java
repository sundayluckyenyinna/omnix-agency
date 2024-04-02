/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import static com.accionmfb.omnix.agency.constant.ApiPaths.CABLETV_BILLERS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.CABLETV_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.CABLETV_SMARTCARD_DETAILS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.CABLE_SUBSCRIPTION;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-cabletv", url = "${zuul.routes.cableTVService.url}")
public interface CableTVService {

    @PostMapping(value = CABLE_SUBSCRIPTION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String cableSubscription(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CABLETV_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String cableTVDetails(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CABLETV_SMARTCARD_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String cableTVSmartcardLookup(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CABLETV_BILLERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String cableTVBillers(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}
