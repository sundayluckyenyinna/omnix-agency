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

import lombok.*;

/**
 *
 * @author bokon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccionAgentBoardPayload {

    @NotNull(message = "ID cannot be null")
    @NotEmpty(message = "ID cannot be empty")
    @NotBlank(message = "ID cannot be blank")
    @Pattern(regexp = "[0-9]{1,}", message = "ID must be an integer")
    private String id;
    @NotNull(message = "Agent name cannot be null")
    @NotEmpty(message = "Agent name cannot be empty")
    @NotBlank(message = "Agent name cannot be blank")
    private String agentName;
    @NotNull(message = "Agent account number cannot be null")
    @NotEmpty(message = "Agent account number cannot be empty")
    @NotBlank(message = "Agent account number cannot be blank")
    private String agentAccountNumber;
    @NotNull(message = "Agent address cannot be null")
    @NotEmpty(message = "Agent address cannot be empty")
    @NotBlank(message = "Agent address cannot be blank")
    private String agentAddress;
    @NotNull(message = "Agent mobile number cannot be null")
    @NotEmpty(message = "Agent mobile number cannot be empty")
    @NotBlank(message = "Agent mobile number cannot be blank")
    private String agentMobileNumber;
    @NotNull(message = "Agent terminal ID cannot be null")
    @NotEmpty(message = "Agent terminal ID cannot be empty")
    @NotBlank(message = "Agent terminal ID cannot be blank")
    private String agentTerminalId;
    @NotNull(message = "Agent state cannot be null")
    @NotEmpty(message = "Agent state cannot be empty")
    @NotBlank(message = "Agent state cannot be blank")
    private String agentState;
    @NotNull(message = "Agent city cannot be null")
    @NotEmpty(message = "Agent city cannot be empty")
    @NotBlank(message = "Agent city cannot be blank")
    private String agentCity;
    @NotNull(message = "Agent supervisor cannot be null")
    @NotEmpty(message = "Agent supervisor cannot be empty")
    @NotBlank(message = "Agent supervisor cannot be blank")
    private String agentSupervisor;
    @NotNull(message = "Agent Id cannot be null")
    @NotEmpty(message = "Agent Id cannot be empty")
    @NotBlank(message = "Agent Id cannot be blank")
    private String agentId;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    @NotNull(message = "Status cannot be null")
    @NotEmpty(message = "Status cannot be empty")
    @NotBlank(message = "Status cannot be blank")
    private String status;
    @NotNull(message = "Agent vendor cannot be null")
    @NotEmpty(message = "Agent vendor cannot be empty")
    @NotBlank(message = "Agent vendor cannot be blank")
    @Pattern(regexp = "^(Grupp)$", message = "Agent vendor must be like Grupp")
    private String agentVendor;
    private String amount;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    private String accountNumber;
    private String accountName;
    private String transactionReference;
}
