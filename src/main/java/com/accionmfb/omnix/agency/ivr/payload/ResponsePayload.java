package com.accionmfb.omnix.agency.ivr.payload;

import lombok.Data;
import java.util.List;

/**
 * @author user on 30/10/2023
 */

@Data
public class ResponsePayload {

  private String branchCode;
  private String responseCode;
  private String responseMessage;
  private String responseDescription;
  private String transRef;
  private String paymentReference;
  private String accountNumber;
  private String status_code;
  private String PAN;
  private String cardStatus;
  //Used by Axa Mansard
  private String responseStatus;
  private String successCounter;
  private String errorCounter;
  private List<Object> responseErrors;

  //used by Xpresspay
  private String token;
  private String energyValue;
  private String units;
  private String vat;
}
