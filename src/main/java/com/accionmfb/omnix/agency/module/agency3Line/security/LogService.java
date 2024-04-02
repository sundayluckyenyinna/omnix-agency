package com.accionmfb.omnix.agency.module.agency3Line.security;

public interface LogService {

    public void logInfo(String app, String token, String logMessage, String logType, String requestId);

    public void logError(String app, String token, String logMessage, String logType, String requestId);
}
