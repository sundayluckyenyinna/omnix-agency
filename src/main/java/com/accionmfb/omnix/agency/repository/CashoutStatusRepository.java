package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.CashoutStatus;
import com.accionmfb.omnix.agency.model.AgentTranLog;

import java.util.List;

public interface CashoutStatusRepository {
    CashoutStatus createStatusReport(CashoutStatus cashoutStatus);
    List<CashoutStatus> getReport (String tranRef);

}
