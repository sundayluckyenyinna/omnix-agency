package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.T24Accounts;
import com.accionmfb.omnix.agency.model.ivr.T24Customers;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author Olaoye on 30/10/2023
 */
@Repository
@Transactional(transactionManager = "customerAccountTransactionManager")
public class CustomerAccountRepositoryImpl implements CustomerAccountRepository {

  @PersistenceContext(unitName = "customerAccountPersistenceUnit")
  EntityManager em;

  @Override
  public T24Accounts createT24Accounts(T24Accounts t24Account) {
    em.persist(t24Account);
    em.flush();
    return t24Account;
  }

  @Override
  public T24Customers createCustomer(T24Customers customer) {
    em.persist(customer);
    em.flush();
    return customer;
  }

  @Override
  public T24Customers getCustomerDetails(String mobileNumber) {
    TypedQuery<T24Customers> query = em.createQuery("SELECT t FROM T24Customers t WHERE t.mobileNumber = :mobileNumber", T24Customers.class)
            .setParameter("mobileNumber", mobileNumber);
    List<T24Customers> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public List<T24Accounts> getT24AccountsUsingPhoneNumber(String mobileNumber) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.mobileNumber = :mobileNumber", T24Accounts.class)
            .setParameter("mobileNumber", mobileNumber);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record;
  }

  @Override
  public T24Accounts getT24AccountUsingPhoneNumber(String mobileNumber) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.mobileNumber = :mobileNumber", T24Accounts.class)
            .setParameter("mobileNumber", mobileNumber);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Accounts getT24AccountUsingPhoneNumberAndProductCategory(String mobileNumber, String productCategory) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.mobileNumber = :mobileNumber AND t.accountDescription = :productCategory", T24Accounts.class)
            .setParameter("mobileNumber", mobileNumber)
            .setParameter("productCategory", productCategory);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Accounts getT24AccountUsingAccountNumber(String accountNumber) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.t24AccountNumber = :accountNumber OR t.accountNumber = :accountNumber", T24Accounts.class)
            .setParameter("accountNumber", accountNumber);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Accounts getT24AccountUsingBVNAndProductCategory(String bvn, String productCategory) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.bvn = :bvn AND t.accountDescription = :productCategory", T24Accounts.class)
            .setParameter("bvn", bvn)
            .setParameter("productCategory", productCategory);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Accounts getT24AccountUsingBVN(String bvn) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE t.bvn = :bvn", T24Accounts.class)
            .setParameter("bvn", bvn);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Customers getT24CustomerUsingCustomerNumber(String customerNumber) {
    TypedQuery<T24Customers> query = em.createQuery("SELECT t FROM T24Customers t WHERE t.customerId = :customerNumber", T24Customers.class)
            .setParameter("customerNumber", customerNumber);
    List<T24Customers> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public T24Customers getT24CustomerUsingPhoneNumber(String mobileNumber) {
    TypedQuery<T24Customers> query = em.createQuery("SELECT t FROM T24Customers t WHERE t.mobileNumber = :mobileNumber", T24Customers.class)
            .setParameter("mobileNumber", mobileNumber);
    List<T24Customers> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public List<T24Customers> getAllRecords() {
    return em.createQuery("select c from T24Customers c", T24Customers.class).getResultList();
  }

  @Override
  public T24Customers updateT24Customer(T24Customers t24Customer) {
    em.merge(t24Customer);
    em.flush();
    return t24Customer;
  }

  @Override
  public T24Accounts updateT24Account(T24Accounts t24Account) {
    em.merge(t24Account);
    em.flush();
    return t24Account;
  }
}
