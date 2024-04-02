/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 *
 * @author bokon
 */
@Repository
@Transactional(transactionManager = "transactionManager")
@Slf4j
public class FundsTransferRepositoryImpl implements FundsTransferRepository {

    @PersistenceContext(unitName = "corePersistenceUnit")
    @Qualifier("omnixDatasource")
    EntityManager em;
    @Value("${omnix.wallet.central.mobile}")
    private String walletCentralMobile;
    @Value("${omnix.wallet.central.customernumber}")
    private String walletCentralCustomerNumber;

    @Override
    public UserActivity createUserActivity(UserActivity userActivity) {
        em.persist(userActivity);
        em.flush();
        return userActivity;
    }


    @Override
    public Customer getCustomerUsingMobileNumber(String mobileNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.mobileNumber = :mobileNumber", Customer.class)
                .setParameter("mobileNumber", mobileNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Account> getAllCustomerAccounts(Customer customer) {
        TypedQuery<Account> query = em.createQuery("SELECT a FROM Account a WHERE a.customer = :customer", Account.class)
                .setParameter("customer", customer);
        return query.getResultList();
    }


    @Override
    public Account getCustomerAccount(Customer customer, String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.customer = :customer AND t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("customer", customer)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getCustomerAccount2(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE  t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public FundsTransfer createFundsTransfer(FundsTransfer fundsTransfer) {
        em.persist(fundsTransfer);
        em.flush();
        return fundsTransfer;
    }

    @Override
    public FundsTransfer createFundsTransferTest(FundsTransfer fundsTransfer) {
        log.info("before setting ========>>>>>>>> {}", fundsTransfer);

        Customer managedCustomer = em.find(Customer.class, fundsTransfer.getCustomer().getId());
        log.info("before setting ========>>>>>>>> {}", fundsTransfer);
        log.info("managed customer ------->>>>>>> {}", managedCustomer);
        fundsTransfer.setCustomer(managedCustomer);
        log.info("after setting ========>>>>>>>> {}", fundsTransfer);
        em.persist(fundsTransfer);
        em.flush();
        return fundsTransfer;
    }


    @Override
    public FundsTransfer updateFundsTransfer(FundsTransfer fundsTransfer) {
        em.merge(fundsTransfer);
        em.flush();
        return fundsTransfer;
    }

    @Override
    public Account getAccountUsingAccountNumber(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.accountNumber = :accountNumber OR t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getAccountUsingAccountNumber2(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AppUser getAppUserUsingUsername(String username) {
        TypedQuery<AppUser> query = em.createQuery("SELECT t FROM AppUser t WHERE t.username = :username", AppUser.class)
                .setParameter("username", username);
        List<AppUser> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }
}
