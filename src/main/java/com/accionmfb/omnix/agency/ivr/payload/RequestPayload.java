package com.accionmfb.omnix.agency.ivr.payload;

import lombok.Data;

import javax.validation.constraints.Pattern;

/**
 * @author user on 30/10/2023
 */
@Data
public class RequestPayload {

  private String requestType;
  private String inputType;
  @Pattern(regexp = "^(234[7-9]{1}[0-1]{1}[1-9]{1}[0-9]{7})|([0]{1}?[7-9]{1}?[0-1]{1}?[0-9]{8})|([0]{2}?[7-9]{1}?[0-1]{1}?[0-9]{8})$")
  private String mobileNumber;
  private String bvn;
  private String details;
  private String pin;
  private String accountNumber;
  private String transId;
  private String sessionId;
}
