package com.accionmfb.omnix.agency.service.utils;

import com.accionmfb.omnix.agency.service.GenericService;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.accionmfb.omnix.agency.constant.ResponseCodes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 *
 * @author dofoleta
 */
@Component
public class NipUtil {

    @Autowired
    Environment env;

    @Autowired
    Gson gson;

    @Autowired
    GenericService genericService;

    @Autowired
    MessageSource messageSource;

    private static final String USER_SECRET_KEY = "TYqZHpdjamZZs3XtgrDorw==";
    private  String BASE_URL = "";
    private  String USERNAME = "";
    private  String PASSWORD = "";

    //Constant Headers
    private static final String SIGNATURE_METHOD = "SHA512";
    private  String AUTHORIZATION = "";

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private static String NONCE = "";
    private static String SIGNATURE = "";
    private static String TIMESTAMP = "";

    private void init() {
        try {

            BASE_URL = env.getProperty("nip.base.url");
            USERNAME = env.getProperty("nip.ws.user");
            PASSWORD = env.getProperty("nip.ws.pass");
            AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
            NONCE = String.valueOf(Math.random());
            TIMESTAMP = TIMESTAMP_FORMAT.format(new Date());

            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, USERNAME, USER_SECRET_KEY);
            SIGNATURE = SignaturePlain; //this must be computation of SignaturePlain using SHA512
            SIGNATURE = hash(SignaturePlain, SIGNATURE_METHOD);
        } catch (Exception e) {
        }
    }

    public static String hash(String plainText, String algorithm) {
        StringBuilder hexString = new StringBuilder();
        if (algorithm.equals("SHA512")) {
            algorithm = "SHA-512";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase();
    }

    public NESingleResponse doNipNameEnquiry(NESingleRequest request) {
        init();
        String res;
        NESingleResponse oNESingleResponse = new NESingleResponse();

        try {
            String url = BASE_URL + "/nip/nameEnquiry";
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(url)
                    .header("Authorization", AUTHORIZATION)
                    .header("SignatureMethod", SIGNATURE_METHOD)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(gson.toJson(request))
                    .asString();

            if (httpResponse.getStatus() == 200 || httpResponse.getStatus() == 202) {
                res = httpResponse.getBody();
                System.out.println(res);
                oNESingleResponse = gson.fromJson(res, NESingleResponse.class);
                return oNESingleResponse;
            }
            
        } catch (UnirestException unirestException) {

        }
        return oNESingleResponse;
    }

    public FTSingleCreditResponse doNipFundsTransfer(FTSingleCreditRequest request) {
        init();

        FTSingleCreditResponse oFTSingleCreditResponse = new FTSingleCreditResponse();

        try {
            String url = BASE_URL + "/nip/fundstransfer";
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(url)
                    .header("Authorization", AUTHORIZATION)
                    .header("SignatureMethod", SIGNATURE_METHOD)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(gson.toJson(request))
                    .asString();

            if (httpResponse.getStatus() == 200 || httpResponse.getStatus() == 202) {
                String res = httpResponse.getBody();
                System.out.println(res);
                oFTSingleCreditResponse = gson.fromJson(res, FTSingleCreditResponse.class);
            }

            return oFTSingleCreditResponse;
        } catch (UnirestException unirestException) {

        }

        return errorResponse(request);
    }

    public TSQuerySingleResponse doNipStatusQuery(TSQuerySingleRequest request) {

        init();
        TSQuerySingleResponse oTSQuerySingleResponse = new TSQuerySingleResponse();

        String res;

        try {
            String url = BASE_URL + "/nip/statusQuery";
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(url)
                    .header("Authorization", AUTHORIZATION)
                    .header("SignatureMethod", SIGNATURE_METHOD)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(gson.toJson(request))
                    .asString();

           if (httpResponse.getStatus() == 200 || httpResponse.getStatus() == 202) {
                res = httpResponse.getBody();
                System.out.println(res);
                oTSQuerySingleResponse = gson.fromJson(res, TSQuerySingleResponse.class);
            }

            return oTSQuerySingleResponse;
        } catch (UnirestException unirestException) {

        }

        return oTSQuerySingleResponse;

    }

    private FTSingleCreditResponse errorResponse(FTSingleCreditRequest oFTSingleCreditRequest) {
        FTSingleCreditResponse oFTSingleCreditResponse = new FTSingleCreditResponse();
        oFTSingleCreditResponse.setNameEnquiryRef(oFTSingleCreditRequest.getNameEnquiryRef());
        oFTSingleCreditResponse.setDestinationInstitutionCode(oFTSingleCreditRequest.getDestinationInstitutionCode());
        oFTSingleCreditResponse.setChannelCode(oFTSingleCreditRequest.getChannelCode());
        oFTSingleCreditResponse.setBeneficiaryAccountName(oFTSingleCreditRequest.getBeneficiaryAccountName());
        oFTSingleCreditResponse.setBeneficiaryAccountNumber(oFTSingleCreditRequest.getBeneficiaryAccountNumber());
        oFTSingleCreditResponse.setBeneficiaryBankVerificationNumber(oFTSingleCreditRequest.getBeneficiaryBankVerificationNumber());
        oFTSingleCreditResponse.setBeneficiaryKYCLevel(oFTSingleCreditRequest.getBeneficiaryKYCLevel());
        oFTSingleCreditResponse.setOriginatorAccountName(oFTSingleCreditRequest.getOriginatorAccountName());
        oFTSingleCreditResponse.setOriginatorAccountNumber(oFTSingleCreditRequest.getOriginatorAccountNumber());
        oFTSingleCreditResponse.setOriginatorBankVerificationNumber(oFTSingleCreditRequest.getOriginatorBankVerificationNumber());
        oFTSingleCreditResponse.setOriginatorKYCLevel(oFTSingleCreditRequest.getOriginatorKYCLevel());
        oFTSingleCreditResponse.setTransactionLocation("");
        oFTSingleCreditResponse.setNarration(oFTSingleCreditRequest.getNarration());
        oFTSingleCreditResponse.setPaymentReference(oFTSingleCreditRequest.getPaymentReference());
        oFTSingleCreditResponse.setAmount(oFTSingleCreditRequest.getAmount());
        oFTSingleCreditResponse.setResponseCode(ResponseCodes.SYSTEM_MALFUNCTION.getResponseCode());
        oFTSingleCreditResponse.setResponseDescription("Unable to perform interbank transfer.");

        return oFTSingleCreditResponse;
    }

}
