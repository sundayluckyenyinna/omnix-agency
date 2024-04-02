package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class FTSingleCreditResponse {
    private String sessionId;
    private String nameEnquiryRef;
    private String destinationInstitutionCode;
    private int channelCode;
    private String beneficiaryAccountName;
    private String beneficiaryAccountNumber;
    private String beneficiaryBankVerificationNumber;
    private String originatorAccountName;
    private String originatorAccountNumber;
    private String originatorBankVerificationNumber;
    private String originatorKYCLevel;
    private String transactionLocation;
    private String narration;
    private String paymentReference;
    private String amount;
    private String responseCode;
    private String responseMessage;
    private String responseDecription;
    private String responseDescription;
    private String beneficiaryKYCLevel;
}
