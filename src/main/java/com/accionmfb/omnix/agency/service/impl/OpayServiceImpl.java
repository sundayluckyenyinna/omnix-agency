package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.dto.HashData;
import com.accionmfb.omnix.agency.dto.Payload;
import com.accionmfb.omnix.agency.model.MerchantTranLog;
import com.accionmfb.omnix.agency.payload.OpayPaymentRequestPayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.OpayService;
import com.google.gson.Gson;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author dofoleta
 */
@Service
public class OpayServiceImpl implements OpayService {

    @Autowired
    Gson gson;

    @Autowired
    AgencyRepository agencyRepository;

    @Value("${pos.settlement.account::NGN1044900010001}")
    private String posSettlementAccount;

    @Override
    public String opayPaymentNotification(OpayPaymentRequestPayload requestPayload) {

        // validate paymant 
        // check terminal is mapped and read agent data from agent table
        Payload oPayload = requestPayload.getPayloadObject();

        HashData data = new HashData();

        data.setAmount(requestPayload.getPayloadObject().getAmount());
        data.setCurrency(requestPayload.getPayloadObject().getCurrency());
        data.setReference(requestPayload.getPayloadObject().getReference());
        data.setRefunded(requestPayload.getPayloadObject().isRefunded() == true ? "t" : "f");
        data.setStatus(requestPayload.getPayloadObject().getStatus());
        data.setTimestamp(requestPayload.getPayloadObject().getTimestamp());
        data.setToken(requestPayload.getPayloadObject().getToken());
        data.setTransactionID(requestPayload.getPayloadObject().getTransactionId());

        String plainValue = gson.toJson(data);

        String hashedValue = encryptThisString(plainValue);

        if (!hashedValue.equals(requestPayload.getSha512())) {

            // Return error
            return "Warning: Invalid message";
        }

        //create merchant transaction record with status set as Ongoing
        MerchantTranLog oTranLoag = new MerchantTranLog();
        oTranLoag = gson.fromJson(gson.toJson(oPayload), MerchantTranLog.class);
        agencyRepository.createMerchantTranLog(oTranLoag);

//        // Credit merchant's account
//        FundsTransferResponsePayload ftResponse = performAccountDebit(requestPayload, oFundsTransfer, token,
//                channel, requestBy, requestBy, charge, userCredentials, debitAccount.getBranch().getBranchCode(), debitAccount);
//
//        if (ftResponse == null) {
//            // return error
//            genericService.generateLog("Outbound NIP", token, "System Malfunction", "API Error", "DEBUG", requestPayload.getRequestId());
//            errorResponse.setResponseCode(ResponseCodes.SYSTEM_MALFUNCTION.getResponseCode());
//            errorResponse.setResponseMessage("System Malfunction");
//            return gson.toJson(errorResponse);
//        } else {
//            if (!ftResponse.getResponseCode().equalsIgnoreCase(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
//                // return error
//                genericService.generateLog("Outbound NIP", token, "System Malfunction", "API Error", "DEBUG", requestPayload.getRequestId());
//                errorResponse.setResponseCode(ftResponse.getResponseCode());
//                errorResponse.setResponseMessage(ftResponse.getResponseMessage());
//                return gson.toJson(errorResponse);
//            }
//        }
//
//        // post transaction to T24
//        // update FT Table
//        //Respond
//        FundsTransferRequestDto request = new FundsTransferRequestDto();
//        request.setTransactionType("POSDEPOSIT");
//
////        System.out.println(JsonConverter.toJson(root));
//        request.setBeneficiaryBankCode("");
//        request.setChargeCode("");
//        request.setCreditCurrency(root.getPayload().getCurrency());
//        request.setTransactionCode(ConfigProvider.getString(wsUser + ".LOCALTRF.TRAN.TYPE"));
//        request.setTransactionSource(ConfigProvider.getString(wsUser + ".CHANNEL"));
//        request.setTransRef(root.getPayload().getTransactionReference());
//
//        try {
//            double fee = Double.valueOf(root.getPayload().getFee());
//            if (fee > 150) {
//                fee = 150;
//            }
//            double netAmount = Double.valueOf(root.getPayload().getAmount());
//            netAmount = netAmount - fee;
//            request.setAmount(netAmount);
//        } catch (NumberFormatException e) {
//            double amount = Double.valueOf(root.getPayload().getAmount());
//            double fee = amount * (0.5 / 100);
//            if (fee > 150) {
//                fee = 150;
//            }
//            amount = amount - fee;
//            request.setAmount(amount);
//        }
//
//        request.setBranchCode("NG0010058"); // Digital Branch
//        if (wsUser.equalsIgnoreCase("OPAY01")) {
//            request.setCreditCurrency("NGN");
//            request.setDebitCurrency("NGN");
//            try {
//                request.setNarration(root.getPayload().getRetrievalReferenceNumber().concat(" : ").concat(root.getPayload().getReference()).concat(" : POS Payment: "));
//                request.setDebitAccount(ConfigProvider.getString("opay.pos.settlement.ac"));
//                request.setTransactionType("MERCHANTPOS");
//            } catch (Exception e) {
//            }
//
//        } else {
//            Pair<Boolean, Response> oValidDebitAccount = Util.validateDebitAccount(t24core, request.getDebitAccount(), wsUser, null, request.getTransRef());
//
//            if (!oValidDebitAccount.item1) {
//                return oValidDebitAccount.item2;
//            }
//        }
//
//        TranUtil oTranUtil = new TranUtil();
//        Pair<Boolean, String> accountNumberResponse = oTranUtil.fetchMerchantAccountByTermialSerialNo(root.getPayload().getSerialNumber());
//
//        if (accountNumberResponse.item1) {
//            request.setCreditAccount(accountNumberResponse.item2);
//        } else {
//            Pair<String, Long> resp = DbUtil.addTransactionLog(
//                    request.getTransRef(), //t24Ref, 
//                    request.getTransRef(),
//                    request.getAmount(),
//                    request.getCharges(),
//                    request.getVat(),
//                    request.getNarration(),
//                    request.getDebitAccount(),
//                    "NA",
//                    ResponseCode.REQUEST_PROCESSING_IN_PROGRESS,
//                    request.getTransactionSource(),
//                    "Closed",
//                    request.getTransactionType(),
//                    request.getTransRef(),
//                    request.getSessionId(),
//                    request.getDestBankCode(),
//                    request.getGsmNetwork(),
//                    request.getChargeBiller(),
//                    request.getAgentAccountNumber());
//
//        }
//
//        Connection conn = new DatabaseResource().getLocalConnection();
//        final ResponseDto ofsRes = t24core.doGenericFundsTransfer(request, conn, wsUser);
//        String status = "Failed";
//        if (ofsRes != null) {
//            if (ofsRes.getResponseCode().equals(ResponseCode.SUCCESSFUL)) {
//                status = "SUCCESSFUL";
//                ofsRes.setStatus("Ok");
//            } else {
//                ofsRes.setStatus("Not Ok");
//            }
//            DbUtil.updateTransactionLog(conn, ofsRes.getResponseCode(), ofsRes.getPaymentReference(), ofsRes.getId(), status);
//        }
//        try {
//            conn.close();
//        } catch (SQLException sQLException) {
//        }
//        return (Response) getResponse(ofsRes, Response.accepted(), null);

        return "";
    }

    public String encryptThisString(String input) {
        try {
            // getInstance() method is called with algorithm SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        } // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
        }
        return null;
    }
}
