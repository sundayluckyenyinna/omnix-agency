package com.accionmfb.omnix.agency.module.agency3Line.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepositResponsePayload {
    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Original request ID")
    private String requestId;

    @Schema(name = "Transaction Status", example = "SUCCESS", description = "Transaction outcome")
    private String status;

    @Schema(name = "Status Message", example = "Deposit successful", description = "Additional details about the status")
    private String message;

    // Continue with other fields as needed...
}

