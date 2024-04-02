/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.AccionAgent;
import com.accionmfb.omnix.agency.model.Account;
import com.accionmfb.omnix.agency.model.AppUser;
import com.accionmfb.omnix.agency.model.Banks;
import com.accionmfb.omnix.agency.model.Biller;
import com.accionmfb.omnix.agency.model.Branch;
import com.accionmfb.omnix.agency.model.Customer;
import com.accionmfb.omnix.agency.model.FundsTransfer;
import com.accionmfb.omnix.agency.model.MerchantTranLog;
import com.accionmfb.omnix.agency.model.UserActivity;
import java.util.List;

/**
 *
 * @author bokon
 */
public interface AgencyRepository {

    AppUser getAppUserUsingUsername(String username);

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    Customer getCustomerUsingCustomerNumber(String customerNumber);

    UserActivity createUserActivity(UserActivity userActivity);

    Branch getBranchUsingBranchCode(String branchCode);

    AccionAgent getAgentUsingTerminalId(String terminalId, String agentVendor);

    AccionAgent getAgentUsingPhoneNumber(String phoneNumber, String agentVendor);

    AccionAgent getAgentUsingAccountNumber(String accountNumber, String agentVendor);

    AccionAgent getAgentUsingId(Long id);

    Biller getCableTVBillerUsingPackageName(String biller, String vendor, String packageName);

    Biller getCableTVBillerUsingAmount(String biller, String vendor, String amount);

    List<Biller> getElectricityBiller(String vendor, String biller);

    Banks getBankUsingBankCode(String bankCode);

    FundsTransfer getFundsTransferUsingTransRef(String transRef);

    FundsTransfer updateFundsTransfer(FundsTransfer fundsTransfer);

    AccionAgent createAccionAgent(AccionAgent accionAgent);

    AccionAgent updateAccionAgent(AccionAgent accionAgent);

    Account getRecordUsingRequestId(String requestId);

    FundsTransfer getFundsTransferUsingRequestId(String requestId);

    Account getAccountUsingAccountNumber(String accountNumber);

    List<AccionAgent> allAgents();
    
    public MerchantTranLog createMerchantTranLog(MerchantTranLog merchantTranLog);
    public MerchantTranLog updateMerchantTranLog(MerchantTranLog merchantTranLog);
    public FundsTransfer createFundsTransfer(FundsTransfer fundsTransfer);
}
