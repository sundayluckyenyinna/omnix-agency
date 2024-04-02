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
public class CableTVResponsePayload {

    private String responseCode;
    private String amount;
    private String mobileNumber;
    private String biller;
    private String bouquet;
    private String bouquetName;
    private String productId;
    private String customerName;
    private String requestId;
    private String transRef;
}
