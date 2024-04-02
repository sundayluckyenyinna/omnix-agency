package com.accionmfb.omnix.agency.module.agency3Line.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PosCallbackRequestPayload {

    @Schema(description = "Unique identifier for the transaction", example = "1234567890")
    private String transactionId;

    @Schema(description = "ID of the POS terminal where the transaction originated", example = "POS-123")
    private String posTerminalId;

    @Schema(description = "Amount of the transaction", example = "100.00")
    private double transactionAmount;
}

