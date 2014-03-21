/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information.
 ******************************************************************************/
package com.microsoft.office365.http;

import java.util.concurrent.Executors;

import com.microsoft.office365.Platform;

/**
 * Java HttpConnection implementation, based on HttpURLConnection and threads
 * async operations
 */
public class JavaHttpConnection implements HttpConnection {

	/**
	 * User agent header name
	 */
	private static final String USER_AGENT_HEADER = "User-Agent";

	@Override
	public HttpConnectionFuture execute(final Request request) {

		request.addHeader(USER_AGENT_HEADER, Platform.getUserAgent());

		final HttpConnectionFuture future = new HttpConnectionFuture();
		final NetworkRunnable target = new NetworkRunnable(request, future);

		final NetworkThread networkThread = new NetworkThread(target) {
			@Override
			void releaseAndStop() {
				try {
					target.closeStreamAndConnection();
				} catch (Throwable error) {
				}
			}
		};

		future.addListener(new Runnable() {

			@Override
			public void run() {
				if (future.isCancelled()) {
					networkThread.releaseAndStop();
				}
			}
		}, Executors.newCachedThreadPool());

		networkThread.start();
		return future;
	}
}
