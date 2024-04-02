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
public class CableTVRequestPayload {

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
    @NotNull(message = "Biller ID cannot be null")
    @NotEmpty(message = "Biller ID cannot be empty")
    @NotBlank(message = "Biller ID cannot be blank")
    @Pattern(regexp = "[0-9]{1,}", message = "The value must be an integer")
    private String billerId;
    @NotNull(message = "Smart card number cannot be null")
    @NotEmpty(message = "Smart card number cannot be empty")
    @NotBlank(message = "Smart card number cannot be blank")
    private String smartCard;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
