package com.accionmfb.omnix.agency.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrandPayBalanceResponse {

    private boolean is_pin_valid;
    private double balance;
    private String agent_status;
}