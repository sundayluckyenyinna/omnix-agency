package com.accionmfb.omnix.agency.ivr.service;

/**
 * @author Olaoye on 30/10/2023
 */
public interface IVRService {

    Boolean checkRequestHeaderValidity(String authorization);

    Boolean typeValidation(String requestJson);

    String ivrRequest(String authorization, String requestJson, String requestSource, String remoteIP, String sessionId);
}
