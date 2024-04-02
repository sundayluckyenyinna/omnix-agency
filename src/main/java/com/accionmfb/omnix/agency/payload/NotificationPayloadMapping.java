package com.accionmfb.omnix.agency.payload;

import lombok.Data;

@Data
public class NotificationPayloadMapping {

    private String amount;
    private String reference;
    private String status;
    private String statusCode;
    private String transactionType;
    private String terminalId;
    private String statusDescription;
    private String transactionDate;
}
