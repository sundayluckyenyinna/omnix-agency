package com.accionmfb.omnix.agency.module.agency3Line.security;

import com.accionmfb.omnibus.payload.GenericPayload;
import com.accionmfb.omnibus.payload.ValidationPayload;
import org.springframework.stereotype.Service;

@Service
public class PgpServiceImpl implements PgpService {
    @Override
    public boolean generateKeyPairRSA(String userId, String privateKeyPassword, String publicKeyFileName, String privateKeyFileName) {
        return false;
    }

    @Override
    public String encryptString(String stringToEncrypt, String recipientPublicKeyFile) {
        return null;
    }

    @Override
    public String decryptString(String stringToDecrypt, String myPrivateKeyFile, String myPrivateKeyPassword) {
        return null;
    }

    @Override
    public ValidationPayload validateRequest(GenericPayload genericRequestPayload) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println("PgpServiceImpl is running as a standalone application.");
    }
}
