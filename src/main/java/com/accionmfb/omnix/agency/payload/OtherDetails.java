package com.accionmfb.omnix.agency.payload;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class OtherDetails {
    @NotNull(message = "MaskedPAN cannot be null")
    @NotEmpty(message = "MaskedPAN cannot be empty")
    @NotBlank(message = "MaskedPAN cannot be blank")
    private String MaskedPAN;
    @NotNull(message = "STAN cannot be null")
    @NotEmpty(message = "STAN cannot be empty")
    @NotBlank(message = "STAN cannot be blank")
    private String Stan;
    @NotNull(message = "Card expiry cannot be null")
    @NotEmpty(message = "Card expiry cannot be empty")
    @NotBlank(message = "Card expiry cannot be blank")
    @Pattern(regexp = "[0-9]{4}", message = "Card expiry must be 4 digits like 1024")
    private String CardExpiry;
    @NotNull(message = "Terminal ID cannot be null")
    @NotEmpty(message = "Terminal ID cannot be empty")
    @NotBlank(message = "Terminal ID cannot be blank")
    private String TerminalID;
}