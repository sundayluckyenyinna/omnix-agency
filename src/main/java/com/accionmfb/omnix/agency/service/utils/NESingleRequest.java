package com.accionmfb.omnix.agency.service.utils;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class NESingleRequest {
    private String destinationInstitutionCode;
    private String channelCode;
    private String accountNumber;
    private String mobileNumber;
    private String pin;
}
