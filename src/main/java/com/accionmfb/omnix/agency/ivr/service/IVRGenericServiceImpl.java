package com.accionmfb.omnix.agency.ivr.service;

import com.accionmfb.omnix.agency.ivr.repository.GenericRepository;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.ivr.ThirdPartyVendors;
import com.accionmfb.omnix.agency.payload.OmnixRequestPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static com.accionmfb.omnix.agency.service.impl.GenericServiceImpl.setKey;

/**
 * @author user on 31/10/2023
 */
@Service
@Slf4j
@PropertySource("classpath:application.properties")
public class IVRGenericServiceImpl implements IVRGenericService{

    @Autowired
    MessageSource messageSource;
    @Autowired
    Environment env;
    @Autowired
    GenericRepository genericRepository;
    @Autowired
    JwtTokenUtil jwtToken;

    private static String AUTHORIZATION = "";
    private static String SIGNATURE = "";
    private static String TIMESTAMP = "";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static String NONCE = "";
    private static String SIGNATURE_METHOD = "";
    private static String USERNAME = "";
    private static String PASSWORD = "";
    private static String USER_SECRET_KEY = "";
    private static String MIDDLEWARE_HOST = "";
    private static String MIDDLEWARE_PORT = "";
    private static String BASE_URL = "";
    public String overrideMessage = "";
    private SecretKeySpec secretKey;
    private byte[] key;
    private static final String IS_BASE_URL = "http://127.0.0.1:8080/accion/api/service";

    @Override
    public String encryptText(String plainText) {
        BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder();
        String encyptedText = bCryptEncoder.encode(plainText);

        return encyptedText;
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.parseInt(env.getProperty("webservice.morning.start").trim());
        int morningEnd = Integer.parseInt(env.getProperty("webservice.morning.end").trim());
        int afternoonStart = Integer.parseInt(env.getProperty("webservice.afternoon.start").trim());
        int afternoonEnd = Integer.parseInt(env.getProperty("webservice.afternoon.end").trim());
        int eveningStart = Integer.parseInt(env.getProperty("webservice.evening.start").trim());
        int eveningEnd = Integer.parseInt(env.getProperty("webservice.evening.end").trim());
        int nightStart = Integer.parseInt(env.getProperty("webservice.night.start").trim());
        int nightEnd = Integer.parseInt(env.getProperty("webservice.night.end").trim());
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }


    @Override
    public String getPostingDate() {
        //This returns the current posting date
        String ofsRequest = "\"" + "DATES,INPUT/S/PROCESS," + env.getProperty("api.webservice.inputter.login.credentials").trim() + "/,NG0010001" + "\"";

        String response = endPointPostRequest("/generic/payment/postofs", ofsRequest);
        if (response == null) {
            return messageSource.getMessage("appMessages.invalidPostingDate", new Object[0], Locale.ENGLISH);
        }

        String t24Date = getStringFromOFSResponse(response, "TODAY:1:1");
        return t24Date;
    }

    @Override
    public String getStringFromOFSResponse(String response, String stringToGet) {
        //Create an array of the response string
        if (response.equals("")) {
            return "";
        } else if (!response.equals("") && !response.isEmpty()) {
            String[] responseArray = response.split(",");
            for (String str : responseArray) {
                //Split each string by the =
                String fields[] = str.split("=");
                if (fields.length == 0) {
                    return "";
                }

                if (fields[0].equals(stringToGet)) {
                    return fields.length == 2 ? fields[1] : "";
                }
            }
        }
        return "";
    }


