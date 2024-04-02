package com.accionmfb.omnix.agency.ivr.repository;

import com.accionmfb.omnix.agency.model.ivr.BVNIVR;
import com.accionmfb.omnix.agency.model.ivr.T24Accounts;

import java.util.Date;
import java.util.List;

/**
 * @author user on 31/10/2023
 */
public interface BVNRepository {

    BVNIVR getBVN(String bvn);

    BVNIVR createBVN(BVNIVR bvn);

    BVNIVR updateBVN(BVNIVR bvn);

    T24Accounts getT24AccountUsingBVN(String bvn);

    Object getExistingBVN(String bvn);

    int getTodaysBVNCount(Date today);

    BVNIVR getPendingBVN(String bvn);

    List<BVNIVR> getAllRecords();
}
