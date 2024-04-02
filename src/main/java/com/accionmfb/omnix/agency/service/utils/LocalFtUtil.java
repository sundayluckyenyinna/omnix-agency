package com.accionmfb.omnix.agency.service.utils;

import com.accionmfb.omnix.agency.constant.ResponseCodes;
import com.accionmfb.omnix.agency.dto.Payload;
import com.accionmfb.omnix.agency.jwt.JwtTokenUtil;
import com.accionmfb.omnix.agency.model.AccionAgent;
import com.accionmfb.omnix.agency.model.AppUser;
import com.accionmfb.omnix.agency.model.Branch;
import com.accionmfb.omnix.agency.model.FundsTransfer;
import com.accionmfb.omnix.agency.payload.FundsTransferResponsePayload;
import com.accionmfb.omnix.agency.payload.OmniResponsePayload;
import com.accionmfb.omnix.agency.repository.AgencyRepository;
import com.accionmfb.omnix.agency.service.GenericService;
import com.google.gson.Gson;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;

/**
 *
 * @author dofoleta
 */
public class LocalFtUtil {

    @Autowired
    JwtTokenUtil jwtToken;

    @Autowired
    GenericService genericService;

    @Autowired
    Gson gson;

    @Autowired
    AgencyRepository agencyRepository;

