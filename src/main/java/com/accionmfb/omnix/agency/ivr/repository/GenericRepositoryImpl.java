package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.ThirdPartyVendors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author user on 31/10/2023
 */
@Repository
@Transactional(transactionManager = "ivrCoreTransactionManager")
public class GenericRepositoryImpl implements GenericRepository {

  @PersistenceContext(unitName = "corePersistenceUnit")
  EntityManager em;
  private static final Logger LOG = Logger.getLogger(GenericRepositoryImpl.class.getName());


  @Override
  public ThirdPartyVendors get3rdPartyDetailsWithIPAddress(String ipAddress) {
    TypedQuery<ThirdPartyVendors> query = em.createQuery("SELECT p FROM ThirdPartyVendors p WHERE p.vendorIPAddress = :ipAddress", ThirdPartyVendors.class)
            .setParameter("ipAddress", ipAddress);
    List<ThirdPartyVendors> _3rdParty = query.getResultList();
    if (_3rdParty.isEmpty()) {
      return null;
    }
    return _3rdParty.get(0);
  }

  @Override
  public String getBranchNameFromCode(String branchCode) {
    TypedQuery<String> query = em.createQuery("SELECT b.branchName FROM Branch b WHERE b.branchCode = :branchCode", String.class)
            .setParameter("branchCode", branchCode);
    List<String> selectedParam = query.getResultList();
    if (selectedParam.isEmpty()) {
      return null;
    }
    return selectedParam.get(0);
  }

  @Override
  public String getProductCodeWithProductId(String productCode) {
    TypedQuery<String> query = em.createQuery("SELECT p.productDesc FROM Product p WHERE p.productCode = :productCode", String.class)
            .setParameter("productCode", productCode);
    List<String> product = query.getResultList();
    if (product.isEmpty()) {
      return null;
    }
    return product.get(0);
  }
}
