package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.module.agency3Line.payload.request.WithdrawalRequestPayload;
import com.accionmfb.omnix.agency.payload.DepositRequestPayload;
import com.accionmfb.omnix.agency.payload.OfsResponse;

/**
 *
 * @author dofoleta
 */
public interface TafjService {

     OfsResponse sendOfsRequest(WithdrawalRequestPayload request);

     OfsResponse doTransaction(DepositRequestPayload depositRequestPayload);

     boolean sendOfsRequest();
}
