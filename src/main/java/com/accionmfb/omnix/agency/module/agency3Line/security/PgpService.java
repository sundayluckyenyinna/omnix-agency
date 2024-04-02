package com.accionmfb.omnix.agency.module.agency3Line.security;

import com.accionmfb.omnibus.payload.GenericPayload;
import com.accionmfb.omnibus.payload.ValidationPayload;

public interface  PgpService {
    public boolean generateKeyPairRSA(String userId, String privateKeyPassword, String publicKeyFileName, String privateKeyFileName);
    public String encryptString(String stringToEncrypt, String recipientPublicKeyFile);
    public String decryptString(String stringToDecrypt, String myPrivateKeyFile, String myPrivateKeyPassword);
    
    public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
