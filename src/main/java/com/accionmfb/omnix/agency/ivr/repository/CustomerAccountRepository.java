package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.T24Accounts;
import com.accionmfb.omnix.agency.model.ivr.T24Customers;

import java.util.List;

/**
 * @author Olaoye on 30/10/2023
 */
public interface CustomerAccountRepository {

    T24Customers updateT24Customer(T24Customers t24Customer);

    T24Accounts updateT24Account(T24Accounts t24Account);

    T24Customers getCustomerDetails(String mobileNumber);

    T24Accounts getT24AccountUsingBVNAndProductCategory(String bvn, String productCategory);

    T24Accounts getT24AccountUsingPhoneNumberAndProductCategory(String phoneNumber, String productCategory);

    T24Accounts getT24AccountUsingAccountNumber(String accountNumber);

    T24Accounts createT24Accounts(T24Accounts t24Account);

    T24Customers createCustomer(T24Customers customer);

    T24Accounts getT24AccountUsingBVN(String bvn);

    List<T24Accounts> getT24AccountsUsingPhoneNumber(String mobileNumber);

    T24Accounts getT24AccountUsingPhoneNumber(String mobileNumber);

    T24Customers getT24CustomerUsingCustomerNumber(String customerNumber);

    T24Customers getT24CustomerUsingPhoneNumber(String phoneNumber);

    List<T24Customers> getAllRecords();
}
