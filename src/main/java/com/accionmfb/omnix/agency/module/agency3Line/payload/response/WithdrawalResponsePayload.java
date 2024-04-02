package com.accionmfb.omnix.agency.module.agency3Line.payload.response;


import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawalResponsePayload {

    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Original request ID")
    private String requestId;

    @Schema(name = "Transaction Status", example = "SUCCESS", description = "Transaction outcome")
    private String status;

    @Schema(name = "Status Message", example = "Withdrawal successful", description = "Additional details about the status")
    private String message;

    @Schema(name = "Transaction ID", example = "1234567890", description = "Unique identifier for the withdrawal transaction (if successful)")
    private String transactionId;

    @Schema(name = "Account Balance", example = "10000.00", description = "The updated account balance after the withdrawal (if successful)")
    private BigDecimal balance;

    @Schema(name = "Reference Number", example = "REF123456", description = "Reference for external systems or reconciliation (if applicable)")
    private String referenceNumber;

    @Schema(name = "Fee Amount", example = "50.00", description = "Any fees associated with the withdrawal (if applicable)")
    private BigDecimal feeAmount;

    @Schema(name = "Error Code", example = "INSUFFICIENT_FUNDS", description = "Specific error code for troubleshooting (if unsuccessful)")
    private String errorCode;

    @Schema(name = "Error Description", example = "The available balance is insufficient for this withdrawal", description = "Clear explanation of the error that occurred (if unsuccessful)")
    private String errorDescription;
}

