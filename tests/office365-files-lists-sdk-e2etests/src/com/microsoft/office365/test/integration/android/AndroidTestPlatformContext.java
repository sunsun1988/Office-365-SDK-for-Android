package com.microsoft.office365.test.integration.android;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.office365.Action;
import com.microsoft.office365.LogLevel;
import com.microsoft.office365.Logger;
import com.microsoft.office365.OfficeClient;
import com.microsoft.office365.OfficeFuture;
import com.microsoft.office365.files.FileClient;
import com.microsoft.office365.http.CookieCredentials;
import com.microsoft.office365.http.OAuthCredentials;
import com.microsoft.office365.http.SharepointCookieCredentials;
import com.microsoft.office365.lists.SharepointListsClient;
import com.microsoft.office365.test.integration.TestPlatformContext;
import com.microsoft.office365.test.integration.framework.TestCase;
import com.microsoft.office365.test.integration.framework.TestExecutionCallback;
import com.microsoft.office365.test.integration.framework.TestResult;

public class AndroidTestPlatformContext implements TestPlatformContext {

	private static Activity mActivity;

	public AndroidTestPlatformContext(Activity activity) {
		mActivity = activity;
	}

	@Override
	public Logger getLogger() {
		return new Logger() {

			@Override
			public void log(String message, LogLevel level) {
				Log.d(Constants.TAG, level.toString() + ": " + message);
			}
		};
	}

