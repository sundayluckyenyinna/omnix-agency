package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class FTSingleCreditRequest {
    private String nameEnquiryRef;
    private String destinationInstitutionCode;
    private int channelCode;
    private String beneficiaryAccountName;
    private String beneficiaryAccountNumber;
    private String beneficiaryBankVerificationNumber;
    private String beneficiaryKYCLevel;
    private String originatorAccountName;
    private String originatorAccountNumber;
    private String originatorBankVerificationNumber;
    private String originatorKYCLevel;
    private String transactionLocation;
    private String narration;
    private String paymentReference;
    private String amount;
    private String sessionId;
    private String mobileNumber;
    private String pin;
}
