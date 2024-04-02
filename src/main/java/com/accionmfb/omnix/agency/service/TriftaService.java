/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.AccountNumberPayload;
import com.accionmfb.omnix.agency.payload.AccountOpeningRequestPayload;
import com.accionmfb.omnix.agency.payload.AccountStatementRequestPayload;
import com.accionmfb.omnix.agency.payload.AirtimeOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.BillerRequestPayload;
import com.accionmfb.omnix.agency.payload.CableTVPayload;
import com.accionmfb.omnix.agency.payload.CableTVRequestPayload;
import com.accionmfb.omnix.agency.payload.DataOtherRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityBillerRequestPayload;
import com.accionmfb.omnix.agency.payload.ElectricityPayload;
import com.accionmfb.omnix.agency.payload.ElectricityRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.IndivCustomerWithoutBvnRequestPayload;
import com.accionmfb.omnix.agency.payload.LocalTransferPayload;
import com.accionmfb.omnix.agency.payload.MobileNumberRequestPayload;
import com.accionmfb.omnix.agency.payload.NIPNameEnquiryPayload;
import com.accionmfb.omnix.agency.payload.NIPTransferPayload;
import com.accionmfb.omnix.agency.payload.SmartcardRequestPayload;
import com.accionmfb.omnix.agency.payload.TransactionPayload;

/**
 *
 * @author bokon
 */
public interface TriftaService {

    Object checkIfSameRequestId(String requestId);

    boolean validateMobileNumberPayload(String token, MobileNumberRequestPayload requestPayload);

    String processCustomerDetails(String token, MobileNumberRequestPayload requestPayload);

    boolean validateAccountNumberPayload(String token, AccountNumberPayload requestPayload);

    String processAccountBalance(String token, AccountNumberPayload requestPayload);

    boolean validateAccountStatementPayload(String token, AccountStatementRequestPayload requestPayload);

    String getAccountStatement(String token, AccountStatementRequestPayload requestPayload);

    String getAccountDetails(String token, AccountNumberPayload requestPayload);

    boolean validateAccountOpeningRequestPayload(String token, AccountOpeningRequestPayload requestPayload);

    String processAccountOpening(String token, AccountOpeningRequestPayload requestPayload);

    boolean validateCreateIndividualCustomerWithBvnPayload(String token, IndivCustomerWithBvnRequestPayload requestPayload);

    String processCreateIndividaulCustomerWithBvn(String token, IndivCustomerWithBvnRequestPayload requestPayload);

    boolean validateCreateIndividualCustomerWithoutBvnPayload(String token, IndivCustomerWithoutBvnRequestPayload requestPayload);

    String processCreateIndividualCustomerWithoutBvn(String token, IndivCustomerWithoutBvnRequestPayload requestPayload);

    boolean validateLocalFundsTransferPayload(String token, LocalTransferPayload requestPayload);

    String processLocalFundsTransfer(String token, LocalTransferPayload requestPayload);

    boolean validateTransactionPayload(String token, TransactionPayload requestPayload);
    
    String processTransactionQuery(String token, TransactionPayload requestPayload);

    String processLocalFundsTransferReversal(String token, TransactionPayload requestPayload);

    boolean validateNIPTransferPayload(String token, NIPTransferPayload requestPayload);

    String processNIPTransfer(String token, NIPTransferPayload requestPayload);

    boolean validateNIPNameEnquiryPayload(String token, NIPNameEnquiryPayload requestPayload);

    String processNIPNameEnquiry(String token, NIPNameEnquiryPayload requestPayload);

    boolean validateAirtimeOthersPayload(String token, AirtimeOtherRequestPayload requestPayload);

    String processAirtime(String token, AirtimeOtherRequestPayload requestPayload);

    boolean validateDataOthersPayload(String token, DataOtherRequestPayload requestPayload);

    String processData(String token, DataOtherRequestPayload requestPayload);

    boolean validateCableTVPayload(String token, CableTVRequestPayload requestPayload);

    String processCableTVSubscription(String token, CableTVRequestPayload requestPayload);

    boolean validateCableTVPayload(String token, CableTVPayload requestPayload);

    String getCableTVDetails(String token, CableTVPayload requestPayload);

    boolean validateBillerPayload(String token, BillerRequestPayload requestPayload);

    String getCableTVBiller(String token, BillerRequestPayload requestPayload);

    boolean validateSmartcardPayload(String token, SmartcardRequestPayload requestPayload);

    String getCableTVSmartcardDetails(String token, SmartcardRequestPayload requestPayload);
    
    boolean validateBillerPayload(String token, ElectricityBillerRequestPayload requestPayload) ;

    boolean validateElectricityPayload(String token, ElectricityRequestPayload requestPayload);

    String processElectricityPayment(String token, ElectricityRequestPayload requestPayload);

    boolean validateElectricityPayload(String token, ElectricityPayload requestPayload);

    String getElectricityDetails(String token, ElectricityPayload requestPayload);

    String getElectricityBiller(String token, ElectricityBillerRequestPayload requestPayload);

    String getElectricitySmartcardDetails(String token, SmartcardRequestPayload requestPayload);
}
