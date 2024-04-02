package com.accionmfb.omnix.agency.module.agency3Line.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequestPayload {

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "[0-9]{10}", message = "Account number must be either 10 digit account number")
    @Schema(name = "Debit Account Number", example = "0123456789", description = "10 digit NUBAN account number")
    private String accountNumber;

}