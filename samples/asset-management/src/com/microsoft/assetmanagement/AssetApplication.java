/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.assetmanagement;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.assetmanagement.files.SharepointListsClientWithFiles;
import com.microsoft.office365.Credentials;
import com.microsoft.office365.LogLevel;
import com.microsoft.office365.Logger;
import com.microsoft.office365.files.FileClient;
import com.microsoft.office365.http.BasicAuthenticationCredentials;
import com.microsoft.office365.http.CookieCredentials;
import com.microsoft.office365.http.OAuthCredentials;
import com.microsoft.office365.http.SharepointCookieCredentials;
import com.microsoft.office365.lists.SharepointListsClient;

// TODO: Auto-generated Javadoc
/**
 * The Class AssetApplication.
 */
public class AssetApplication extends Application {

	/** The app context. */
	private static Context appContext;

	/** The m preferences. */
	private AssetPreferences mPreferences;

	/** The m credentials. */
	private Credentials mCredentials;

	/** The m sharepoint lists client. */
	private SharepointListsClient mSharepointListsClient;

	/** The m fileClient lists client. */
	private FileClient mFileClient;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {

		Log.d("Asset Management", "onCreate");
		super.onCreate();
		AssetApplication.appContext = getApplicationContext();

		mPreferences = new AssetPreferences(appContext, PreferenceManager.getDefaultSharedPreferences(this));
	}

	/**
	 * Gets the credentials.
	 * 
	 * @return the credentials
	 */
	public Credentials getCredentials() {
		return mCredentials;
	}

	/**
	 * Sets the credentials.
	 * 
	 * @param credentials
	 *            the new credentials
	 */
	public void setCredentials(Credentials credentials) {
		mCredentials = credentials;
	}

	/**
	 * Handle error.
	 * 
	 * @param throwable
	 *            the throwable
	 */
	public void handleError(Throwable throwable) {
		Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
		Log.e("Asset", throwable.toString());
	}

	/**
	 * Authenticate.
	 * 
	 * @param activity
	 *            the activity
	 * @return the office future
	 */
	public ListenableFuture<Credentials> authenticate(Activity activity) {
		final SettableFuture<Credentials> result = SettableFuture.create();

		String method = mPreferences.getAuthenticationMethod();
		if (method.equals(Constants.AUTHENTICATIONMETHOD_COOKIES)) {
			ListenableFuture<CookieCredentials> future = SharepointCookieCredentials.requestCredentials(
					mPreferences.getSharepointServer(), activity);

			Futures.addCallback(future, new FutureCallback<CookieCredentials>() {
				@Override
				public void onFailure(Throwable t) {
					result.setException(t);
				}

				@Override
				public void onSuccess(CookieCredentials credentials) {
					mCredentials = credentials;
					result.set(credentials);
				}
			});
		}else if (method.equals(Constants.AUTHENTICATIONMETHOD_AAD)) {
			getAuthenticationContext(activity).acquireToken(
					activity, mPreferences.getSharepointServer(),
					mPreferences.getClientId(),mPreferences.getRedirectUrl(), PromptBehavior.Auto,

					new AuthenticationCallback<AuthenticationResult>() {

						@Override
						public void onSuccess(AuthenticationResult authenticationResult) {
							// once succeeded we create a credentials instance
							// using the token from ADAL
							mCredentials = new OAuthCredentials(authenticationResult.getAccessToken());
							result.set(mCredentials);
						}

						@Override
						public void onError(Exception exc) {
							result.setException(exc);
						}
					});
		} else {
			String userName = mPreferences.getNTLMUser();
			String password = mPreferences.getNTLMPassword();
			mCredentials = new BasicAuthenticationCredentials(userName, password);
			result.set(mCredentials);
		}
		return result;
	}

	public AuthenticationContext context = null;

	/**
	 * Gets AuthenticationContext for AAD.
	 * 
	 * @return authenticationContext, if successful
	 */
	public AuthenticationContext getAuthenticationContext(Activity activity) {

		try {
			context = new AuthenticationContext(activity, Constants.AUTHORITY_URL, false);
		} catch (Exception e) {
		}

		return context;
	}

