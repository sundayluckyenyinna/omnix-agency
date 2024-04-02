/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.*;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GruppAgentBalancePayload {

    @NotBlank(message = "NUBAN account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "NUBAN account number must be 10 digit")
    @Schema(name = "Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;
    private String requestId;
}
