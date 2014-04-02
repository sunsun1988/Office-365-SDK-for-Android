/**
 * Copyright © Microsoft Open Technologies, Inc.
 *
 * All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 *
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 */
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
    private static final String SERVICE_REDIRECT_URL = "serviceRedirectUrl";

    private static AuthType sAuthType = AuthType.UNDEFINED;
    private static String sRootUrl = null;
    private static String sUsername = null;
    private static String sPassword = null;
    private static String sResourceId = null;
    private static String sClientId = null;
    private static String sAuthorityUrl = null;
    private static String sRedirectUrl = null;
    
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
        if (arguments.containsKey(SERVICE_REDIRECT_URL)) {
            sRedirectUrl = arguments.getString(SERVICE_REDIRECT_URL);
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
    
     public static String getRedirectUrl() {
        return sRedirectUrl;
     }
}
