
package com.accionmfb.omnix.agency.service;

import com.accionmfb.omnix.agency.payload.OpayPaymentRequestPayload;

/**
 *
 * @author dofoleta
 */
public interface OpayService {
    
    public String opayPaymentNotification(OpayPaymentRequestPayload requestPayload);
    
}
