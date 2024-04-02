/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import lombok.*;

/**
 *
 * @author bokon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GruppResponsePayload {

    private String status;
    private String transactionReference;
    private String message;
    private String balance;
    private String token;
    private String responseCode;
}
