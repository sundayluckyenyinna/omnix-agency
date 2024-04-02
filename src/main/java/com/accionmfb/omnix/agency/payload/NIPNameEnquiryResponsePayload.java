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
@AllArgsConstructor
@NoArgsConstructor
public class NIPNameEnquiryResponsePayload {

    private String nameEnquiryRef;
    private String beneficiaryBankCode;
    private String beneficiaryBvn;
    private String beneficiaryKycLevel;
    private String beneficiaryAccountName;
    private String responseCode;
}
