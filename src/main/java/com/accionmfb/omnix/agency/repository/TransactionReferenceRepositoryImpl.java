package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.TransactionReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
@Transactional(transactionManager = "transactionManager")
@Slf4j
public class TransactionReferenceRepositoryImpl implements TransactionReferenceRepository {

    @PersistenceContext(unitName = "corePersistenceUnit")
    @Qualifier("omnixDatasource")
    EntityManager em;

    @Override
    public TransactionReference getByReferenceNumber(String referenceNumber) {
        TypedQuery<TransactionReference> query = em.createQuery("SELECT t FROM TransactionReference t WHERE t.referenceNumber = :referenceNumber", TransactionReference.class)
                .setParameter("referenceNumber", referenceNumber);
        List<TransactionReference> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public TransactionReference createTransactionReference(TransactionReference transactionReference) {
        log.info("Trying to save transaction reference in the db");
        em.persist(transactionReference);
        em.flush();
        log.info("transaction reference --------->>>> {}", transactionReference);
        return transactionReference;
    }
}