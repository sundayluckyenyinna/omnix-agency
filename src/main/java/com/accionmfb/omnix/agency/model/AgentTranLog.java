package com.accionmfb.omnix.agency.model;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "agent_tran_log")
public class AgentTranLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mobileNumber;
    private String debitAccount;
    private String creditAccount;
    private String amount;
    private String narration;
    private String transType;
    private String branchCode;
    private String inputter;
    private String authorizer;
    private String noOfAuthorizer;
    private String requestId;
    private String status;
    private LocalDate date;
    private String accountNumber;
    private String requestBy;
    private String agentAmount;
    private String accionFee;
    private String channel;
    private String terminalId;
    private String remainingLimit;
    private String tranRef;
    private String settled = "false";
    private String message;
}