    public String processOPayMerchantInflow(String token, Payload oPayload, String posSettlementAccount) {
        OmniResponsePayload errorResponse = new OmniResponsePayload();
        String requestBy = jwtToken.getUsernameFromToken(token);
        String channel = jwtToken.getChannelFromToken(token);
        String userCredentials = jwtToken.getUserCredentialFromToken(token);
        String ofsBase;
        String defaultBranch = "NG00100068"; // Digital Branch

        oPayload.setAmount(oPayload.getAmount().replaceAll(",", ""));
        String transRef = "";
        try {

            //Check if an agent exist with the terminal ID
            AccionAgent agent = agencyRepository.getAgentUsingTerminalId(oPayload.getTerminalId(), "OPay");

            //Check if the branch is valid
            Branch branch = genericService.getBranchUsingBranchCode(agent.getBranchCode());
            if (branch == null) {
                //Log the error
                branch = genericService.getBranchUsingBranchCode(defaultBranch);
            }

            //Check the channel information
            AppUser appUser = agencyRepository.getAppUserUsingUsername(requestBy);

            transRef = oPayload.getTransactionReference() == null ? genericService.generateTransRef("OPay") : oPayload.getTransactionReference();
            String narration = "Merchant Payment - ".concat(oPayload.getSerialNumber()).concat(" ").concat(transRef);
            String transactionType = "AC";
            String inputter = "OPay".concat(oPayload.getSerialNumber());
            String authorizer = "OPay".concat(oPayload.getSerialNumber());

            String debitAccount = posSettlementAccount;
            String creditAccount = agent.getAgentAccountNumber();
            String creditAccountName = agent.getAgentName();
            String merchantMobileNumber = agent.getAgentMobile();

            double netAmount = 0.00;
            double fee = 0.00;

            try {
                fee = Double.valueOf(oPayload.getFee());
                if (fee > 150) {
                    fee = 150;
                }
                netAmount = Double.valueOf(oPayload.getAmount());
                netAmount = netAmount - fee;
            } catch (NumberFormatException e) {
                double amount = Double.valueOf(oPayload.getAmount());
                fee = amount * (0.5 / 100);
                if (fee > 150) {
                    fee = 150;
                }
                netAmount = amount - fee;
            }

            //Persist the funds transfer data
            FundsTransfer newFT = new FundsTransfer();
            newFT.setAmount(oPayload.getAmount());
            newFT.setFee(String.valueOf(fee));
            newFT.setAppUser(appUser);
            newFT.setBranch(branch);
            newFT.setCreditCurrency("NGN");
            newFT.setCreatedAt(LocalDateTime.now());
            newFT.setCreditAccount(creditAccount);
            newFT.setCreditAccountName(creditAccountName);
            newFT.setCreditAccountKyc("3");
            newFT.setCustomer(null);
            newFT.setDebitAccount(debitAccount);
            newFT.setDebitAccountName("INTERNAL ACCOUNT");
            newFT.setDebitAccountKyc("3");
            newFT.setDebitCurrency("NGN");
            newFT.setDestinationBank("ACCION");
            newFT.setGateway("ACCION");
            newFT.setMobileNumber(merchantMobileNumber);
            newFT.setNarration(narration);
            newFT.setRequestId(transRef);
            newFT.setStatus("COMPLETED");
            newFT.setSourceBank("ACCION");
            newFT.setT24TransRef("");
            newFT.setTimePeriod(genericService.getTimePeriod());
            newFT.setTransType("ATI");
            newFT.setDebitAccountType("SYSTEM");
            newFT.setDestinationBankCode("090134"); //Accion Bank Code
            newFT.setCreditAccountType("S");
            FundsTransfer createFT = agencyRepository.createFundsTransfer(newFT);

            //Generate Funds Transfer OFS
            ofsBase = genericService.generateFTOFS(transRef, debitAccount, creditAccount, oPayload.getAmount(), narration, transactionType, inputter, authorizer);

            // check the old implementation in expert bridge
            ofsBase = ofsBase.concat(",COMMISSION.CODE::=WAIVE");

            String inflowVersion = "FUNDS.TRANSFER,PHB.GENERIC.ACTR.INFLOW/I/PROCESS/0/0";
            String ofsRequest = inflowVersion + "," + userCredentials + "/" + createFT.getBranch().getBranchCode() + ",/" + transRef + "," + ofsBase;
            String newOfsRequest = genericService.formatOfsUserCredentials(ofsRequest, userCredentials);
            //Generate the OFS Response log
            genericService.generateLog("Transaction With PL", token, newOfsRequest, "OFS Request", "INFO", transRef);
            String ofsResponse = genericService.postToT24(ofsRequest);

            //Update the Funds Transfer request
            Pair<String, String> ftResp = genericService.getResponseMessage(ofsResponse);
            String responseCode = ftResp.item1;
            String responseMessage = ftResp.item2;
            String sataus = "FAILED";
            String ftId;
            if (responseCode.equals(ResponseCodes.SUCCESS_CODE.getResponseCode())) {
                sataus = "SUCCESS";
                ftId = genericService.getT24TransIdFromResponse(ofsResponse);
            } else {
                ftId = "";
            }
            createFT.setFailureReason(responseMessage);
            createFT.setStatus(sataus);
            createFT.setT24TransRef(ftId);
            agencyRepository.updateFundsTransfer(createFT);

            //Log the error. 
            genericService.generateLog("Transaction With PL", token, sataus, "API Response", "INFO", transRef);
            //Create User Activity log
            genericService.createUserActivity(debitAccount, "Funds Transfer", oPayload.getAmount(), channel, sataus, merchantMobileNumber, sataus.charAt(0));

            FundsTransferResponsePayload ftResponse = new FundsTransferResponsePayload();
            ftResponse.setAmount(createFT.getAmount());
            ftResponse.setCreditAccount(createFT.getCreditAccount());
            ftResponse.setCreditAccountName(createFT.getCreditAccountName());
            ftResponse.setDebitAccount(createFT.getDebitAccount());
            ftResponse.setDebitAccountName(createFT.getDebitAccountName());
            ftResponse.setNarration(createFT.getNarration());
            ftResponse.setResponseCode(responseCode);
            ftResponse.setResponseMessage(responseMessage);
            ftResponse.setStatus(createFT.getStatus());
            ftResponse.setTransRef(transRef);
            ftResponse.setT24TransRef(ftId);
            return gson.toJson(ftResponse);

        } catch (NumberFormatException | NoSuchMessageException ex) {
            //Log the response
            System.out.println("Error: " + ex.getMessage());
            genericService.generateLog("Transaction With PL", token, ex.getMessage(), "API Error", "DEBUG", transRef);
            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
            errorResponse.setResponseMessage(ex.getMessage());
            return gson.toJson(errorResponse);
        }
    }
}
