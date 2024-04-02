/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.accionmfb.omnix.agency.constant.ApiPaths.*;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-customer", url = "${zuul.routes.customerService.url}")
public interface CustomerService {

    @PostMapping(value = CUSTOMER_DETAILS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String customerDetails(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CREATE_INDIVIDUAL_CUSTOMER_WITH_BVN, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String createCustomerWithBvn(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CREATE_INDIVIDUAL_CUSTOMER_WITH_NOBVN, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String createCustomerWithoutBvn(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = CUSTOMER_DETAILS_BY_MOBILE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String getCustomerDetailsByMobileNo(@RequestHeader("Authorization") String bearerToken);
}
