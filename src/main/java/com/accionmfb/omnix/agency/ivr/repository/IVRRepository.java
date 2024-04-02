package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.IVRAccount;
import com.accionmfb.omnix.agency.model.ivr.IVRCustomer;

import java.util.List;

/**
 * @author user on 30/10/2023
 */
public interface IVRRepository {

    IVRAccount createAccount(IVRAccount account);

    IVRAccount updateAccount(IVRAccount account);

    IVRCustomer createCustomer(IVRCustomer customer);

    IVRCustomer updateCustomer(IVRCustomer customer);

    IVRCustomer getCustomerUsingAccountNumber(String accountNumber);

    IVRCustomer getT24CustomerUsingPhoneNumber(String mobileNumber);
    List<IVRCustomer> getAllIVRCustomer();

    IVRCustomer getCustomerUsingPhoneNumber(String mobileNumber);

    IVRAccount getAccountDetails(String mobileNumber);
}
