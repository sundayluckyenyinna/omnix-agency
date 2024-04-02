/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author eisrael
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionPosting {

    @NotNull(message = "TransRefNo cannot be null")
    @NotEmpty(message = "TransRefNo cannot be empty")
    @NotBlank(message = "TransRefNo cannot be blank")
    private String transRefNo;
    private String debitAccountNumber;
    @NotNull(message = "Credit Account Number cannot be null")
    @NotEmpty(message = "Credit Account Number cannot be empty")
    @NotBlank(message = "Credit Account Number cannot be blank")
    private String creditAccountNumber;
    @NotNull(message = "Narration cannot be null")
    @NotEmpty(message = "Narration cannot be empty")
    @NotBlank(message = "Narration cannot be blank")
    private String narration;
    @NotNull(message = "Tran Code cannot be null")
    @NotEmpty(message = "Tran Code cannot be empty")
    @NotBlank(message = "Tran Code cannot be blank")
    private String tranCode;
    private String CheqNo;
    @NotNull(message = "Amount cannot be null")
    @NotEmpty(message = "Amount cannot be empty")
    @NotBlank(message = "Amount cannot be blank")
    private String amount;
    @NotNull(message = "Tran Charge cannot be null")
    @NotEmpty(message = "Tran Charge cannot be empty")
    @NotBlank(message = "Tran Charge cannot be blank")
    private String tranCharge;
    @NotNull(message = "Value Date cannot be null")
    @NotEmpty(message = "Value Date cannot be empty")
    @NotBlank(message = "Value Date cannot be blank")
    private String valueDate;
    @NotNull(message = "Bank FIID cannot be null")
    @NotEmpty(message = "Bank FIID cannot be empty")
    @NotBlank(message = "Bank FIID cannot be blank")
    @SerializedName(value = "BankFiid", alternate = {"bankFiid"})
    private String bankFiid;
    private int RespCode;
    private String RespMessage;
}
