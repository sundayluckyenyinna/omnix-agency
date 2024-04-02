package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.module.agency3Line.payload.request.WithdrawalRequestPayload;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.OfsRequest;
import com.accionmfb.omnix.agency.payload.OfsResponse;
import com.accionmfb.omnix.agency.service.TafjService;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Properties;

@Service
public class TafjServiceImpl implements TafjService {

    @Autowired
    Gson gson;

    @Autowired
    Environment env;

    private static final Logger log = LoggerFactory.getLogger(TafjServiceImpl.class.getName());

    @Override
    public OfsResponse sendOfsRequest(WithdrawalRequestPayload request) {

        if (request == null || request.getOfsRequest() == null || request.getOfsRequest().trim().isEmpty()) {
            return OfsResponse.builder()
                    .ofsRequest("")
                    .ofsResponse("request cannot be empty or null")
                    .build();
        }

        String tafOfsResponse = "";
        try {
            String username = env.getProperty("tafj.username");
            String password = env.getProperty("tafj.password");
            String credential = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            String url = this.env.getProperty("tafj.url");
            String tafOfsRequest = gson.toJson(request, OfsRequest.class);

            log.info("raw ofsResponse ------->>> {}", tafOfsRequest);

            Properties props = System.getProperties();
            props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

            HttpResponse<String> oHttpResponse = Unirest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", credential)
                    .header("id-token", "AUTH")
                    .body(tafOfsRequest)
                    .asString();

            tafOfsResponse = oHttpResponse.getBody();
            log.info("raw ofsResponse ------->>> {}", tafOfsResponse);
        } catch (Exception ex) {
            log.error("Exception occurred while trying to connect to resource. Exception message is: {}", ex.getMessage());
        }
        return gson.fromJson(tafOfsResponse, OfsResponse.class);
    }

    @Override
    public OfsResponse doTransaction(DepositRequestPayload depositRequestPayload) {
        return null;
    }

    @Override
    public boolean sendOfsRequest() {
        return false;
    }

}
