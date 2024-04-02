package com.accionmfb.omnix.agency.module.agency3Line.payload.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PosCallbackResponsePayload {

    @Schema(description = "Indicates whether the callback was successful or not", example = "SUCCESS")
    private String status;

    @Schema(description = "Error message in case of failure")
    private String errorMessage;
}

