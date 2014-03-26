/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.filediscovery.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.microsoft.filediscovery.AssetApplication;
import com.microsoft.filediscovery.datasource.ListItemsDataSource;
import com.microsoft.filediscovery.viewmodel.FileItem;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
// TODO: Auto-generated Javadoc
/**
 * The Class DownloadFileTask.
 */
public class DownloadFileTask extends AsyncTask<FileItem, Void, FileItem> {

	/** The m source. */
	private ListItemsDataSource mSource;

	/** The m activity. */
	private Activity mActivity;

	/** The m dialog. */
	private ProgressDialog mDialog;

	/** The m application. */
	private AssetApplication mApplication;

	/** The m throwable. */
	private Throwable mThrowable;
	
	/**
	 * Instantiates a new save car task.
	 *
	 * @param activity the activity
	 */
	public DownloadFileTask(Activity activity) {
		mActivity = activity;
		mDialog = new ProgressDialog(mActivity);
		mApplication = (AssetApplication) activity.getApplication();
		mSource = new ListItemsDataSource(mApplication);
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	protected void onPreExecute() {
		mDialog.setTitle("Downloading file...");
		mDialog.setMessage("Please wait.");
		mDialog.setCancelable(false);
		mDialog.setIndeterminate(true);
		mDialog.show();
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(FileItem result) {
		if (mDialog.isShowing()) {
			mDialog.dismiss();
		}

		if (mThrowable == null) {
			
			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

			File file = new File(path, result.Id);
					
			if (file.exists()) { file.delete(); }
			
			try {
				file.createNewFile();
				FileOutputStream fos= new FileOutputStream(file);
			
				fos.write(result.Content);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Intent targetIntent = new Intent(Intent.ACTION_VIEW);

			Uri uri = Uri.parse("file://" + path + "/" + result.Id);
			targetIntent.setDataAndType(uri, "image/*");
			targetIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			
			Intent intent = Intent.createChooser(targetIntent, "Open File");
			
			mActivity.startActivity(intent);

		} else {
			mApplication.handleError(mThrowable);
		}
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected FileItem doInBackground(FileItem... params) {
		FileItem viewItem = params[0];
		
		if (viewItem != null) {
			try {
				viewItem.Content = mSource.getFile(viewItem).get();
			} catch (Throwable t) {
				mThrowable = t;
			}
		} else {
			mThrowable = new IllegalArgumentException(
					"params argument must contain at least a CarListViewItem");
		}
		return viewItem;
	}
}
