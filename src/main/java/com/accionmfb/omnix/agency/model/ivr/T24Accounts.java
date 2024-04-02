package com.accionmfb.omnix.agency.model.ivr;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author user on 30/10/2023
 */

@Setter
@Getter
@Entity
@Table(name = "accountDetails")
public class T24Accounts implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;
  @Column(name = "bvn")
  private String bvn;
  @Column(name = "t24AccountNumber")
  private String t24AccountNumber;
  @Column(name = "accountNumber")
  private String accountNumber;
  @Column(name = "accountName")
  private String accountName;
  @Column(name = "mobileNumber")
  private String mobileNumber;
  @Column(name = "accountRestriction")
  private String accountRestriction;
  @Column(name = "accountStatus")
  private String accountStatus;
  @Column(name = "customerId")
  private String customerId;
  @Column(name = "currency")
  private String currency;
  @Column(name = "branchCode")
  private String branchCode;
  @Column(name = "branchName")
  private String branchName;
  @Column(name = "accountType")
  private String accountType;
  @Column(name = "productCode")
  private String productCode;
  @Column(name = "accountDescription")
  private String accountDescription;
  @Column(name = "productDescription")
  private String productDescription;
  @Column(name = "categoryCode")
  private String categoryCode;
  @Column(name = "openingDate")
  private String openingDate;
}
