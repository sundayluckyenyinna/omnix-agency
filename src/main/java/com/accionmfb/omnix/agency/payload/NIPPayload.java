/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import lombok.*;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NIPPayload {

    private String nameEnquiryRef;
    private String destinationInstitutionCode;
    private String channelCode;
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
    private String accountNumber;
    private String responseCode;
    private String responseMessage;
    private String responseDecription;
    private String accountName;
    private String bankVerificationNumber;
    private String kycLevel;
}
