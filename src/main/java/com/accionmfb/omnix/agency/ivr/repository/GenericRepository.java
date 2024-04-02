package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.ThirdPartyVendors;

import java.time.LocalDate;

/**
 * @author user on 31/10/2023
 */
public interface GenericRepository {

    ThirdPartyVendors get3rdPartyDetailsWithIPAddress(String ipAddress);

    String getBranchNameFromCode(String branchCode);

    String getProductCodeWithProductId(String productCode);
}
