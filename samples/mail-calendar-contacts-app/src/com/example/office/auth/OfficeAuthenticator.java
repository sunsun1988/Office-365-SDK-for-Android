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
package com.example.office.auth;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.example.office.storage.AuthPreferences;
import com.example.office.ui.BaseActivity;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.office.core.auth.IOfficeCredentials;
import com.microsoft.office.core.auth.method.IAuthenticator;
import com.microsoft.office.core.net.NetworkException;
import com.msopentech.org.apache.http.client.HttpClient;
import com.msopentech.org.apache.http.client.methods.HttpUriRequest;

/**
 * Abstract implementation for credentials required to authorize to Office 365 online.
 */
public class OfficeAuthenticator implements IAuthenticator {

    /**
     * An activity passed to ADAL as a context.
     */
    protected final BaseActivity mActivity;
    
    /**
     * Gets credentials used in this authenticator.
     * 
     * @return credentials in use.
     */
    private IOfficeCredentials getCredentials() {
        IOfficeCredentials creds = AuthPreferences.loadCredentials();
        return creds == null ? mActivity.createNewCredentials() : creds;
    }

    public OfficeAuthenticator(BaseActivity activity) {
        mActivity = activity;
        mCredentials = getCredentials();
        try {
            mAuthContext = new AuthenticationContext(mActivity, mCredentials.getAuthorityUrl(), false);
        } catch (Exception e) {
            mAuthContext = null;
        }
    }

    IOfficeCredentials mCredentials;

    Runnable mUiRunnable;
    private AuthenticationContext mAuthContext = null;

    /** Authentication callback. */
    protected final AuthenticationCallback<AuthenticationResult> mCallback = new AuthenticationCallback<AuthenticationResult>() {
        @Override
        public void onSuccess(AuthenticationResult result) {
            if (result != null && !TextUtils.isEmpty(result.getAccessToken())) {
                OfficeAuthenticator.this.onDone(result);
            }
        }

        @Override
        public void onError(Exception exc) {
            OfficeAuthenticator.this.onError(exc);
        }
    };

    /**
     * Invoked when ADAL activity reports about finish.
     * 
     * @param requestCode Request code.
     * @param resultCode Result code.
     * @param data Data.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAuthContext != null) {
            mAuthContext.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void prepareRequest(HttpUriRequest request) {
        // Will be called after prepareClient that will retrieve token if non available.
        request.addHeader("Authorization", "Bearer " + mCredentials.getToken());
    }

    @Override
    public void prepareClient(final HttpClient client) throws NetworkException {
        // We do have credentials, simply pass this step. Token will be set later in prepareRequest().
        if (mCredentials != null && !StringUtils.isEmpty(mCredentials.getAuthorityUrl()) && !StringUtils.isEmpty(mCredentials.getResourceId()) && 
                !StringUtils.isEmpty(mCredentials.getClientId()) && !StringUtils.isEmpty(mCredentials.getToken())) {
            return;
        }

        mCredentials = getCredentials();

        // Should call this on UI thread b/c WebVew must be instantiated and run on UI thread only.
        try {
            mUiRunnable = new Runnable() {
                public void run() {
                    acquireToken(OfficeAuthenticator.this.mActivity);
                }
            };
            // As WebView is running on it's own thread we should block an wait until it's finished.
            synchronized (mUiRunnable) {
                OfficeAuthenticator.this.mActivity.runOnUiThread(mUiRunnable);
                mUiRunnable.wait();
            }
        } catch (Exception e) {
            // TODO: Log it
            throw new NetworkException("Authentication: Error executing access code retrieval." + e.getMessage(), e);
        }
    }

    public void onDone(AuthenticationResult result) {
        try {
            AuthPreferences.storeCredentials(mCredentials.setToken(result.getAccessToken()).setRefreshToken(result.getRefreshToken()));
            releaseUiThread();
            mActivity.onAuthenticated();
        } catch (Exception e) {
            // TODO: log it.
        }
    }

    public void onError(Throwable error) {
        releaseUiThread();
    }

    private void releaseUiThread() {
        try {
            synchronized (mUiRunnable) {
                mUiRunnable.notify();
            }
        } catch (Exception e) {
            // Ignore.
        }
    }

    /**
     * Begins authentication flow.
     * 
     * @param activity
     */
    public void acquireToken(final Activity activity) {
        try {
            mAuthContext.acquireToken(activity, mCredentials.getResourceId(), mCredentials.getClientId(),
                    mCredentials.getRedirectUrl(), mCredentials.getUserHint(), mCallback);
        } catch (Exception exc) {
            onError(exc);
        }
    }

    /**
     * Resets current authenticator preferences by clearing authentication context cache.
     */
    public void reset() {
        mAuthContext.getCache().removeAll();
    }
}
