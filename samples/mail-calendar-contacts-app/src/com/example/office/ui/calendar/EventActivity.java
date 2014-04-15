/**
 * Copyright � Microsoft Open Technologies, Inc.
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
package com.example.office.ui.calendar;

import android.app.ActionBar;
import android.os.Bundle;

import com.example.office.R;
import com.example.office.auth.OfficeAuthenticator;
import com.example.office.logger.Logger;
import com.example.office.ui.BaseActivity;

/**
 * Activity managing specific email details.
 */
public class EventActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_activity);
        try {
            ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
            mAuthenticator = new OfficeAuthenticator(this);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreate(): Error.");
        }
    }

    @Override
    protected EventFragment getCurrentFragment() {
        return (EventFragment) getFragmentManager().findFragmentById(R.id.event_details);
    }
    
    @Override
    public void onAuthenticated() {
        getCurrentFragment().attachFileToEvent();
    }

}
