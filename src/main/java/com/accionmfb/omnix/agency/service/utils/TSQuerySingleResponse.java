package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class TSQuerySingleResponse {
    private String sessionId;
    private String sourceInstitutionCode;
    private int channelCode;
    private String responseCode;
}
