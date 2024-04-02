package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.IVRAccount;
import com.accionmfb.omnix.agency.model.ivr.IVRCustomer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author user on 30/10/2023
 */

@Repository
@Transactional(transactionManager = "customerAccountTransactionManager")
public class IVRRepositoryImpl implements IVRRepository{

  @PersistenceContext(unitName = "customerAccountPersistenceUnit")
  EntityManager em;

  @Override
  public IVRAccount createAccount(IVRAccount account) {
    em.persist(account);
    em.flush();
    return account;
  }

  @Override
  public IVRAccount updateAccount(IVRAccount account) {
    em.merge(account);
    em.flush();
    return account;
  }

  @Override
  public IVRCustomer createCustomer(IVRCustomer customer) {
    em.persist(customer);
    em.flush();
    return customer;
  }

  @Override
  public IVRCustomer updateCustomer(IVRCustomer customer) {
    em.merge(customer);
    em.flush();
    return customer;
  }

  @Override
  public IVRCustomer getCustomerUsingAccountNumber(String accountNumber) {
    TypedQuery<IVRCustomer> query = em.createQuery("SELECT t FROM IVRCustomer t WHERE t.accountNumber = :accountNumber", IVRCustomer.class)
            .setParameter("accountNumber", accountNumber);
    List<IVRCustomer> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public IVRCustomer getT24CustomerUsingPhoneNumber(String mobileNumber) {
    TypedQuery<IVRCustomer> query = em.createQuery("SELECT t FROM IVRCustomer t WHERE t.mobileNumber = :mobileNumber", IVRCustomer.class)
            .setParameter("mobileNumber", mobileNumber);
    List<IVRCustomer> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public List<IVRCustomer> getAllIVRCustomer() {
    return em.createQuery("select t from IVRCustomer t", IVRCustomer.class).getResultList();
  }

  @Override
  public IVRCustomer getCustomerUsingPhoneNumber(String mobileNumber) {
    TypedQuery<IVRCustomer> query = em.createQuery("SELECT t FROM IVRCustomer t WHERE t.mobileNumber = :mobileNumber AND t.accountNumber = '' AND t.status = 'Pending' ", IVRCustomer.class)
            .setParameter("mobileNumber", mobileNumber);
    List<IVRCustomer> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public IVRAccount getAccountDetails(String mobileNumber) {
    TypedQuery<IVRAccount> query = em.createQuery("SELECT t FROM IVRAccount t WHERE t.mobileNumber = :mobileNumber", IVRAccount.class)
            .setParameter("mobileNumber", mobileNumber);
    List<IVRAccount> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

}
