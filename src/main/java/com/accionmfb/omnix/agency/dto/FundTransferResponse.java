package com.accionmfb.omnix.agency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundTransferResponse {
    private String debitAccount;
    private String debitAccountName;
    private String creditAccount;
    private String creditAccountName;
    private String amount;
    private String narration;
    private String responseCode;
    private String transRef;
    private String status;
}
