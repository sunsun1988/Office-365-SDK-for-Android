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
package com.microsoft.office.integration;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TestActivity extends Activity {

    /**
     * Oauth2 office authenticator.
     */
    private static AbstractOfficeAuthenticator mOfficeAuthenticator = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mOfficeAuthenticator != null) {
            mOfficeAuthenticator.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    public static void setOfficeAuthenticator(AbstractOfficeAuthenticator authenticator) {
        mOfficeAuthenticator = authenticator;
    }
}
