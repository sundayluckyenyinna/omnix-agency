package com.accionmfb.omnix.agency.payload;

import lombok.Data;

@Data
public class CustomerDetailsFromT24 {

    private String otherName;
    private String kycTier;
    private String lastName;
    private String customerType;
}
