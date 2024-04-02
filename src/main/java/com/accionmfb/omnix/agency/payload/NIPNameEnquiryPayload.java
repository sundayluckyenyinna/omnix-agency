/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NIPNameEnquiryPayload {

    @NotBlank(message = "Beneficiary account is required")
    @Pattern(regexp = "[0-9]{10}", message = "Beneficiary account must be either 10 digit account number")
    @Schema(name = "Beneficiary Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String beneficiaryAccount;
    @NotBlank(message = "Beneficiary bank code is required")
    @Pattern(regexp = "[0-9]{6}", message = "Beneficiary bank code must be either 6 digit bank code")
    @Schema(name = "Beneficiary bank code", example = "123456", description = "6 digit bank code")
    private String beneficiaryBankCode;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "PYLON67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotBlank(message = "Request ID is required")
    @Schema(name = "Request ID", example = "PYLON67XXTY78999GHTRE", description = "Request ID is required")
    private String requestId;
    private String token;
}
