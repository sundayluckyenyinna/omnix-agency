package com.accionmfb.omnix.agency.payload;

import com.accionmfb.omnix.agency.dto.Payload;
import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class OpayPaymentRequestPayload {

    Payload payloadObject;
    private String sha512;
    private String type;

    private String transactionType;
}
