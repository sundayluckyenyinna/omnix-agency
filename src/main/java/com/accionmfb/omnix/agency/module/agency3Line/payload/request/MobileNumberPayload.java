package com.accionmfb.omnix.agency.module.agency3Line.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MobileNumberPayload {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "[0-9]{10,11}", message = "Mobile number must be 10 or 11 digits")
    @Schema(name = "Mobile Number", example = "08037727138", description = "11 digit mobile number")
    private String mobileNumber;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
}