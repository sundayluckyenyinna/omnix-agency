package com.accionmfb.omnix.agency.payload;

import com.accionmfb.omnix.agency.model.CashoutStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatusReportResponse {
    private String responseCode;
    private String responseMessage;
    private String status;
    private List<CashoutStatus> cashoutStatuses;
}
