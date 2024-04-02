/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.model;

import io.micrometer.core.lang.Nullable;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.*;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "funds_transfer")
public class FundsTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "debit_account")
    private String debitAccount;
    @Column(name = "debit_account_name")
    private String debitAccountName;
    @Column(name = "debit_account_kyc")
    private String debitAccountKyc;
    @Column(name = "credit_account")
    private String creditAccount;
    @Column(name = "credit_account_name")
    private String creditAccountName;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "source_bank")
    private String sourceBank;
    @Column(name = "destination_bank")
    private String destinationBank;
    @Column(name = "credit_account_kyc")
    private String creditAccountKyc;
//    @ManyToOne(cascade = {CascadeType.ALL, CascadeType.MERGE})
    @ManyToOne(cascade = CascadeType.ALL)
    @Nullable
    private Customer customer;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "narration")
    private String narration;
    @Column(name = "amount")
    private String amount;
    @Column(name = "fee")
    private String fee;
    @Column(name = "debit_currency")
    private String debitCurrency;
    @Column(name = "credit_currency")
    private String creditCurrency;
    @Column(name = "time_period")
    private char timePeriod;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "status")
    private String status;
    @Column(name = "t24_trans_ref")
    private String t24TransRef;
    @Column(name = "accion_fee_t24_ref")
    private String accionFeeT24Ref;
    @Column(name = "gateway")
    private String gateway;
    @Column(name = "trans_type")
    private String transType;
    @ManyToOne
    private Branch branch;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "destination_bank_code")
    private String destinationBankCode;
    @Column(name = "debit_account_type")
    private String debitAccountType;
    @Column(name = "credit_account_type")
    private String creditAccountType;
    @Column(name = "settlement_account")
    private String settelemtAccount;

    @Column(name = "notify_status", length = 1)
    private String notifyStatus = "N";

}
