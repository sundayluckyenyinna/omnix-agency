package com.accionmfb.omnix.agency.service.utils;

import com.accionmfb.omnix.agency.dto.DecryptionRequest;
import com.accionmfb.omnix.agency.dto.EncryptionRequest;
import com.accionmfb.omnix.agency.dto.FundTransferResponse;
import com.accionmfb.omnix.agency.payload.LocalTransferWithInternalPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import kong.unirest.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.accionmfb.omnix.agency.constant.ApiPaths.*;

@Service
@Slf4j
public class Utils {

    private static final Gson JSON = new Gson();

    @Value("${accion.accessToken}")
    private String accessToken;
    public String postForObject(String url, String requestJson, Map<String, String> headers, Map<String, Object> params) {
        String responseJson = null;
        HttpResponse<String> response;
        Unirest.config().verifySsl(false);
        try{
            RequestBodyEntity postRequest = Unirest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestJson);
            if(headers != null)
                postRequest = postRequest.headers(headers);
            if(params != null)
                postRequest = postRequest.queryString(params);
            response = postRequest.asString();

            if(response != null){
                responseJson = response.getBody();
            }
        }catch (UnirestException ex){
            ex.printStackTrace();
        }
        return responseJson;
    }

    public String postForObject(String url, Object requestObject, Map<String, String> headers, Map<String, Object> params) {
        String requestJson = JSON.toJson(requestObject);
        return postForObject(url, requestJson, headers, params);
    }

    public FundTransferResponse decryptFundsTransferPayload(Object payload) throws JsonProcessingException {

        String result = postForObject(decryptPayloadUrl, payload, paramsForEndpoint(), null);
        System.out.println("Response after decryption------>>> "+result);
        FundTransferResponse response = new ObjectMapper().readValue(result, FundTransferResponse.class);
        log.info("response after mapping ----->>>> {}", response);
        return response;
    }

    public DecryptionRequest fundTransfer(EncryptionRequest request) throws JsonProcessingException {

        String result = postForObject(localTransferUrl, request, paramsForEndpoint(), null);
        log.info("response after calling fund transfer endpoint ---->> {}", result);
        DecryptionRequest response = new ObjectMapper().readValue(result, DecryptionRequest.class);
        log.info("Response after calling fund transfer endpoint------>>> "+result);
        return response;
    }

    public EncryptionRequest encryptPayload(LocalTransferWithInternalPayload payload) throws JsonProcessingException {
        log.info("payload before encryption    ----------->>>>>> {}", payload);
        String result = postForObject(encryptPayloadUrl, payload, paramsForEndpoint(), null);
        System.out.println("Response after encryption------>>> "+result);
        EncryptionRequest response = new ObjectMapper().readValue(result, EncryptionRequest.class);
        log.info("Response after mapping encryption------>>> "+result);
        return response;
    }

    public Map<String, String> paramsForEndpoint() {
        Map<String, String> request = new HashMap<>();
        request.put("Authorization", "Bearer "+accessToken);
        request.put("Content-Type", "application/json");
        return request;
    }

//    public static void main(String[] args) {
//        GruppServiceImpl gruppService = new GruppServiceImpl();
//        gruppService.testHash();
//    }

}
