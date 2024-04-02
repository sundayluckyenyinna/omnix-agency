package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.AgentTranLog;
import java.util.List;

public interface NotificationHistoryRepo {

    AgentTranLog getTransactionByTranRef(String tranRef);

    AgentTranLog createTransaction(AgentTranLog notificationHistory);

    List<AgentTranLog> findApprovedNotifications();

    void updateTransaction(AgentTranLog notificationHistory);

    List<AgentTranLog> findAllNotificationHistory();

    public List<AgentTranLog> findPendingTransactions(String status);

    AgentTranLog findNotificationByReference(String reference);
}