    @Override
    public String endPointPostRequest(String url, String requestJson) {
        AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((env.getProperty("accion.authorization.username") + ":" + env.getProperty("accion.authorization.password").trim()).getBytes());
        NONCE = String.valueOf(Math.random());
        TIMESTAMP = TIMESTAMP_FORMAT.format(new Date());
        String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, env.getProperty("accion.authorization.username"), env.getProperty("accion.authorization.secret.key").trim());
        SIGNATURE = hash(SignaturePlain, Objects.requireNonNull(env.getProperty("accion.authorization.signature.method")));
        String responseString = "";
        Client client = ClientBuilder.newClient();
        Response response = client.target(env.getProperty("accion.middleware.base.url") + url)
                .request()
                .header("Authorization", AUTHORIZATION)
                .header("SignatureMethod", env.getProperty("accion.authorization.signature.method"))
                .header("Accept", "application/json")
                .header("Timestamp", TIMESTAMP)
                .header("Nonce", NONCE)
                .header("Signature", SIGNATURE)
                .header("ContentType", "application/json")
                .post(Entity.json(requestJson));
        responseString = (String) response.getEntity();
//            response.close();
        client.close();
        client.close();
        log.info("responseString ----------------------------->>>>>>>>>>>>>>> {}", responseString);
        return responseString;
    }

    @Override
    public String endPointPostRequest(String url, String requestJson, String username, String password) {
        USERNAME = username;
        PASSWORD = password;
        SIGNATURE_METHOD = env.getProperty("accion.authorization.signature.method");
        USER_SECRET_KEY = env.getProperty("accion.authorization.secret.key").trim();
        AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
        MIDDLEWARE_HOST = env.getProperty("accion.middleware.host");
        MIDDLEWARE_PORT = env.getProperty("accion.middleware.port");
        BASE_URL = "http://" + MIDDLEWARE_HOST + ":" + MIDDLEWARE_PORT + "/T24Gateway/services";
        NONCE = String.valueOf(Math.random());
        TIMESTAMP = TIMESTAMP_FORMAT.format(new Date());
        String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, USERNAME, USER_SECRET_KEY);
        SIGNATURE = SignaturePlain; //this must be computation of SignaturePlain using SHA512
        SIGNATURE = hash(SignaturePlain, SIGNATURE_METHOD);
        Client client = ClientBuilder.newClient();
        try {
            Response response = client.target(BASE_URL + url)
                    .request()
                    .header("Authorization", AUTHORIZATION)
                    .header("SignatureMethod", SIGNATURE_METHOD)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Signature", SIGNATURE)
                    .header("ContentType", "application/json")
                    .post(Entity.json(requestJson));
            String responseString = (String) response.getEntity();
//            response.close();
            client.close();
            log.info("responseString ----------------------------->>>>>>>>>>>>>>> {}", responseString);

            return responseString;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }


    @Override
    public String validateResponse(String response) {
        if (response.contains("Maximum T24 users")) {
            return messageSource.getMessage("appMessages.maxLicense", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("Failed to receive message")) {
            return messageSource.getMessage("appMessages.serverDisconnect", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("No records were found") || response.contains("No entries for the period")) {
            return messageSource.getMessage("appMessages.recordRetrievalFailed", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("INVALID COMPANY SPECIFIED")) {
            return messageSource.getMessage("appMessages.invalidCompany", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("java.lang.OutOfMemoryError")) {
            return messageSource.getMessage("appMessages.outOfMemory", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("Failed to connect to host")) {
            return messageSource.getMessage("appMessages.serverDisconnect", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("No Cheques found")) {
            return messageSource.getMessage("appMessages.noCheques", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("Unreadable")) {
            return messageSource.getMessage("appMessages.dataTooLarge", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("MANDATORY INPUT")) {
            return response;
        }

        if (response.contains("Some errors while encountered")) {
            return response;
        }

        if (response.contains("Some override conditions have not been met")) {
            return response;
        }

        if (response.contains("don't have permissions to access this data")) {
            return messageSource.getMessage("appMessages.noPermission", new Object[0], Locale.ENGLISH);
        }

        if (response.equals("<Unreadable>")) {
            return response;
        }

        if (response.contains("User has no id")) {
            return response;
        }

        if (response.equals("java.net.SocketException: Unexpected end of file from server")) {
            return response;
        }

        String[] responseSplit = response.split("//");
        if (responseSplit.length == 2) {
            if ("-1".equals(responseSplit[1].substring(0, 2))) {
                return response;
            } else if ("-2".equals(responseSplit[1].substring(0, 2))) {
                //Set the override message
                // overrideMessage = response;
                return response;
            }
        }

        if (response.contains("No Cash available")) {
            return "No cash available";
        }

        if (response.contains("INVALID ACCOUNT")) {
            return response;
        }

        if (response.contains("MISSING") && !response.substring(0, 4).equals("\"//1")) {
            return response;
        }

        if (response.contains("java.net.SocketException")
                || response.contains("java.net.ConnectException")
                || response.contains("java.net.NoRouteToHostException")) {
            return response;
        }

        if (response.contains("SECURITY VIOLATION")) {
            return response;
        }

        if (response.contains("NOT SUPPLIED")) {
            return response;
        }

        if (response.contains("NO EN\\\"\\t\\\"TRIES FOR PERIOD")) {
            return messageSource.getMessage("appMessages.recordRetrievalFailed", new Object[0], Locale.ENGLISH);
        }

        if (response.contains("CANNOT ACCESS RECORD IN ANOTHER COMPANY")) {
            return response;
        }

        return null;
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

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase();
    }


    @Override
    public ThirdPartyVendors getIPDetails(String IP) {
        return genericRepository.get3rdPartyDetailsWithIPAddress(IP);
    }

    @Override
    public String getProductCodeWithProductCode(String productCode) {
        return genericRepository.getProductCodeWithProductId(productCode);
    }

    @Override
    public String getBranchNameUsingCode(String branchCode) {
        return genericRepository.getBranchNameFromCode(branchCode);
    }

    @Override
    public String getT24TransIdFromResponse(String response) {
        String[] splitString = response.split("//");
        return splitString[0].replace("\"", "");
    }

    @Override
    public String generateMnemonic(int max) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder mnemonic = new StringBuilder();
        while (max-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            mnemonic.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return mnemonic.toString();
    }

    @Override
    public String ofsResponse(String environment, String ofsRequest) {
        //Get from the environment where the menu is pointed to
        String response = "";
        if (environment == null || ofsRequest == null || environment.equals("") || ofsRequest.equals("")) {
            return null;
        }

        switch (environment.trim()) {
            case "Production":
                response = endPointPostRequest("/generic/payment/postofs", ofsRequest, env.getProperty("accion.middleware.production.username"), env.getProperty("accion.middleware.production.password").trim());
                break;
            case "Staging":
                response = endPointPostRequest("/generic/payment/postofs", ofsRequest, env.getProperty("accion.middleware.staging.username"), env.getProperty("accion.middleware.staging.password").trim());
                break;
            default:
                return null;
        }
        return response;
    }

    @Override
    public String hashAccountBalanceRequest(OmnixRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        log.info("trying to encrypt");
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            secretKey = setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

}
