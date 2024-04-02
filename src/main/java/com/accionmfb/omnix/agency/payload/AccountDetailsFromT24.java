package com.accionmfb.omnix.agency.payload;

import lombok.Data;

@Data
public class AccountDetailsFromT24 {
    private String customerId;
    private String accountNumber;
}
