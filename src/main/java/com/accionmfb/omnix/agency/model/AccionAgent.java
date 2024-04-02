/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.*;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "accion_agent")
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccionAgent implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "agent_account_number")
    private String agentAccountNumber;
    @Column(name = "agent_id")
    private String agentId;
    @Column(name = "agent_name")
    private String agentName;
    @Column(name = "agent_state")
    private String agentState;
    @Column(name = "agent_city")
    private String agentCity;
    @Column(name = "agent_address")
    private String agentAddress;
    @Column(name = "agent_mobile")
    private String agentMobile;
    @Column(name = "agent_supervisor")
    private String agentSupervisor;
    @Column(name = "date_registered")
    private String dateRegistered;
    @Column(name = "ranking")
    private String ranking;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "status")
    private String status;
    @Column(name = "terminal_id")
    private String terminalId;
    @Column(name = "agent_vendor")
    private String agentVendor;
    @Column(name = "remaining_limit")
    private BigDecimal remainingLimit;

    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "bvn")
    private String bvn;
    @Column(name = "kyc_level")
    private String kycLevel;

}
