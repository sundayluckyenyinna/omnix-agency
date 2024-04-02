package com.accionmfb.omnix.agency.module.agency3Line.payload.request;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawalRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "Account number must be either 10 digit account number")
    @Schema(name = "Debit Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;

    @NotBlank(message = "Transaction narration is required")
    @Schema(name = "Transaction Narration", example = "Cash Withdrawal ATM Lagos", description = "Transaction Narration")
    private String narration;

    @NotBlank(message = "Transaction amount is required")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Transaction Amount must contain only digits, comma or dot only")
    @Schema(name = "Transaction Amount", example = "1,000.00", description = "Transaction Amount")
    private String amount;

    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "PYLON67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;

    @NotBlank(message = "Request ID is required")
    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Request ID is required")
    private String requestId;

    public String getOfsRequest() {
        return getAccountNumber();
    }
}

