/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery;

import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.adal.AuthenticationSettings;
import com.microsoft.assetmanagement.R;
import com.microsoft.office365.Credentials;
// TODO: Auto-generated Javadoc
/**
 * The Class MainActivity.
 */
public class MainActivity extends Activity {

	/** The m application. */
	private DiscoveryAPIApplication mApplication;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mApplication = (DiscoveryAPIApplication) getApplication();
		DiscoveryAPIApplication.mSharedUri = null;

		try {
			Intent intent = getIntent();
			Bundle bundle = intent.getExtras();
			DiscoveryAPIApplication.mSharedUri = (Uri)bundle.get(Intent.EXTRA_STREAM);
		} catch (Throwable t) {}

		createEncryptionKey();
		AuthenticationSettings.INSTANCE.setSecretKey(getEncryptionKey());
		
		if(DiscoveryAPIApplication.mSharedUri != null){
			StartServiceListActivity(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		try {
			StartServiceListActivity(false);
		} catch (Throwable t) {
			Log.e("Asset", t.getMessage());
		}
		return true;
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

	void StartServiceListActivity(final boolean shouldRedirect){

		ListenableFuture<Map<String, Credentials>> future = mApplication.authenticate(this,
				Constants.DISCOVERY_RESOURCE_ID);

		Futures.addCallback(future, new FutureCallback<Map<String, Credentials>>() {
			@Override
			public void onFailure(Throwable t) {
				Log.e("Asset", t.getMessage());
			}

			@Override
			public void onSuccess(Map<String, Credentials> credentials) {
				Intent intent = new Intent(MainActivity.this, ServiceListActivity.class);

				if(shouldRedirect){

					JSONObject payload = new JSONObject();
					try {
						payload.put("isShareUri", true);
						intent.putExtra("data", payload.toString());
					} catch (Throwable t) {
						Log.e("Asset", t.getMessage());
					}
				}
				startActivity(intent);
			}
		});
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		mApplication.context.onActivityResult(requestCode, resultCode, data);
	}
}
