package com.accionmfb.omnix.agency.dto;

import lombok.Data;

@Data
public class FundTransferRequest {
    private String mobileNumber;
    private String debitAccount;
    private String creditAccount;
    private String amount;
    private String narration;
    private String transType;
    private String branchCode;
    private String inputter;
    private String authorizer;
    private String noOfAuthorizer;
    private String requestId;
    private String hash;
}
