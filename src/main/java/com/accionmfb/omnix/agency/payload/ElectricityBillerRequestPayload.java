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
public class ElectricityBillerRequestPayload {

    @NotNull(message = "Biller cannot be null")
    @NotEmpty(message = "Biller cannot be empty")
    @NotBlank(message = "Biller cannot be blank")
    @Pattern(regexp = "^(ABUJA|PHED|IKEJA|EKO|IBADAN|ENUGU|KADUNA|KANO)$", message = "Value must be either ABUJA, IKEJA, EKO, IBADAN, ENUGU, KADUNA, KANO or PHED")
    private String biller;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
}
