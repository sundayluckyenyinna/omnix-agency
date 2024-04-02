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
public class SmartcardRequestPayload {

    @NotNull(message = "Smartcaard cannot be null")
    @NotEmpty(message = "Smartcard cannot be empty")
    @NotBlank(message = "Smartcard cannot be blank")
    @Pattern(regexp = "[0-9]{10,}", message = "Valid smartcard digits required")
    private String smartcard;
    @NotNull(message = "Biller cannot be null")
    @NotEmpty(message = "Biller cannot be empty")
    @NotBlank(message = "Biller cannot be blank")
    @Pattern(regexp = "^(DSTV|GOTV)$", message = "The value must be DSTV or GOTV required")
    private String biller;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
