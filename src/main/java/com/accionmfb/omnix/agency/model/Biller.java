/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "biller")
public class Biller implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "biller")
    private String biller;
    @Column(name = "bouquet")
    private String bouquet;
    @Column(name = "package_name")
    private String packageName;
    @Column(name = "amount")
    private String amount;
    @Column(name = "biller_id")
    private String billerId;
    @Column(name = "product_id")
    private String productId;
    @Column(name = "status")
    private String status;
    @Column(name = "vendor")
    private String vendor;
}
