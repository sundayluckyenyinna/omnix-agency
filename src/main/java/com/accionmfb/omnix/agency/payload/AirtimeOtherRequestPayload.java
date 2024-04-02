/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
public class AirtimeOtherRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "Debit account cannot be null")
    @NotEmpty(message = "Debit account cannot be empty")
    @NotBlank(message = "Debit account cannot be blank")
    @Pattern(regexp = "[0-9]{10}", message = "Debit account must be either 10 digit account number")
    private String debitAccount;
    @NotNull(message = "Third party mobile number cannot be null")
    @NotEmpty(message = "Third party mobile number cannot be empty")
    @NotBlank(message = "Third party mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String thirdPartyMobileNumber;
    @NotNull(message = "Third party telco cannot be null")
    @NotEmpty(message = "Third party telco cannot be empty")
    @NotBlank(message = "Third party telco cannot be blank")
    @Pattern(regexp = "^(MTN|mtn|GLO|glo|Airtel|airtel|Etisalat|etisalat|9Mobile|9mobile|SMILE|smile|SPECTRANET|spectranet)$", message = "Value must be either MTN, GLO, Airtel, Etisalat, 9Mobile, Smile or Spectranet")
    private String thirdPartyTelco;
    @NotNull(message = "Amount cannot be null")
    @NotEmpty(message = "Amount cannot be empty")
    @NotBlank(message = "Amount cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Amount must contain only digits, comma or dot only")
    private String amount;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
