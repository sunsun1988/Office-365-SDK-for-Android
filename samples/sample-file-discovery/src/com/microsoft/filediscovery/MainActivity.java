/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery;

import java.util.Map;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.microsoft.assetmanagement.R;
import com.microsoft.office365.Action;
import com.microsoft.office365.Credentials;

// TODO: Auto-generated Javadoc
/**
 * The Class MainActivity.
 */
public class MainActivity extends Activity {

	/** The m application. */
	private AssetApplication mApplication;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mApplication = (AssetApplication) getApplication();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		try {
			mApplication.authenticate(this, Constants.DISCOVERY_RESOURCE_ID).done(new Action<Map<String,Credentials>>() {
				@Override
				public void run(Map<String,Credentials> obj) throws Exception {
					startActivity(new Intent(MainActivity.this,
							ServiceListActivity.class));
				}
			});
		} catch (Throwable t) {
			Log.e("Asset", t.getMessage());
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		mApplication.context.onActivityResult(requestCode, resultCode, data);
	}
}