	/**
	 * Checks for configuration settings.
	 * 
	 * @return true, if successful
	 */
	public boolean hasConfigurationSettings() {

		String authenticationMethod = mPreferences.getAuthenticationMethod();
		if (isNullOrEmpty(authenticationMethod))
			return false;

		if (isNullOrEmpty(mPreferences.getLibraryName()))
			return false;
		if (authenticationMethod.equals(Constants.AUTHENTICATIONMETHOD_NTLM)) {
			String server = mPreferences.getSharepointServer();
			String username = mPreferences.getNTLMUser();
			String password = mPreferences.getNTLMPassword();

			boolean result = (!isNullOrEmpty(server)) && (!isNullOrEmpty(username)) && (!isNullOrEmpty(password));
			return result;
		} else if (authenticationMethod.equals(Constants.AUTHENTICATIONMETHOD_COOKIES)
				|| authenticationMethod.equals(Constants.AUTHENTICATIONMETHOD_AAD)) {
			return (!isNullOrEmpty(mPreferences.getSharepointServer()) && (!isNullOrEmpty(mPreferences
					.getSiteRelativeUrl())));
		} else {
			String authorityUrl = mPreferences.getAuthorityUrl();
			String clientId = mPreferences.getClientId();
			String resourceUrl = mPreferences.getRedirectUrl();
			String userHint = mPreferences.getUserHint();
			boolean result = (!isNullOrEmpty(authorityUrl)) && (!isNullOrEmpty(clientId))
					&& (!isNullOrEmpty(resourceUrl)) && (!isNullOrEmpty(userHint));
			return result;
		}
	}

	/**
	 * Checks if is null or empty.
	 * 
	 * @param value
	 *            the value
	 * @return true, if is null or empty
	 */
	private boolean isNullOrEmpty(String value) {

		return value == null || value.length() == 0;
	}

	/**
	 * Store site url.
	 * 
	 * @param url
	 *            the url
	 * @return the boolean
	 */
	public Boolean storeSiteUrl(String url) {
		mPreferences.storeSharepointListUrl(url);
		return true;
	}

	/**
	 * Gets the stored lists.
	 * 
	 * @return the stored lists
	 */
	public ArrayList<String> getStoredLists() {
		return mPreferences.getSharepointListNames();
	}

	/**
	 * Checks for default list.
	 * 
	 * @return true, if successful
	 */
	public boolean hasDefaultList() {
		return mPreferences.getLibraryName() != null;
	}

	/**
	 * Gets the preferences.
	 * 
	 * @return the preferences
	 */
	public AssetPreferences getPreferences() {
		return mPreferences;
	}

	/**
	 * Clear preferences.
	 */
	public void clearPreferences() {
		// mPreferences.clear();
		CookieSyncManager syncManager = CookieSyncManager.createInstance(this);
		if (syncManager != null) {
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookie();
		}
	}

	/**
	 * Gets the current list client.
	 * 
	 * @return the current list client
	 */
	public SharepointListsClient getCurrentListClient() {
		String serverUrl = mPreferences.getSharepointServer();
		String siteRelativeUrl = mPreferences.getSiteRelativeUrl();
		Credentials credentials = getCredentials();
		mSharepointListsClient = new SharepointListsClientWithFiles(serverUrl, siteRelativeUrl, credentials,
				new Logger() {

			@Override
			public void log(String message, LogLevel level) {
				Log.d("Asset", message);
			}
		});
		return mSharepointListsClient;
	}

	/**
	 * Gets the account info.
	 * 
	 * @return the account info
	 */
	public String getAccountInfo() {
		SharepointListsClient client = getCurrentListClient();
		try {
			return client.getUserProperties().get();
		} catch (Throwable t) {
			Log.d("Asset", t.getMessage());
		}
		return "";
	}

	/**
	 * Gets the current list client.
	 * 
	 * @return the current list client
	 */
	public FileClient getCurrentFileClient() {
		String serverUrl = mPreferences.getSharepointServer();
		String siteRelativeUrl = mPreferences.getSiteRelativeUrl();
		Credentials credentials = getCredentials();
		mFileClient = new FileClient(serverUrl, siteRelativeUrl, credentials, new Logger() {

			@Override
			public void log(String message, LogLevel level) {
				Log.d("Asset", message);
			}
		});
		return mFileClient;
	}
}
