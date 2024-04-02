package com.accionmfb.omnix.agency.model.ivr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author user on 31/10/2023
 */
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bvn_ivr")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BVNIVR implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "customer_bvn")
  private String customerBvn;

  @Column(name = "date_of_birth")
  private String dateOfBirth;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "gender")
  private String gender;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "mobile_number")
  private String mobileNumber;

  @Column(name = "source")
  private String source;

  @Column(name = "image")
  private String image;

  @Column(name = "basicDetailBase64", length = 100000)
  private String basicDetailBase64;

  @Column(name = "imageBase64", length = 100000)
  private String imageBase64;

  @Column(name = "status")
  private String status;

  @Column(name = "middle_name")
  private String middleName;

  @Column(name = "registration_date")
  private String registrationDate;

  @Column(name = "request_from")
  private String requestFrom;

  @Column(name = "remote_ip")
  private String remoteIP;


}
