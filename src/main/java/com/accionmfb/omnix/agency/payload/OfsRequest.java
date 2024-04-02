package com.accionmfb.omnix.agency.payload;

import com.accionmfb.omnix.agency.module.agency3Line.payload.request.WithdrawalRequestPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfsRequest extends WithdrawalRequestPayload {
    private String ofsRequest;
}
