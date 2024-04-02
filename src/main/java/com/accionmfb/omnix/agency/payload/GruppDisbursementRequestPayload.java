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
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GruppDisbursementRequestPayload {

    @NotNull(message = "Amount cannot be null")
    @NotEmpty(message = "Amount cannot be empty")
    @NotBlank(message = "Amount cannot be blank")
    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Transaction Amount must contain only digits, comma or dot only")
    private String amount;
    @NotNull(message = "Terminal ID cannot be null")
    @NotEmpty(message = "Terminal ID cannot be empty")
    @NotBlank(message = "Terminal ID cannot be blank")
    private String terminalId;
    private String productCategory;
    @NotNull(message = "Serial number cannot be null")
    @NotEmpty(message = "Serial number cannot be empty")
    @NotBlank(message = "Serial number cannot be blank")
    private String serialNumber;
    @NotNull(message = "Trasaction type cannot be null")
    @NotEmpty(message = "Transaction type cannot be empty")
    @NotBlank(message = "Transaction type cannot be blank")
    @Pattern(regexp = "^(PHCN|Phcn|TRANSFER|Transfer|AIRTIME|Airtime|CABLE_TV|Cable_TV)$", message = "Transaction Type must be like TRANSFER, AIRTIME, CABLE_TV or PHCN")
    private String transactionType;
    @NotNull(message = "Customer biller ID cannot be null")
    @NotEmpty(message = "Customer biller ID cannot be empty")
    @NotBlank(message = "Customer biller ID cannot be blank")
    @Pattern(regexp = "[0-9]{10,11}", message = "Customer biller id must be a 10 or 11 numerical values")
    private String customerBillerId;
    @NotNull(message = "Unique identifier cannot be null")
    @NotEmpty(message = "Unique identifier cannot be empty")
    @NotBlank(message = "Unique identifier cannot be blank")
    private String uniqueIdentifier;
    private String bankCode;
    private String product;
    private String packageName;
    private String customerPhoneNumber;
    @NotNull(message = "Hash cannot be null")
    @NotEmpty(message = "Hash cannot be empty")
    @NotBlank(message = "Hash cannot be blank")
    private String hash;
}
