/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import static com.accionmfb.omnix.agency.constant.ApiPaths.AIRTIME_SELF;
import static com.accionmfb.omnix.agency.constant.ApiPaths.AIRTIME_OTHERS;
import static com.accionmfb.omnix.agency.constant.ApiPaths.DATA_SELF;
import static com.accionmfb.omnix.agency.constant.ApiPaths.DATA_OTHERS;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-airtime", url = "${zuul.routes.airtimeService.url}")
public interface AirtimeService {

    @PostMapping(value = AIRTIME_SELF, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String airtimeSelf(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = AIRTIME_OTHERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String airtimeOthers(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = DATA_SELF, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String dataSelf(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = DATA_OTHERS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String dataOthers(@RequestHeader("Authorization") String bearerToken, String requestPayload);

}
