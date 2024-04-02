/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
public class IndivCustomerWithBvnRequestPayload {

    @NotNull(message = "Mobile number cannot be null")
    @NotEmpty(message = "Mobile number cannot be empty")
    @NotBlank(message = "Mobile number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit mobile number required")
    private String mobileNumber;
    @NotNull(message = "BVN cannot be null")
    @NotEmpty(message = "BVN cannot be empty")
    @NotBlank(message = "BVN cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "11 digit BVN required")
    private String bvn;
    private String branchCode;
    @NotNull(message = "Marital status cannot be null")
    @NotEmpty(message = "Marital status cannot be empty")
    @NotBlank(message = "Marital status cannot be blank")
    @Pattern(regexp = "^(Single|Married|Divorced)$", message = "Value must be either Single, Married or Divorced")
    private String maritalStatus;
    @NotNull(message = "State of residence cannot be null")
    @NotEmpty(message = "State of residence cannot be empty")
    @NotBlank(message = "State of residence cannot be blank")
    private String stateOfResidence;
    @NotNull(message = "City of residence cannot be null")
    @NotEmpty(message = "City of residence cannot be empty")
    @NotBlank(message = "City of residence cannot be blank")
    private String cityOfResidence;
    @NotNull(message = "Residential address cannot be null")
    @NotEmpty(message = "Residential address cannot be empty")
    @NotBlank(message = "Residential address cannot be blank")
    private String residentialAddress;
    private String accountOfficer;
    private String otherOfficer;
    private String sector;
    @NotBlank(message = "Hash value is required")
    @Schema(name = "Hash value", example = "OBA67XXTY78999GHTRE", description = "Encrypted hash value is required")
    private String hash;
    @NotNull(message = "Request id cannot be null")
    @NotEmpty(message = "Request id cannot be empty")
    @NotBlank(message = "Request id cannot be blank")
    private String requestId;
    private String email;
    private String otp;
    private String inputter;
    private String authorizer;
    private String potentialCustomerId;
    private String applicationDate;
    private String processingDate;
    private String title;
    private String initials;
    private String gender;
    private String placeOfBirth;
    private String motherMaidenName;
    private String education;
    private String loanOfficer;
    private String savingOfficer;
    private String recoveryOfficer;
    private String occupation;
    private String preferedLanguage;
    private String noOfChildren;
    private String sourceOfInfo;
    private String noOfDependants;
    private String idType;
    private String idNumber;
    private String idExpiryDate;
    private String otherId;
    private String landmark;
    private String livingSince;
    private String typeOfHouse;
    private String mortgage;
    private String fixedPhone;
    private String businessName;
    private String typeOfBusiness;
    private String businessRegistered;
    private String businessRegistrationNo;
    private String dateRegistered;
    private String economicSector;
    private String typeOfEstablishment;
    private String businessActivity;
    private String ageOfBusiness;
    private String timeInBusinessLocation;
    private String businessStreetNumber;
    private String businessCity;
    private String businessState;
    private String businessNeighbourhood;
    private String businessLandmark;
    private String businessPhone;
    private String spouseLastname;
    private String spouseFirstname;
    private String spouseId;
    private String spouseIdNumber;
    private String spouseRelationship;
    private String spouseMobileNumber;
    private String spouseOther;
    private String spouseCustomerNumber;
    private String personalReferenceName;
    private String personalReferenceRelation;
    private String personalReferencePhone;
    private String supplierName;
    private String supplierContactPerson;
    private String supplierPhone;
    private String referenceNumber;
    private String homeVerificationCompleted;
    private String collateralAmount;
    private String loanPurpose;
    private String loanAmountRequested;
    private String loanTerm;
    private String loanFrequency;
    private String employer;
    private String employmentPosition;
    private String employedSince;
    private String employerPostalAddress;
    private String employerPhysicalAddress;
    private String employerNeighbourhood;
    private String employerLandmark;
    private String employerTown;
    private String employerState;
    private String employerPhone;
    private String employementNumber;
    private String employerCounty;
    private String relationshipIndicator;
    private String officer;
    private String typeOfOfficer;
    private String blocked;
    private String insurable;
    private String taxId;
    private String mailingList;
    private String mailingListStatement;
    private String mailingListLetter;
    private String mailingListLabel;
    private String minimumBalanceAt55;
    private String minimumBalanceAt60;
    private String minimumBalanceAt65;
    private String minimumBalanceAt70;
    private String creditCheckDone;
    private String creditIndicator;
    private String consentToDisclosure;
    private String signatureDate;
    private String nominationForm;
    private String nominationBeneficiaryName;
    private String nominationAddress;
    private String nominationPhone;
    private String nominationRelationship;
    private String nominationAmount;
    private String nominationCustomerNumber;
    private String nominationTown;
    private String nominationCounty;
    private String nominationState;
    private String nominationPostalCode;
    private String industryClassification;
    private String loansWrittenOff;
    private String areaCode;
    private String nonQualifying;
    private String otherAccountNumber;
    private String otherAccountBankBranch;
    private String otherAccountSortCode;
    private String otherAccountDateOpened;
    private String smsNotification;
    private String smsNotificationMobile;
    private String smsNotificationHomePhone;
    private String smsNotificationWorkPhone;
    private String emailAddress;
    private String fax;
    private String primaryAccount;  
    
}
