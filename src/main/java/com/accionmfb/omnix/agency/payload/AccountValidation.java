/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import com.google.gson.annotations.SerializedName;
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
public class AccountValidation {

    @NotNull(message = "Account Number cannot be null")
    @NotEmpty(message = "Account number cannot be empty")
    @NotBlank(message = "Account number cannot be blank")
    @SerializedName(value = "AcctNo", alternate = {"acctNo"})
    @Pattern(regexp = "[0-9]{10}", message = "10 NUBAN account number required")
    private String acctNo;
    @NotNull(message = "Bank FIID cannot be null")
    @NotEmpty(message = "Bank FIID cannot be empty")
    @NotBlank(message = "Bank FIID cannot be blank")
    @SerializedName(value = "Bankfiid", alternate = {"bankfiid"})
    private String bankFiid;
    @NotNull(message = "Institution ID cannot be null")
    @NotEmpty(message = "Institution ID cannot be empty")
    @NotBlank(message = "Institution ID cannot be blank")
    @SerializedName(value = "InstitutionID", alternate = {"institutionID"})
    private String institutionID;
    private String accttype;
    private String AccountTitle;
    private String Currency;
    private String ChequeDepositFlag;
    private String AccountStatus;
    private String AvailableBalance;
    private int RespCode;
    private String RespMessage;
    private String BranchName;
}
