package com.accionmfb.omnix.agency.payload;

import lombok.Data;

@Data
public class ExcelTransaction {
    private String firstName;
    private String lastName;
    private String businessName;
    private String phoneNumber;
    private String userType;
    private String reference;
    private String uniqueId;
    private String amount;
    private String serviceFee;
    private String bankServiceFee;
    private String customerServiceFee;
    private String billerId;
    private String transactionType;
    private String status;
    private String rrn;
    private String stan;
    private String maskedPan;
    private String cardDescription;
    private String cardType;
    private String terminalId;
    private String date;
}
