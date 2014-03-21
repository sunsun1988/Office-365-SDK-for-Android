/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery;

import org.json.JSONObject;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.microsoft.assetmanagement.R;
import com.microsoft.filediscovery.adapters.ServiceItemAdapter;
import com.microsoft.filediscovery.tasks.RetrieveServicesTask;
import com.microsoft.filediscovery.viewmodel.ServiceViewItem;

// TODO: Auto-generated Javadoc
/**
 * The Class ServiceListActivity.
 */
public class ServiceListActivity extends FragmentActivity {

	/** The m list view. */
	private ListView mListView;

	/** The m application. */
	private AssetApplication mApplication;

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_lists);

		new RetrieveServicesTask(ServiceListActivity.this).execute();

		mApplication = (AssetApplication) getApplication();
		mListView = (ListView) findViewById(R.id.list);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View arg1, int position, long arg3) {

				final ServiceViewItem serviceItem = (ServiceViewItem) mListView.getItemAtPosition(position);

				if(serviceItem.Selectable){
					try {
						
						mApplication.authenticate(ServiceListActivity.this, 
								((ServiceViewItem) mListView.getItemAtPosition(position))
								.ResourceId);//.get();
					} catch (Throwable t) {
						Log.e("Asset", t.getMessage());
					}				
					openSelectedService(position,serviceItem);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home: {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		case R.id.menu_refresh: {
			new RetrieveServicesTask(ServiceListActivity.this).execute();
			return true;
		}
		default:
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		NavUtils.navigateUpFromSameTask(this);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.service_list_menu, menu);
		return true;
	}

	/**
	 * Sets the list adapter.
	 *
	 * @param adapter the new list adapter
	 */
	public void setListAdapter(ServiceItemAdapter adapter) {
		mListView.setAdapter(adapter);
	}

	/**
	 * Open selected car.
	 *
	 * @param position the position
	 */
	public void openSelectedService(int position,ServiceViewItem serviceItem ) {

		Intent intent = new Intent(mApplication, FileListActivity.class);
		JSONObject payload = new JSONObject();
		try {
			payload.put("resourseId", serviceItem.ResourceId);
			payload.put("endpoint", serviceItem.EndpointUri);
			intent.putExtra("data", payload.toString());
			startActivity(intent);
		} catch (Throwable t) {
			mApplication.handleError(t);
		}
	}
}
