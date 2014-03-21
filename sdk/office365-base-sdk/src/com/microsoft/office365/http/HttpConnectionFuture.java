/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information.
 ******************************************************************************/
package com.microsoft.office365.http;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.office365.ErrorCallback;

/**
 * A SinglaRFuture for Http operations
 */
public class HttpConnectionFuture implements ListenableFuture<Response> {

	private Queue<Throwable> mTimeoutQueue = new ConcurrentLinkedQueue<Throwable>();
	private ErrorCallback mTimeoutCallback;
	private Object mTimeoutLock = new Object();
	private SettableFuture<Response> mSettableFuture;

	public HttpConnectionFuture() {
		mSettableFuture = SettableFuture.create();
	}

	/**
	 * Handles the timeout for an Http operation
	 * 
	 * @param errorCallback
	 *            The handler
	 */
	public void onTimeout(ErrorCallback errorCallback) {
		synchronized (mTimeoutLock) {
			mTimeoutCallback = errorCallback;

			while (!mTimeoutQueue.isEmpty()) {
				if (mTimeoutCallback != null) {
					mTimeoutCallback.onError(mTimeoutQueue.poll());
				}
			}
		}
	}

	/**
	 * Triggers the timeout error
	 * 
	 * @param error
	 *            The error
	 */
	public void triggerTimeout(Throwable error) {
		synchronized (mTimeoutLock) {
			if (mTimeoutCallback != null) {
				mTimeoutCallback.onError(error);
			} else {
				mTimeoutQueue.add(error);
			}
		}
	}

	public boolean set(Response response) {
		return mSettableFuture.set(response);
	}

	public boolean setException(Throwable throwable) {
		return mSettableFuture.setException(throwable);
	}

	/**
	 * Represents the callback to invoke when a response is returned after a
	 * request
	 */
	public interface ResponseCallback {
		/**
		 * Callback invoked when a response is returned by the request
		 * 
		 * @param response
		 *            The returned response
		 */
		public void onResponse(Response response) throws Exception;
	}

	@Override
	public boolean cancel(boolean mayInterrupt) {
		return mSettableFuture.cancel(mayInterrupt);
	}

	@Override
	public Response get() throws InterruptedException, ExecutionException {
		return mSettableFuture.get();
	}

	@Override
	public Response get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return mSettableFuture.get(timeout, unit);
	}

	@Override
	public boolean isCancelled() {
		return mSettableFuture.isCancelled();
	}

	@Override
	public boolean isDone() {
		return mSettableFuture.isDone();
	}

	@Override
	public void addListener(Runnable listener, Executor exec) {
		mSettableFuture.addListener(listener, exec);
	}

}
