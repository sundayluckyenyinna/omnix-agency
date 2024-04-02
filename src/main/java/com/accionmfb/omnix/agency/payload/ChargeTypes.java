/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

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
public class ChargeTypes {

    @NotNull(message = "Charge type is required")
    @Pattern(regexp = "[A-Za-z]{1,}", message = "Charge type must be alphabets")
    private String chargeType;
    @NotNull(message = "Charge amount is required")
    @Pattern(regexp = "(?=.*?\\d)^\\$?(([1-9]\\d{0,2}(,\\d{3})*)|\\d+)?(\\.\\d{1,3})?$", message = "Charge amount must be numeric")
    private String chargeAmount;
}
