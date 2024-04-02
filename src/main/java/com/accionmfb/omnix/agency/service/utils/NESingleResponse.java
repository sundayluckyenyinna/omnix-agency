package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class NESingleResponse {

    private String sessionId;
    private String destinationInstitutionCode;
    private String channelCode;
    private String accountNumber;
    private String accountName;
    private String bankVerificationNumber;
    private String responseCode;
    private String kycLevel;
}
