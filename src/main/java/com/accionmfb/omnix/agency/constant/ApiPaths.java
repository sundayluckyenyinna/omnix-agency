/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.constant;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
public class ApiPaths {

    /**
     * This class includes the name and API end points of other microservices
     * that we need to communicate. NOTE: WRITE EVERYTHING IN ALPHABETICAL ORDER
     */
    //A
    public static final String ACCOUNT_BALANCE = "/balance";
    public static final String AIRTIME_SELF = "/self";
    public static final String AIRTIME_OTHERS = "/others";
    public static final String ACCOUNT_DETAILS = "/details";
    public static final String ACCOUNT_STATEMENT = "/statement";
    public static final String ACCION_AGENT_BOARD = "/agent/board";
    public static final String ACCION_AGENT_DETAILS = "/agent/details";
    public static final String ACCOUNT_OPENING = "/new";
    public static final String ACCOUNT_TO_WALLET = "/local/wallet";
    //B
    public static final String BASE_API = "/omnix/api";
    //C
    public static final String CABLE_SUBSCRIPTION = "/subscription";
    public static final String CABLETV_DETAILS = "/details";
    public static final String CABLETV_SMARTCARD_DETAILS = "/smartcard/lookup";
    public static final String CABLETV_BILLERS = "/billers";
    public static final String CREATE_INDIVIDUAL_CUSTOMER_WITH_BVN = "/individual/new/bvn";
    public static final String CREATE_INDIVIDUAL_CUSTOMER_WITH_NOBVN = "/individual/new/no-bvn";
    public static final String CUSTOMER_DETAILS = "/details";
    public static final String CUSTOMER_DETAILS_BY_MOBILE = "/details/by-mobile";

    //D
    public static final String DATA_SELF = "/data/self";
    public static final String DATA_OTHERS = "/data/others";
    //E
    public static final String ELECTRICITY_PAYMENT = "/pay-bill";
    public static final String ELECTRICITY_DETAILS = "/details";
    public static final String ELECTRICITY_BILLERS = "/billers";
    public static final String ELECTRICITY_SMARTCARD_DETAILS = "/meter/lookup";
    public static final String ERRAND_PAY_CASHOUT_NOTIFICATION = "/errandpay/notification/pos";
    public static final String ERRAND_PAY_AGENT_BALANCE = "/errandpay/notification/balance";


    //F
    //G
    public static final String GRUPP_CASHOUT_NOTIFICATION = "/grupp/notification/pos";
    public static final String GRUPP_CASHOUT_NOTIFICATION_2 = "/grupp/notification/pos";
    public static final String GRUPP_AGENT_DETAILS = "/grupp/notification/agent";
    public static final String GRUPP_DISBURSEMENT_NOTIFICATION = "/grupp/notification/disbursement";
    public static final String GRUPP_AGENT_BALANCE = "/grupp/notification/balance";
    public static final String GRUPP_CASHOUT_STATUS_REPORT = "/grupp/cashout/report";

    //H
    public static final String HEADER_STRING = "Authorization";
    //L
    public static final String LOCAL_TRANSFER = "/local";
    public static final String LOCAL_TRANSFER_WITH_PL_INTERNAL = "/local/internal-account";
    public static final String LOCAL_TRANSFER_WITH_CHARGE = "/local/with-charges";
    public static final String LOCAL_TRANSFER_INTERNAL_DEBIT_WITH_CHARGE = "/local/debit-internal-account";
    public static final String LOCAL_TRANSFER_REVERSE = "/local/reverse";
    public static final String LOCAL_TRANSFER_STATUS = "/trans/status";
    //O
    //P
    public static final String POLARIS_BANK_ACCOUNT_VALIDATION = "/polaris/account/validation";
    public static final String POLARIS_BANK_DEPOSIT = "/polaris/transaction/deposit";
    public static final String POLARIS_BANK_TRANSACTION_QUERY = "/polaris/transaction/query";
    //M
    //N
    public static final String NIP_TRANSFER = "/inter-bank";
    public static final String NIP_TRANSFER_STATUS = "/inter-bank/status";
    public static final String NIP_NAME_ENQUIRY = "/inter-bank/name-enquiry";
    public static final String NIP_TO_WALLET = "/wallet/deposit";
    public static final String STATISTICS_MEMORY = "/actuator/stats";
    //R
    //S
    public static final String SMS_NOTIFICATION = "/sms/send";
    //T
    public static final String TOKEN_PREFIX = "Bearer";
    public static final String TRIFTA_ACCOUNT_OPENING = "/trifta/account/new";
    public static final String TRIFTA_ACCOUNT_BALANCE = "/trifta/account/balance";
    public static final String TRIFTA_ACCOUNT_DETAILS = "/trifta/account/details";
    public static final String TRIFTA_ACCOUNT_STATEMENT = "/trifta/account/statement";
    public static final String TRIFTA_CUSTOMER_WITH_BVN = "/trifta/customer/bvn";
    public static final String TRIFTA_CUSTOMER_WITHOUT_BVN = "/trifta/customer/nobvn";
    public static final String TRIFTA_CUSTOMER_DETAILS = "/trifta/customer/details";
    public static final String TRIFTA_FUNDS_TRANSFER_LOCAL = "/trifta/funds-transfer/local";
    public static final String TRIFTA_LOCAL_TRANSFER_REVERSE = "/trifta/funds-transfer/local/reverse";
    public static final String TRIFTA_LOCAL_TRANSFER_STATUS = "/trifta/funds-transfer/local/status";
    public static final String TRIFTA_NIP_NAME_ENQUIRY = "/trifta/funds-transfer/nip/enquiry";
    public static final String TRIFTA_FUNDS_TRANSFER_NIP = "/trifta/funds-transfer/nip";
    public static final String TRIFTA_AIRTIME_CALLS = "/trifta/airtime/call";
    public static final String TRIFTA_AIRTIME_DATA = "/trifta/airtime/data";
    public static final String TRIFTA_BILLS_CABLE_DETAILS = "/trifta/bills/cable/details";
    public static final String TRIFTA_BILLS_CABLE_LOOKUP = "/trifta/bills/cable/lookup";
    public static final String TRIFTA_BILLS_CABLE_BILLERS = "/trifta/bills/cable/billers";
    public static final String TRIFTA_BILLS_CABLE = "/trifta/bills/cable";
    public static final String TRIFTA_BILLS_ELECTRICITY_LOOKUP = "/trifta/bills/electricity/lookup";
    public static final String TRIFTA_BILLS_ELECTRICITY_DETAILS = "/trifta/bills/electricity/details";
    public static final String TRIFTA_BIILS_ELECTRICITY_BILLERS = "/trifta/bills/electricity/billers";
    public static final String TRIFTA_BILLS_ELECTRICITY = "/trifta/bills/electricity";
    //W
    //Z
    public static final String ZENITH_BANK_ACCOUNT_VALIDATION = "/zenith/account/validation";
    public static final String ZENITH_BANK_DEPOSIT = "/zenith/transaction/deposit";
    public static final String ZENITH_BANK_TRANSACTION_QUERY = "/zenith/transaction/query";
    public static final String encryptPayloadUrl = "http://mobilebanking.accionmfb.com:1018/f/app/encrypt";
    public static final String decryptPayloadUrl = "http://mobilebanking.accionmfb.com:1018/f/app/decrypt";
    public static final String localTransferUrl = "http://mobilebanking.accionmfb.com:1018/proxy/fundstransferService/local";
}
