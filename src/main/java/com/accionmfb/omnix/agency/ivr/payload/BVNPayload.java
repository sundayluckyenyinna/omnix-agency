package com.accionmfb.omnix.agency.ivr.payload;

import lombok.Data;

/**
 * @author user on 30/10/2023
 */
@Data
public class BVNPayload {

  private String firstName;
  private String lastName;
  private String middleName;
  private String dob;
  private String gender;
  private String responseCode;
}
