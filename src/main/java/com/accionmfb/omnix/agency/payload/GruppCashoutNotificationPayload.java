/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
public class GruppCashoutNotificationPayload {

    @NotNull(message = "PAN cannot be null")
    @NotEmpty(message = "PAN cannot be empty")
    @NotBlank(message = "PAN cannot be blank")
    private String pan;
    @NotNull(message = "RRN cannot be null")
    @NotEmpty(message = "RRN cannot be empty")
    @NotBlank(message = "RRN cannot be blank")
    private String rrn;
    @NotNull(message = "STAN cannot be null")
    @NotEmpty(message = "STAN cannot be empty")
    @NotBlank(message = "STAN cannot be blank")
    private String stan;
    @NotNull(message = "Amount cannot be null")
    @NotEmpty(message = "Amount cannot be empty")
    @NotBlank(message = "Amount cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Transaction Amount must contain only digits, comma or dot only")
    private String amount;
    @NotNull(message = "Reference cannot be null")
    @NotEmpty(message = "Reference cannot be empty")
    @NotBlank(message = "Reference cannot be blank")
    private String reference;
    @NotNull(message = "Card expiry cannot be null")
    @NotEmpty(message = "Card expiry cannot be empty")
    @NotBlank(message = "Card expiry cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "Card expiry must be 4 digits like 1024")
    private String cardExpiry;
    @NotNull(message = "Status cannot be null")
    @NotEmpty(message = "Status cannot be empty")
    @NotBlank(message = "Status cannot be blank")
    @Pattern(regexp = "[a-zA-Z]{1,}", message = "Status must be like 'Success'")
    private String status;
    @NotNull(message = "Status code cannot be null")
    @NotEmpty(message = "Status code cannot be empty")
    @NotBlank(message = "Status code cannot be blank")
    @Pattern(regexp = "[0-9]{2}", message = "Status code must be 2 digit code like 00")
    private String statusCode;
    @NotNull(message = "Trasaction type cannot be null")
    @NotEmpty(message = "Transaction type cannot be empty")
    @NotBlank(message = "Transaction type cannot be blank")
    private String transactionType;
    @NotNull(message = "Terminal ID cannot be null")
    @NotEmpty(message = "Terminal ID cannot be empty")
    @NotBlank(message = "Terminal ID cannot be blank")
    private String terminalId;
    @NotNull(message = "Serial number cannot be null")
    @NotEmpty(message = "Serial number cannot be empty")
    @NotBlank(message = "Serial number cannot be blank")
    private String serialNumber;
    @NotNull(message = "Status description cannot be null")
    @NotEmpty(message = "Status description cannot be empty")
    @NotBlank(message = "Status description cannot be blank")
    private String statusDescription;
    @NotNull(message = "Transaction date cannot be null")
    @NotEmpty(message = "Transaction date cannot be empty")
    @NotBlank(message = "Transaction date cannot be blank")
    private String transactionDate;
    @NotNull(message = "Hash cannot be null")
    @NotEmpty(message = "Hash cannot be empty")
    @NotBlank(message = "Hash cannot be blank")
    private String hash;
}
