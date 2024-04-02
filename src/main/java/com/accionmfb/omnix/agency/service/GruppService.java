/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.model.*;
import com.accionmfb.omnix.agency.payload.*;
import java.math.BigDecimal;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author bokon
 */
public interface GruppService {

    boolean validateGruppCashoutNotificationPayload(String token, GruppCashoutNotificationPayload requestPayload);

    String processGruppCashoutNotification(String token, GruppCashoutNotificationPayload requestPayload);

    boolean validateGruppAgentDetailsPayload(String token, GruppAgentDetailsPayload requestPayload);

    String processGruppAgentDetails(String token, GruppAgentDetailsPayload requestPayload);

    boolean validateGruppDisbursementPayload(String token, GruppDisbursementRequestPayload requestPayload);

    String processGruppCableTV(String token, GruppDisbursementRequestPayload requestPayload);

    String processGruppElectricityBill(String token, GruppDisbursementRequestPayload requestPayload);

    String processGruppFundsTransfer(String token, GruppDisbursementRequestPayload requestPayload);

    String processGruppAirtime(String token, GruppDisbursementRequestPayload requestPayload);

    boolean validateAccountBalancePayload(String token, GruppAgentBalancePayload requestPayload);

    String processAccountBalance(String token, GruppAgentBalancePayload requestPayload);

    FundsTransferResponsePayload posNotificationSplit(String accountNumber, String transRef, String requestBy, String valueOf, String accionFee, String narration, String channel, AppUser appUser, Branch branch, String customerName) throws Exception;

    String internalLocalTransfer2(LocalTransferWithInternalPayload requestPayload, AppUser appUser, Branch branch, String token, String customerName) throws Exception;

    GruppResponsePayload validateGruppCashout(GruppCashoutNotificationPayload requestPayload);

    GruppResponsePayload validateGruppDisbursementForTest(GruppDisbursementRequestPayload requestPayload);

    GruppResponsePayload validateTerminalId(String terminalId);

    String getCashoutReport(HttpServletRequest httpRequest, StatusReportRequest requestPayload);
    String checkAccountBalance(String accountNumber, String token);
    String internalLocalTransfer(LocalTransferWithInternalPayload requestPayload, AppUser appUser, Branch branch, String token, String customerName) throws Exception;

    GruppResponsePayload performAllTransfers(String token, AgentTranLog history, GruppCashoutNotificationPayload requestPayload, String requestBy, String channel, AccionAgent agent, Customer customer, AppUser appUser, Branch branch, BigDecimal netAmount, BigDecimal accionFee, BigDecimal agentAmount, BigDecimal vatAmount, BigDecimal netIncomeAmount, BigDecimal remBal) throws Exception;
//    public String processNIPTransfer(String token, NIPTransferPayload requestPayload);

}