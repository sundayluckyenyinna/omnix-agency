package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.BVNIVR;
import com.accionmfb.omnix.agency.model.ivr.T24Accounts;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * @author Olaoye on 31/10/2023
 */
@Repository
@Transactional(transactionManager = "ivrCoreTransactionManager")
public class BVNRepositoryImpl implements BVNRepository {

  @PersistenceContext(unitName = "ivrCorePersistenceUnit")
  EntityManager em;

  @Override
  public BVNIVR getBVN(String bvn) {
    TypedQuery<BVNIVR> query = em.createQuery("SELECT t FROM BVNIVR t WHERE t.customerBvn = :bvn AND t.status = 'Valid'", BVNIVR.class)
            .setParameter("bvn", bvn);
    List<BVNIVR> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public BVNIVR createBVN(BVNIVR bvn) {
    em.persist(bvn);
    em.flush();
    return bvn;
  }

  @Override
  public T24Accounts getT24AccountUsingBVN(String bvn) {
    TypedQuery<T24Accounts> query = em.createQuery("SELECT t FROM T24Accounts t WHERE REPLACE(t.bvn, '''','') = :bvn", T24Accounts.class)
            .setParameter("bvn", bvn);
    List<T24Accounts> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public Object getExistingBVN(String bvn) {
    TypedQuery<BVNIVR> query = em.createQuery("SELECT t FROM BVNIVR t WHERE t.customerBvn = :bvn", BVNIVR.class)
            .setParameter("bvn", bvn);
    List<BVNIVR> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public BVNIVR updateBVN(BVNIVR bvn) {
    em.merge(bvn);
    em.flush();
    return bvn;
  }

  @Override
  public int getTodaysBVNCount(Date today) {
    TypedQuery<BVNIVR> query = em.createQuery("SELECT t FROM BVNIVR t WHERE CAST(t.createdAt as date) = :today", BVNIVR.class)
            .setParameter("today", today);
    List<BVNIVR> record = query.getResultList();
    if (record.isEmpty()) {
      return record.size();
    }
    return record.size();
  }

  @Override
  public BVNIVR getPendingBVN(String bvn) {
    TypedQuery<BVNIVR> query = em.createQuery("SELECT t FROM BVNIVR t WHERE t.customerBvn = :bvn AND t.status = 'Pending'", BVNIVR.class)
            .setParameter("bvn", bvn);
    List<BVNIVR> record = query.getResultList();
    if (record.isEmpty()) {
      return null;
    }
    return record.get(0);
  }

  @Override
  public List<BVNIVR> getAllRecords() {
    return em.createQuery("select t from BVNIVR t", BVNIVR.class).getResultList();
  }
}
