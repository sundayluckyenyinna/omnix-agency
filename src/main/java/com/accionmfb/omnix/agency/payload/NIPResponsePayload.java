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
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NIPResponsePayload {

    private ActionPayload action;
    private String contextData;
    private String responseCode;
    private String responseMessage;
    private String responseDecription;
    private String responseDescription;
    private String transRef;
    private String paymentReference;
}
