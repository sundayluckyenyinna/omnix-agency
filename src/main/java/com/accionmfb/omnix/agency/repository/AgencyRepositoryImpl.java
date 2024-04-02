/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.*;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author bokon
 */
@Repository
@Slf4j
@Transactional(transactionManager = "transactionManager")
public class AgencyRepositoryImpl implements AgencyRepository {

    @PersistenceContext(unitName = "corePersistenceUnit")
    EntityManager em;

    @Override
    public UserActivity createUserActivity(UserActivity userActivity) {
        log.info("em =====>>> {}", em);
        log.info("user activity =====>>> {}", userActivity);
        em.persist(userActivity);
        em.flush();
        return userActivity;
    }

    @Override
    public Customer getCustomerUsingCustomerNumber(String customerNumber) {
        TypedQuery<Customer> query = em.createQuery("SELECT t FROM Customer t WHERE t.customerNumber = :customerNumber", Customer.class)
                .setParameter("customerNumber", customerNumber);
        List<Customer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
    public Branch getBranchUsingBranchCode(String branchCode) {
        TypedQuery<Branch> query = em.createQuery("SELECT t FROM Branch t WHERE t.branchCode = :branchCode", Branch.class)
                .setParameter("branchCode", branchCode);
        List<Branch> record = query.getResultList();
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

    @Override
    public AccionAgent getAgentUsingTerminalId(String terminalId, String agentVendor) {
        TypedQuery<AccionAgent> query = em.createQuery("SELECT t FROM AccionAgent t WHERE t.terminalId = :terminalId AND t.agentVendor = :agentVendor", AccionAgent.class)
                .setParameter("terminalId", terminalId)
                .setParameter("agentVendor", agentVendor);
        List<AccionAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AccionAgent getAgentUsingPhoneNumber(String phoneNumber, String agentVendor) {
        TypedQuery<AccionAgent> query = em.createQuery("SELECT t FROM AccionAgent t WHERE t.agentMobile = :phoneNumber AND t.agentVendor = :agentVendor", AccionAgent.class)
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("agentVendor", agentVendor);
        List<AccionAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Biller getCableTVBillerUsingPackageName(String vendor, String biller, String packageName) {
        TypedQuery<Biller> query = em.createQuery("SELECT t FROM Biller t WHERE t.biller = :biller AND t.packageName = :packageName AND t.vendor = :vendor AND t.status = 'OK'", Biller.class)
                .setParameter("biller", biller)
                .setParameter("packageName", packageName)
                .setParameter("vendor", vendor);
        List<Biller> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public List<Biller> getElectricityBiller(String vendor, String biller) {
        TypedQuery<Biller> query = em.createQuery("SELECT t FROM Biller t WHERE t.biller = :biller AND t.vendor = :vendor AND t.status = 'OK'", Biller.class)
                .setParameter("biller", biller)
                .setParameter("vendor", vendor);
        List<Biller> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Banks getBankUsingBankCode(String bankCode) {
        TypedQuery<Banks> query = em.createQuery("SELECT t FROM Banks t WHERE t.bankCode = :bankCode", Banks.class)
                .setParameter("bankCode", bankCode);
        List<Banks> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public FundsTransfer getFundsTransferUsingTransRef(String transRef) {
        TypedQuery<FundsTransfer> query = em.createQuery("SELECT t FROM FundsTransfer t WHERE t.requestId = :transRef", FundsTransfer.class)
                .setParameter("transRef", transRef);
        List<FundsTransfer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public FundsTransfer updateFundsTransfer(FundsTransfer fundsTransfer) {
        em.merge(fundsTransfer);
        em.flush();
        log.info("updated successfully !!!!!!!!!");
        return fundsTransfer;
    }

    @Override
    public AccionAgent getAgentUsingAccountNumber(String accountNumber, String agentVendor) {
        TypedQuery<AccionAgent> query = em.createQuery("SELECT t FROM AccionAgent t WHERE t.agentAccountNumber = :accountNumber AND t.agentVendor = :agentVendor", AccionAgent.class)
                .setParameter("accountNumber", accountNumber)
                .setParameter("agentVendor", agentVendor);
        List<AccionAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public AccionAgent createAccionAgent(AccionAgent accionAgent) {
        em.persist(accionAgent);
        em.flush();
        return accionAgent;
    }

    @Override
    public AccionAgent updateAccionAgent(AccionAgent accionAgent) {
        em.merge(accionAgent);
        em.flush();
        return accionAgent;
    }

    @Override
    public AccionAgent getAgentUsingId(Long id) {
        TypedQuery<AccionAgent> query = em.createQuery("SELECT t FROM AccionAgent t WHERE t.id = :id", AccionAgent.class)
                .setParameter("id", id);
        List<AccionAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public Account getRecordUsingRequestId(String requestId) {
        TypedQuery<Account> query = em.createQuery("SELECT t FROM Account t WHERE t.requestId = :requestId", Account.class)
                .setParameter("requestId", requestId);
        List<Account> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
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
    public List<AccionAgent> allAgents() {
        TypedQuery<AccionAgent> query = em.createQuery("SELECT t FROM AccionAgent t ", AccionAgent.class);
        List<AccionAgent> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record;
    }

    @Override
    public Biller getCableTVBillerUsingAmount(String biller, String vendor, String amount) {
        TypedQuery<Biller> query = em.createQuery("SELECT t FROM Biller t WHERE t.biller = :biller AND t.vendor = :vendor AND t.status = 'OK' AND t.amount = :amount", Biller.class)
                .setParameter("biller", biller)
                .setParameter("vendor", vendor)
                .setParameter("amount", amount);
        List<Biller> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

    @Override
    public FundsTransfer getFundsTransferUsingRequestId(String requestId) {
        TypedQuery<FundsTransfer> query = em.createQuery("SELECT t FROM FundsTransfer t WHERE t.requestId = :requestId", FundsTransfer.class)
                .setParameter("requestId", requestId);
        List<FundsTransfer> record = query.getResultList();
        if (record.isEmpty()) {
            return null;
        }
        return record.get(0);
    }

   @Override
    public MerchantTranLog createMerchantTranLog(MerchantTranLog merchantTranLog) {
        em.persist(merchantTranLog);
        em.flush();
        return merchantTranLog;
    }

    @Override
    public MerchantTranLog updateMerchantTranLog(MerchantTranLog merchantTranLog) {
        em.merge(merchantTranLog);
        em.flush();
        return merchantTranLog;
    }  
    
    @Override
    public FundsTransfer createFundsTransfer(FundsTransfer fundsTransfer) {
        em.persist(fundsTransfer);
        em.flush();
        return fundsTransfer;
    }

}
