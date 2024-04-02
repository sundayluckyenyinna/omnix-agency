package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.ErrandPayBalanceRequest;
import com.accionmfb.omnix.agency.payload.ErrandPayCashoutNotification;
import com.accionmfb.omnix.agency.payload.GruppResponsePayload;

public interface ErrandPayService {

    GruppResponsePayload validateErrandPayCashout(ErrandPayCashoutNotification requestPayload);

    boolean validateErrandPayCashoutNotificationPayload(String token, ErrandPayCashoutNotification requestPayload);

    String processErrandPayCashoutNotification(String token, ErrandPayCashoutNotification requestPayload);
    boolean validateAccountBalancePayload(String token, ErrandPayBalanceRequest requestPayload);

    String processAccountBalance(String token, ErrandPayBalanceRequest requestPayload);



}
