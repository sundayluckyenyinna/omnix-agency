/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

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
@NoArgsConstructor
@AllArgsConstructor
public class AccionAgentResponsePayload {

    private String agentName;
    private String accountNumber;
    private String responseCode;
    private String phoneNumber;
    private String bankCode;
    private String agentVendor;
    private String id;
    private String terminalId;
    private String agentSupervisor;
    private String agentAddress;
    private String agentCity;
    private String agentState;
}
