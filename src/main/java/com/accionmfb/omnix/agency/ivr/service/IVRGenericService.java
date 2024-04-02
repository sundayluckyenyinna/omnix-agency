package com.accionmfb.omnix.agency.ivr.service;

import com.accionmfb.omnix.agency.model.ivr.ThirdPartyVendors;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import org.springframework.http.HttpHeaders;

/**
 * @author user on 31/10/2023
 */
public interface IVRGenericService {


    String encryptText(String plainText);

    char getTimePeriod();

    String getPostingDate();

    String getStringFromOFSResponse(String response, String stringToGet);

    String endPointPostRequest(String url, String requestJson);

    String endPointPostRequest(String url, String requestJson, String username, String password);

    String validateResponse(String response);

    ThirdPartyVendors getIPDetails(String IP);

    String getProductCodeWithProductCode(String productCode);

    String getBranchNameUsingCode(String branchCode);

    String getT24TransIdFromResponse(String response);

    String generateMnemonic(int max);

    String ofsResponse(String environment, String ofsRequest);
    String hashAccountBalanceRequest(OmnixRequestPayload requestPayload);
    String encryptString(String textToEncrypt, String token);
}
