/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import lombok.Data;

/**
 *
 * @author ofasina
 */
@Data
public class OmnixRequestPayload {

    private String accountNumber;

    private String requestId;

    private String token;

    private String mobileNumber;

    private String startDate;

    private String endDate;

    private String accountOfficer;

    private String branchCode;

    private String customerNumber;

    private String otherOfficer;

    private String productCode;

    private String bvn;

    private String maritalStatus;

    private String sector;

    private String stateOfResidence;

    private String cityOfResidence;

    private String residentialAddress;

    private String lastName;

    private String otherName;

    private String dob;

    private String gender;

    private String debitAccount;

    private String creditAccount;

    private String amount;

    private String narration;

    private String transType;

    private String inputter;

    private String authorizer;

    private String noAuthorizer;

    private String transRef;

    private String beneficiaryAccount;

    private String beneficiaryAccountName;

    private String beneficiaryBankCode;

    private String beneficiaryKycLevel;

    private String beneficiaryBvn;

    private String thirdPartyMobileNumber;

    private String thirdPartyTelco;

    private String dataPlanId;

    private String smartCard;

    private String billerId;

    private String biller;

    private String meterNumber;

    private String hash;

    private String nameEnquiryRef;

}
