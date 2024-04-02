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
public class FundsTransferResponsePayload {

    private String debitAccount;
    private String debitAccountName;
    private String creditAccount;
    private String creditAccountName;
    private String amount;
    private String narration;
    private String responseCode;
    private String transRef;
    private String status;
    private String handshakeId;
    private String responseMessage;
    private String t24TransRef;
}
