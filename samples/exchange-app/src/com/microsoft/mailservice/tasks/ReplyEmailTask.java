/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information. 
 ******************************************************************************/
package com.microsoft.mailservice.tasks;

import microsoft.exchange.services.odata.model.Message;
import com.microsoft.mailservice.MainActivity;
import com.microsoft.mailservice.SendMailActivity;
import com.microsoft.office365.Credentials;
import com.microsoft.office365.exchange.MailClient;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

// TODO: Auto-generated Javadoc
/**
 * The Class ReplyEmailTask.
 */
public class ReplyEmailTask extends AsyncTask<Message, Void, Message> {

	private String mComments;
	/** The m dialog. */
	private ProgressDialog mDialog;

	/** The m context. */
	private Context mContext;

	/** The m activity. */
	private SendMailActivity mActivity;

	/** The m stored rotation. */
	private int mStoredRotation;

	static Credentials mCredentials;

	public ReplyEmailTask(SendMailActivity activity, Credentials crendential, String comments) {
		mActivity = activity;
		mContext = activity;
		mDialog = new ProgressDialog(mContext);
		mCredentials = crendential;
		mComments = comments;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPreExecute()
	 */
	protected void onPreExecute() {

		mStoredRotation = mActivity.getRequestedOrientation();
		mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

		mDialog.setTitle("Retrieving Messages...");
		mDialog.setMessage("Please wait.");
		mDialog.setCancelable(false);
		mDialog.setIndeterminate(true);
		mDialog.show();
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(Message message) {
		if (mDialog.isShowing()) {
			mDialog.dismiss();
			mActivity.setRequestedOrientation(mStoredRotation);
		}

		if (message != null) {
			//MessageItemAdapter adapter = new MessageItemAdapter(mActivity, message);
			//mActivity.setListAdapter(adapter);
			//adapter.notifyDataSetChanged();
			Toast.makeText(mContext, "Finished Sending Mail", Toast.LENGTH_LONG).show();

			NavUtils.navigateUpTo(mActivity,new Intent(mActivity, MainActivity.class));
		} else {
			//mApplication.handleError(mThrowable);
		}
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	protected Message doInBackground(final Message... args) {
		Message messageSend = new Message();
		try {
			MailClient client = new MailClient(mCredentials);

			client.reply(args[0]).get();
			//client.send(messageId).get();
		} catch (Exception e) {
		}

		return messageSend;
	}
}
