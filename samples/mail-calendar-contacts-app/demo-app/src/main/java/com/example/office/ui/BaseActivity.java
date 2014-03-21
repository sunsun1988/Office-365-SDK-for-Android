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
package com.example.office.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.example.office.Constants;
import com.example.office.R;
import com.example.office.auth.AbstractOfficeAuthenticator;
import com.example.office.auth.AuthType;
import com.example.office.auth.OfficeCredentials;
import com.example.office.storage.AuthPreferences;
import com.example.office.ui.fragments.AuthFragment;

/**
 * Base class for activities.
 * Enables indeterminate progress bar and 'Up' navigation via ActionBar.
 */
public abstract class BaseActivity extends Activity {
    
    /**
     * Oauth2 office authenticator.
     */
    protected static AbstractOfficeAuthenticator mAuthenticator;    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: handle API version
        // Needs to be called before setting the content view
        // supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Support "Up Navigation" for ActionBar
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent upIntent = getParentActivityIntent();
            if (shouldUpRecreateTask(upIntent)) {
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(upIntent)
                        .startActivities();
            } else {
                navigateUpTo(upIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_common, menu);
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Propagate to current fragment
        AuthFragment fragment = getCurrentFragment();
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    /**
     * Sets new authenticator globally for application.
     * 
     * @param authenticator new authenticator.
     */
    protected void setAuthenticator(AbstractOfficeAuthenticator authenticator) {
        mAuthenticator = authenticator;
        com.microsoft.office.core.Configuration.setAuthenticator(authenticator);
    }
    

    /**
     * Creates and returns new credentials.
     * 
     * @return created credentials instance.
     */
    protected OfficeCredentials createNewCredentials() {
        OfficeCredentials creds = new OfficeCredentials(Constants.AUTHORITY_URL, Constants.CLIENT_ID, Constants.RESOURCE_ID, Constants.REDIRECT_URL);
        creds.setUserHint(Constants.USER_HINT);
        creds.setAuthType(AuthType.OAUTH);
        AuthPreferences.storeCredentials(creds);
        return creds;
    }
    
    /**
     * Returns currently displayed fragment.
     * 
     * @return fragment which user interacts with.
     */
    protected abstract AuthFragment getCurrentFragment();
    
    public abstract AbstractOfficeAuthenticator getAuthenticator();

}
