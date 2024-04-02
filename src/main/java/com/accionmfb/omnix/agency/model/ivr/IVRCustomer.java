/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 *
 * @author bokon
 */
@Entity
@Table(name = "ussd_customer")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IVRCustomer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "date_of_birth")
    private String dateOfBirth;
    @Column(name = "gender")
    private String gender;
    @Column(name = "customer_number")
    private String customerNumber;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "customer_type")
    private String customerType;
    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "pin")
    private String pin;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "telco")
    private String telco;
    @Column(name = "otp")
    private String otp;
    @Column(name = "status")
    private String status;
    @Column(name = "security_question")
    private String securityQuestion;
    @Column(name = "security_answer")
    private String securityAnswer;
    @Column(name = "unboard_at")
    private String unboardAt;
    @Column(name = "session_id")
    private String sessionId;
    @Column(name = "time_period")
    private char timePeriod;
    @Column(name = "trans_id")
    private String transId;
    @Column(name = "residence_state")
    private String residenceState;
    @Column(name = "residence_city")
    private String residenceCity;
    @Column(name = "residence_address")
    private String residenceAddress;
    @Column(name = "trans_limit")
    private String transactionLimit;
    @Column(name = "daily_limit")
    private String dailyLimit;
    @Column(name = "optout_date")
    private String optoutDate;
    @Column(name = "failure_reason")
    private String failureReason;

}
