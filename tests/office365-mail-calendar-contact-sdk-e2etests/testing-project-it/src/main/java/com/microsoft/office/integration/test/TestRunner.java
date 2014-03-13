package com.microsoft.office.integration.test;

import android.os.Bundle;
import android.test.InstrumentationTestRunner;

public class TestRunner extends InstrumentationTestRunner {

    private static final String SERVICE_AUTH = "serviceAuth";
    private static final String SERVICE_ROOT_URL = "serviceRootURL";
    private static final String SERVICE_USERNAME = "serviceUsername";
    private static final String SERVICE_PASSWORD = "servicePassword";
    private static final String SERVICE_RESOURCE_ID = "serviceResourceId";
    private static final String SERVICE_CLIENT_ID = "serviceClientId";
    private static final String SERVICE_AUTHORITY_URL = "serviceAuthorityUrl";

    private static AuthType sAuthType = AuthType.UNDEFINED;
    private static String sRootUrl = null;
    private static String sUsername = null;
    private static String sPassword = null;
    private static String sResourceId = null;
    private static String sClientId = null;
    private static String sAuthorityUrl = null;
    
    @Override
    public void onCreate(Bundle arguments) {
        if (arguments.containsKey(SERVICE_AUTH)) {
            sAuthType = AuthType.fromString(arguments.getString(SERVICE_AUTH));
        }
        if (arguments.containsKey(SERVICE_ROOT_URL)) {
            sRootUrl = arguments.getString(SERVICE_ROOT_URL);
        } 
        if (arguments.containsKey(SERVICE_USERNAME)) {
            sUsername = arguments.getString(SERVICE_USERNAME);
        }
        if (arguments.containsKey(SERVICE_PASSWORD)) {
            sPassword = arguments.getString(SERVICE_PASSWORD);
        } 
        if (arguments.containsKey(SERVICE_RESOURCE_ID)) {
            sResourceId = arguments.getString(SERVICE_RESOURCE_ID);
        } 
        if (arguments.containsKey(SERVICE_CLIENT_ID)) {
            sClientId = arguments.getString(SERVICE_CLIENT_ID);
        } 
        if (arguments.containsKey(SERVICE_AUTHORITY_URL)) {
            sAuthorityUrl = arguments.getString(SERVICE_AUTHORITY_URL);
        }
        super.onCreate(arguments);
    }

    public static AuthType getAuthType() {
        return sAuthType;
    }
    
    public static String getRootUrl() {
        return sRootUrl;
    }
    
    public static String getUsername() {
        return sUsername;
    }
    
    public static String getPassword() {
        return sPassword;
    }
    
    public static String getResourceId() {
        return sResourceId;
    }
    
    public static String getClientId() {
        return sClientId;
    }
    
    public static String getAuthorityUrl() {
        return sAuthorityUrl;
    }
}
