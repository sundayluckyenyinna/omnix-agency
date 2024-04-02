//package com.accionmfb.omnix.agency.module.agency3Line.security;
//
//import org.springframework.security.authentication.AnonymousAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.util.ResourceBundle;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//public class UserProvider {
//
//    private static ResourceBundle configBundle;
//    private static ResourceBundle configBundleRole;
//    private static ResourceBundle resBundleC;
//    private static AesService aesService;
//
//    public static void setAesService(AesService aesService) {
//        UserProvider.aesService = aesService;
//    }
//
//    public static void initialize() {
//        configBundle = ResourceBundle.getBundle("users");
//        configBundleRole = ResourceBundle.getBundle("webproxyacl");
//        resBundleC = ResourceBundle.getBundle("userchannel");
//    }
//
//    public static String getUserPassword(String wsUser) {
//
//        return configBundle.getString(wsUser);
//    }
//
//    public static T24CredDto getT24UserCred(String wsUser) {
//        T24CredDto cred = new T24CredDto();
//        try {
//            cred.setUsername(aesService.decryptString(ConfigProvider.getString(wsUser + ".T24USER"), aesEncryptionKey));
//            cred.setPassword(aesService.decryptString(ConfigProvider.getString(wsUser + ".T24PASS"), aesEncryptionKey));
//            cred.setTransType(aesService.decryptString(ConfigProvider.getString(wsUser + ".TRAN.TYPE"), aesEncryptionKey));
//            try {
//                cred.setAuthrizer(aesService.decryptString(ConfigProvider.getString(wsUser + ".AUTH"), aesEncryptionKey));
//                cred.setAuthrizerPassword(aesService.decryptString(ConfigProvider.getString(wsUser + ".AUTH.PASS"), aesEncryptionKey));
//            } catch (Exception ex) {
//                Logger.getLogger(UserProvider.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(UserProvider.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return cred;
//    }
//    public static String[] getUserRoles(String wsUser) {
//
//        return configBundleRole.getString(wsUser).split(";");
//    }
//
//    public static String getAuthenticatedUsername() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
//            return authentication.getName();
//        }
//        return null;
//    }
//
//    public static String getUserChannel(String user) {
//        return resBundleC.getString(user);
//    }
//
//}
//
