/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.model.ivr;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author ofasina
 */
@Entity
@Table(name = "ivr_account")
public class IVRAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "bvn")
    private String bvn;
    @Column(name = "kyc")
    private String kyc;
    @Column(name = "status")
    private String status;
    @Column(name = "session_id")
    private String sessionId;
    @Column(name = "create_customer_first")
    private boolean createCustomerFirst;
    @Column(name = "numbering_code_generated")
    private boolean numberingCodeGenerated;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "gender")
    private String gender;
    @Column(name = "birth_date")
    private String dateOfBirth;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "product_category")
    private String productCategory;
    @Column(name = "failure_reason")
    private String failureReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBvn() {
        return bvn;
    }

    public void setBvn(String bvn) {
        this.bvn = bvn;
    }

    public String getKyc() {
        return kyc;
    }

    public void setKyc(String kyc) {
        this.kyc = kyc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isCreateCustomerFirst() {
        return createCustomerFirst;
    }

    public void setCreateCustomerFirst(boolean createCustomerFirst) {
        this.createCustomerFirst = createCustomerFirst;
    }

    public boolean isNumberingCodeGenerated() {
        return numberingCodeGenerated;
    }

    public void setNumberingCodeGenerated(boolean numberingCodeGenerated) {
        this.numberingCodeGenerated = numberingCodeGenerated;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    
    
    

}
