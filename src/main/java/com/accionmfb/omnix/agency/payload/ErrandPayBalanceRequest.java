package com.accionmfb.omnix.agency.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrandPayBalanceRequest {

    private String serial_number;
    private String pin;
    private String transaction_type;
    private boolean debitAgent;
    private String serviceCode;
    private double amount;
    private String reference;
}