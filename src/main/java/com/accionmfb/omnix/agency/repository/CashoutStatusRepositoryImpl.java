package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.CashoutStatus;
import com.accionmfb.omnix.agency.model.AgentTranLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(transactionManager = "transactionManager")
@Slf4j
public class CashoutStatusRepositoryImpl implements CashoutStatusRepository {

    @PersistenceContext(unitName = "corePersistenceUnit")
    @Qualifier("omnixDatasource")
    EntityManager em;

    @Override
    public CashoutStatus createStatusReport(CashoutStatus cashoutStatus) {
        em.persist(cashoutStatus);
        em.flush();
        return cashoutStatus;
    }

    @Override
    public List<CashoutStatus> getReport(String tranRef) {
        TypedQuery<CashoutStatus> query = em.createQuery("SELECT t FROM CashoutStatus t WHERE t.tranRef = :tranRef", CashoutStatus.class)
                .setParameter("tranRef", tranRef);

        List<CashoutStatus> record = query.getResultList();
        if (record.isEmpty()) {
            log.info("No transaction found");
            return null;
        }
        return record;
    }
}
