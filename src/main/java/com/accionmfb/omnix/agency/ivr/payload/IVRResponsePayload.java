package com.accionmfb.omnix.agency.ivr.payload;

import lombok.Data;

/**
 * @author user on 30/10/2023
 */

@Data
public class IVRResponsePayload {

  private String mobileNumber;
  private String pin;
  private String responseCode;
  private String responseDescription;
  private BVNPayload bvn;
  private String availableBalance;
  private String accountNumber;
  private String sessionId;
}
