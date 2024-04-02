package com.accionmfb.omnix.agency.dto;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class Payload {

    private String country;
    private String instrumentId;
    private String fee;
    private String channel;
    private String displayedFailure;
    private String reference;
    private String updated_at;
    private String currency;
    private boolean refunded;
    private String instrument_id;
    private String timestamp;
    private String amount;
    private String instrumentType;
    private String transactionId;
    private String token;
    private String bussinessType;
    private String payChannel;
    private String status;
    private String terminalId;
    private String serialNumber;
    private String stampDuty;
    private String retrievalReferenceNumber;
    private String transactionReference;
    private String stan;
    private String maskedPan;
}
