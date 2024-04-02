/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.Account;
import com.accionmfb.omnix.agency.model.UserActivity;

/**
 *
 * @author bokon
 */
public interface AccountRepository {

    UserActivity createUserActivity(UserActivity userActivity);

    Account getAccountUsingAccountNumber(String accountNumber);

    Account getAccountUsingAccountNumber2(String accountNumber);
}
