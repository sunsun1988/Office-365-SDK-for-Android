package com.microsoft.office365.demo;

import java.util.Random;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationSettings;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.office365.OfficeClient;
import com.microsoft.office365.files.FileClient;
import com.microsoft.office365.http.OAuthCredentials;

public class DemoApplication extends Application {
	public AuthenticationContext getAuthenticationContext() {
	    AuthenticationContext context = null;
	    try {
            context = new AuthenticationContext(this, Constants.AUTHORITY_URL, false);
        } catch (Exception e) {
        }
	    
	    return context;
	}

	@Override
	public void onCreate() {
		Log.d("Asset Management", "onCreate");
		createEncryptionKey();
		AuthenticationSettings.INSTANCE.setSecretKey(getEncryptionKey());
		super.onCreate();
	}

	private void createEncryptionKey() {
	    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

	    if (!preferences.contains(Constants.ENCRYPTION_KEY)) {
	        //generate a random key
	        Random r = new Random();
	        byte[] bytes = new byte[32];
	        r.nextBytes(bytes);
	        
	        String key = Base64.encodeToString(bytes, Base64.DEFAULT);
	        
	        Editor editor = preferences.edit();
	        editor.putString(Constants.ENCRYPTION_KEY, key);
	        editor.commit();
	    }
    }
	
	public byte[] getEncryptionKey() {
	    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    String key = preferences.getString(Constants.ENCRYPTION_KEY, null);

        if (key != null) {
            byte[] bytes = Base64.decode(key, Base64.DEFAULT);
            return bytes;
        } else {
            return new byte[32];
        }
	}

    public ListenableFuture<OfficeClient> getOfficeClient(final Activity activity, String resourceId) {
        final SettableFuture<OfficeClient> future = SettableFuture.create();

        try {
        	//here we get the token using ADAL Library
            getAuthenticationContext().acquireToken(activity, resourceId,
                    Constants.CLIENT_ID, Constants.REDIRECT_URL, PromptBehavior.Auto,
                    new AuthenticationCallback<AuthenticationResult>() {

                        @Override
                        public void onError(Exception exc) {
                            future.setException(exc);
                        }

                        @Override
                        public void onSuccess(AuthenticationResult result) {
                        	//once succedded we create a credentials instance using the token from ADAL
                            OAuthCredentials credentials = new OAuthCredentials(result
                                    .getAccessToken());
                            
                            //retrieve the OfficeClient with the credentials
                            OfficeClient client = new OfficeClient(credentials);
                            future.set(client);
                        }
                    });

        } catch (Throwable t) {
            future.setException(t);
        }
        return future;
    }
	
	public ListenableFuture<FileClient> getFileClient(final Activity activity, String resourceId, final String sharepointUrl) {
        final SettableFuture<FileClient> future = SettableFuture.create();

        try {
            getAuthenticationContext().acquireToken(activity, resourceId,
                    Constants.CLIENT_ID, Constants.REDIRECT_URL, "",
                    new AuthenticationCallback<AuthenticationResult>() {

                        @Override
                        public void onError(Exception exc) {
                            future.setException(exc);
                        }

                        @Override
                        public void onSuccess(AuthenticationResult result) {
                            OAuthCredentials credentials = new OAuthCredentials(result
                                    .getAccessToken());
                            
                            FileClient client = new FileClient(sharepointUrl, "", credentials);
                            future.set(client);
                        }
                    });

        } catch (Throwable t) {
            future.setException(t);
        }
        return future;
    }
}
