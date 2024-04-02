package com.accionmfb.omnix.agency.model.ivr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author Olaoye on 31/10/2023
 */
@Entity
@Table(name = "connecting_ip")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyVendors implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Column(name = "vendor_name")
  private String vendorName;
  @Column(name = "vendor_ip_address")
  private String vendorIPAddress;
  @Column(name = "application")
  private String application;
  @Column(name = "created_at")
  private LocalDateTime createdAt;
  @Column(name = "status")
  private String status;
  @Column(name = "environment")
  private String environment;
  @Column(name = "connecting_services")
  private String connectingServices;
}
