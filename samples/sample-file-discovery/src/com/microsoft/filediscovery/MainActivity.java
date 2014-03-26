/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery;

import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.assetmanagement.R;
import com.microsoft.office365.Credentials;
// TODO: Auto-generated Javadoc
/**
 * The Class MainActivity.
 */
public class MainActivity extends Activity {

	/** The m application. */
	private AssetApplication mApplication;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mApplication = (AssetApplication) getApplication();
		
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		
		Uri uri = null;
		
		try {
			uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
		} catch (Throwable t) {
		
		}

		if(uri != null){
			StartServiceListActivity(uri.getPath());
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
			StartServiceListActivity("");
		} catch (Throwable t) {
			Log.e("Asset", t.getMessage());
		}
		return true;
	}

	void StartServiceListActivity(final String uri){

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
				
				if(uri.length() > 0){
				
					JSONObject payload = new JSONObject();
					try {
						payload.put("shareUri", uri);
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
