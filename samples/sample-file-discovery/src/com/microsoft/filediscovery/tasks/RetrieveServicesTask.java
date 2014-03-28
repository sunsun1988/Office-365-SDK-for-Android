/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery.tasks;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import com.microsoft.filediscovery.DiscoveryAPIApplication;
import com.microsoft.filediscovery.ServiceListActivity;
import com.microsoft.filediscovery.adapters.ServiceItemAdapter;
import com.microsoft.filediscovery.datasource.ListItemsDataSource;
import com.microsoft.filediscovery.viewmodel.ServiceViewItem;

// TODO: Auto-generated Javadoc
/**
 * The Class RetrieveServicesTask.
 */
public class RetrieveServicesTask extends AsyncTask<String, Void, ArrayList<ServiceViewItem>> {

	/** The m dialog. */
	private ProgressDialog mDialog;
	
	/** The m context. */
	private Context mContext;
	
	/** The m activity. */
	private ServiceListActivity mActivity;
	
	/** The m source. */
	private ListItemsDataSource mSource;
	
	/** The m application. */
	private DiscoveryAPIApplication mApplication;
	
	/** The m throwable. */
	private Throwable mThrowable;
	
	/** The m stored rotation. */
	private int mStoredRotation;

	/**
	 * Instantiates a new retrieve services task.
	 *
	 * @param activity the activity
	 */
	public RetrieveServicesTask(ServiceListActivity activity) {
		mActivity = activity;
		mContext = activity;
		mDialog = new ProgressDialog(mContext);
		mApplication = (DiscoveryAPIApplication) activity.getApplication();
		mSource = new ListItemsDataSource(mApplication);
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	protected void onPreExecute() {

		mStoredRotation = mActivity.getRequestedOrientation();
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		mDialog.setTitle("Retrieving Services...");
		mDialog.setMessage("Please wait.");
		mDialog.setCancelable(false);
		mDialog.setIndeterminate(true);
		mDialog.show();
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(final ArrayList<ServiceViewItem> serviceItems) {
		if (mDialog.isShowing()) {
			mDialog.dismiss();
			mActivity.setRequestedOrientation(mStoredRotation);
		}
		if (serviceItems != null) {

			ServiceItemAdapter adapter = new ServiceItemAdapter(mActivity, serviceItems);
			mActivity.setListAdapter(adapter);
			adapter.notifyDataSetChanged();
			Toast.makeText(mContext, "Finished loading services", Toast.LENGTH_LONG).show();
		} else {
			mApplication.handleError(mThrowable);
		}
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	protected ArrayList<ServiceViewItem> doInBackground(final String... args) {
		try {
			return mSource.getServices();
		} catch (Exception e) {
			mThrowable = e;
			return null;
		}
	}
}
