
package com.accionmfb.omnix.agency.module.agency3Line.security;


import com.accionmfb.omnibus.payload.GenericPayload;
import com.accionmfb.omnibus.payload.ValidationPayload;


public interface AesService {
    
    public String encryptString(String textToEncrypt, String encryptionKey);
    public String decryptString(String textToDecrypt, String encryptionKey);
    
     public String encryptFlutterString(String strToEncrypt, String secret) ;
     public String decryptFlutterString(final String textToDecrypt, final String encryptionKey);
     
     public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
