/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 *
 * @author bokon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountBalanceResponsePayload {

    private String availableBalance;
    private String ledgerBalance;
    private String accountNumber;
    private String responseCode;
    private String responseMessage;
    private String branchCode;
    private String categoryCode;
    private String accountName;
    private String cif;
    private String productName;
    private String oldAccountNumber;
}
