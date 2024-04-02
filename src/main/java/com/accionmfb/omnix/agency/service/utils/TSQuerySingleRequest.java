package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class TSQuerySingleRequest {
    private String sourceInstitutionCode;
    private int channelCode;
    private String sessionId;
}
