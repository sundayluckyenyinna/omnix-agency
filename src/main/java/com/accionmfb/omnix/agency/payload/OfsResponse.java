package com.accionmfb.omnix.agency.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfsResponse {
    private String ofsRequest;
    private String ofsResponse;
}