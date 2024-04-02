package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.AgentTranLog;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "transactionManager")
@Slf4j
public class NotificationHistoryRepoImpl implements NotificationHistoryRepo {

    @PersistenceContext(unitName = "corePersistenceUnit")
    EntityManager em;

    @Override
    public AgentTranLog getTransactionByTranRef(String tranRef) {
        TypedQuery<AgentTranLog> query = em.createQuery("SELECT t FROM NotificationHistory t WHERE t.tranRef = :tranRef", AgentTranLog.class)
                .setParameter("tranRef", tranRef);
        List<AgentTranLog> record = query.getResultList();
        if (record.isEmpty()) {
            log.info("No transaction found");
            return null;
        }
        return record.get(0);
    }

    @Override
    public AgentTranLog createTransaction(AgentTranLog notificationHistory) {
        em.persist(notificationHistory);
        em.flush();
        return notificationHistory;
    }

    @Override
    public List<AgentTranLog> findApprovedNotifications() {
        TypedQuery<AgentTranLog> query = em.createQuery("SELECT t FROM NotificationHistory t WHERE t.status = :status", AgentTranLog.class)
                .setParameter("status", "Approved");
        List<AgentTranLog> record = query.getResultList();
        if (record.isEmpty()) {
            log.info("No pending transaction found");
        }
        return record;
    }


    @Override
    public void updateTransaction(AgentTranLog notificationHistory) {
        em.merge(notificationHistory);
        em.flush();
    }

    @Override
    public List<AgentTranLog> findAllNotificationHistory() {
        TypedQuery<AgentTranLog> query = em.createQuery("SELECT t FROM NotificationHistory t", AgentTranLog.class);
        List<AgentTranLog> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public List<AgentTranLog> findPendingTransactions(String status) {
        TypedQuery<AgentTranLog> query = em.createQuery("SELECT t FROM NotificationHistory t WHERE t.status = :status", AgentTranLog.class)
                .setParameter("status", status);
        List<AgentTranLog> record = query.getResultList();
        if (record.isEmpty()) {
            log.info("No pending transaction found");
        }
        return record;
    }

    @Override
    public AgentTranLog findNotificationByReference(String reference) {
        TypedQuery<AgentTranLog> query = em.createQuery("SELECT t FROM NotificationHistory t WHERE t.tranRef = :tranRef", AgentTranLog.class)
                .setParameter("tranRef", reference);
        List<AgentTranLog> record = query.getResultList();
        if (record == null || record.isEmpty()) {
            log.info("No transaction found");
            return null;
        }
        else {
            return record.get(0);
        }
    }
}