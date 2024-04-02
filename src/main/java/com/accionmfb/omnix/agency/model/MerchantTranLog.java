package com.accionmfb.omnix.agency.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author dofoleta
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "merchant_tran_log")
public class MerchantTranLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    
    private String country;
    private String instrumentId;
    private String fee;
    private String channel;
    private String displayedFailure;
    private String reference;
    private String updated_at;
    private String currency;
    private boolean refunded;
    private String instrument_id;
    private String timestamp;
    private String amount;
    private String instrumentType;
    private String transactionId;
    private String token;
    private String bussinessType;
    private String payChannel;
    private String status;
    private String terminalId;
    private String serialNumber;
    private String stampDuty;
    private String retrievalReferenceNumber;
    private String transactionReference;
    private String stan;
    private String maskedPan;
}
