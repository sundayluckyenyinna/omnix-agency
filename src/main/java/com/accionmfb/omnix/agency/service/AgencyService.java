/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.AccionAgentBoardPayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;

import java.util.Map;

/**
 *
 * @author bokon
 */
public interface AgencyService {

    String localFundsTransferTest(String token, Object requestPayload);

    String accountBalanceTest(String token, Object requestPayload);

    boolean validateAccionAgentBoardPayload(String token, AccionAgentBoardPayload requestPayload);

    String processAccionAgentBoard(String token, AccionAgentBoardPayload requestPayload);

    boolean validateAccionAgentDetailsPayload(String token, MobileNumberRequestPayload requestPayload);

    String processAccionAgentDetails(String token, MobileNumberRequestPayload requestPayload);

    String openAccount(Map<String, String> customerInfo, double initialDepositAmount, String accountType, String openingBranch);
}
