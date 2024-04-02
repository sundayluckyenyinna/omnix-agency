/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.repository;


import com.accionmfb.omnix.agency.model.*;

import java.util.List;

/**
 *
 * @author bokon
 */
public interface FundsTransferRepository {

    Customer getCustomerUsingMobileNumber(String mobileNumber);

    UserActivity createUserActivity(UserActivity userActivity);

    Account getCustomerAccount(Customer customer, String accountNumber);
    List<Account> getAllCustomerAccounts(Customer customer);


    Account getCustomerAccount2(String accountNumber);

    FundsTransfer createFundsTransfer(FundsTransfer fundsTransfer);

    FundsTransfer createFundsTransferTest(FundsTransfer fundsTransfer);

    FundsTransfer updateFundsTransfer(FundsTransfer fundsTransfer);

    Account getAccountUsingAccountNumber(String accountNumber);

    Account getAccountUsingAccountNumber2(String accountNumber);


    AppUser getAppUserUsingUsername(String username);
}