	@Override
	public String getServerUrl() {
		return PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
				Constants.PREFERENCE_SHAREPOINT_URL, "");
	}

	@Override
	public String getTestListName() {
		return PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
				Constants.PREFERENCE_LIST_NAME, "");
	}

	@Override
	public String getSiteRelativeUrl() {
		return PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
				Constants.PREFERENCE_SITE_URL, "");
	}

	@Override
	public Future<Void> showMessage(final String message) {
		final OfficeFuture<Void> result = new OfficeFuture<Void>();

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

				builder.setTitle("Message");
				builder.setMessage(message);
				builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						result.setResult(null);
					}
				});

				builder.create().show();
			}
		});

		return result;
	}

	@Override
	public void executeTest(final TestCase testCase, final TestExecutionCallback callback) {
		AsyncTask<Void, Void, TestResult> task = new AsyncTask<Void, Void, TestResult>() {

			@Override
			protected TestResult doInBackground(Void... params) {
				return testCase.executeTest();
			}

			@Override
			protected void onPostExecute(TestResult result) {
				callback.onTestComplete(testCase, result);
			}
		};

		task.execute();
	}

	@Override
	public void sleep(int seconds) throws Exception {
		Thread.sleep(seconds * 1000);
	}

	//	@Override
	//	public SharepointListsClient getListsClient() {
	//
	//		final OfficeFuture<SharepointListsClient> clientFuture = new OfficeFuture<SharepointListsClient>();
	//
	//		mActivity.runOnUiThread(new Runnable() {
	//
	//			@Override
	//			public void run() {
	//				OfficeFuture<CookieCredentials> future = SharepointCookieCredentials
	//						.requestCredentials(getServerUrl(), mActivity);
	//
	//				future.done(new Action<CookieCredentials>() {
	//
	//					@Override
	//					public void run(CookieCredentials credentials) throws Exception {
	//						SharepointListsClient client = new SharepointListsClient(getServerUrl(),
	//								getSiteRelativeUrl(), credentials, getLogger());
	//						clientFuture.setResult(client);
	//					}
	//				});
	//
	//			}
	//		});
	//
	//		try {
	//			return clientFuture.get();
	//		} catch (Throwable t) {
	//			Log.e(Constants.TAG, t.getMessage());
	//			return null;
	//		}
	//	}
	//
	//	@Override
	//	public FileClient getFileClient() {
	//		
	//		final OfficeFuture<FileClient> clientFuture = new OfficeFuture<FileClient>();
	//
	//		mActivity.runOnUiThread(new Runnable() {
	//
	//			@Override
	//			public void run() {
	//				OfficeFuture<CookieCredentials> future = SharepointCookieCredentials
	//						.requestCredentials(getServerUrl(), mActivity);
	//
	//				future.done(new Action<CookieCredentials>() {
	//
	//					@Override
	//					public void run(CookieCredentials credentials) throws Exception {
	//						FileClient client = new FileClient(getServerUrl(),
	//								getSiteRelativeUrl(), credentials, getLogger());
	//						clientFuture.setResult(client);
	//					}
	//				});
	//
	//			}
	//		});
	//
	//		try {
	//			return clientFuture.get();
	//		} catch (Throwable t) {
	//			Log.e(Constants.TAG, t.getMessage());
	//			return null;
	//		}
	//	}

	public static AuthenticationContext context = null;
	public AuthenticationContext getAuthenticationContext() {
		
		try {
			context = new AuthenticationContext(mActivity, "https://login.windows.net/common", false);
		} catch (Exception e) {
		}

		return context;
	}

	@Override
	public SharepointListsClient getListsClient() {
		final OfficeFuture<SharepointListsClient> future = new OfficeFuture<SharepointListsClient>();

		try {
			//here we get the token using ADAL Library
			getAuthenticationContext().acquireToken(
					mActivity, 
<<<<<<< HEAD
					"https://msopentechandroidtest.sharepoint.com", //	resourceId,
=======
					"msopentechandroidtest.onmicrosoft.com", //	resourceId,
>>>>>>> 6fe6be20cf5d66216aafaefb187c42f18c7e331a
					"da146996-bb8c-45f4-a054-bdecba247cb6",//Constants.CLIENT_ID, 
					"http://msopentechtest.com", //Constants.REDIRECT_URL, 
					"",
					new AuthenticationCallback<AuthenticationResult>() {

						@Override
						public void onError(Exception exc) {
							future.triggerError(exc);
						}

						@Override
						public void onSuccess(AuthenticationResult result) {
							//once succedded we create a credentials instance using the token from ADAL
							OAuthCredentials credentials = new OAuthCredentials(result
									.getAccessToken());

							//retrieve the OfficeClient with the credentials
							SharepointListsClient client = new SharepointListsClient(getServerUrl(),
									getSiteRelativeUrl(), credentials, getLogger());
							future.setResult(client);
						}
					});

		} catch (Throwable t) {
			future.triggerError(t);
		}

		try {
			return future.get();
		} catch (Throwable t) {
			Log.e(Constants.TAG, t.getMessage());
			return null;
		}
	}

	@Override
	public FileClient getFileClient() {
		final OfficeFuture<FileClient> future = new OfficeFuture<FileClient>();

		try {
			getAuthenticationContext().acquireToken(
					mActivity, 
					"https://contosomotors.sharepoint.com", //	resourceId,"msopentechandroidtest.onmicrosoft.com",//
					"da146996-bb8c-45f4-a054-bdecba247cb6",//Constants.CLIENT_ID, 
					"http://msopentechtest.com", //Constants.REDIRECT_URL, 
					"",

					new AuthenticationCallback<AuthenticationResult>() {

						@Override
						public void onError(Exception exc) {
							future.triggerError(exc);
						}

						@Override
						public void onSuccess(AuthenticationResult result) {
							OAuthCredentials credentials = new OAuthCredentials(result
									.getAccessToken());

							FileClient client =new FileClient(getServerUrl(),
										getSiteRelativeUrl(), credentials, getLogger());
							future.setResult(client);
						}
					});

		} catch (Throwable t) {
			future.triggerError(t);
		}
		try {
			return future.get();
		} catch (Throwable t) {
			Log.e(Constants.TAG, t.getMessage());
			return null;
		}
	}
	
//	@Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//       mActivity.o	super.onActivityResult(requestCode, resultCode, data);
//    
//        context.onActivityResult(requestCode, resultCode, data);
//    }
}
