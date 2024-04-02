package com.accionmfb.omnix.agency.model.ivr;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * @author Olaoye on 30/10/2023
 */

@Entity
@Setter
@Getter
@Table(name = "customerDetails")
public class T24Customers {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;
  @Column(name = "customerName")
  private String customerName;
  @Column(name = "accountOfficer")
  private String accountOfficer;
  @Column(name = "address")
  private String address;
  @Column(name = "gender")
  private String gender;
  @Column(name = "mobileNumber")
  private String mobileNumber;
  @Column(name = "branchName")
  private String branchName;
  @Column(name = "branchCode")
  private String branchCode;
  @Column(name = "customerId")
  private String customerId;
  @Column(name = "bvn")
  private String bvn;
  @Column(name = "email")
  private String email;
  @Column(name = "customerRestriction")
  private String customerRestriction;
  @Column(name = "firstName")
  private String firstName;
  @Column(name = "lastName")
  private String lastName;
  @Column(name = "dob")
  private String dob;
  @Column(name = "street")
  private String street;
  @Column(name = "city")
  private String city;
  @Column(name = "state")
  private String state;
  @Column(name = "middleName")
  private String middleName;
  @Column(name = "kyc_tier")
  private String kycTier;

}
