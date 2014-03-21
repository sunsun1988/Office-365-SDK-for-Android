/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information.
 ******************************************************************************/
package com.microsoft.office365.http;

import com.microsoft.office365.PlatformOld;


/**
 * Java HttpConnection implementation, based on HttpURLConnection and 
 * threads async operations
 */
public class JavaHttpConnectionOld implements HttpConnectionOld{
	
    /**
     * User agent header name
     */
    private static final String USER_AGENT_HEADER = "User-Agent";


    @Override
    public HttpConnectionFutureOld execute(final Request request) {
        
        request.addHeader(USER_AGENT_HEADER, PlatformOld.getUserAgent());
        
        final HttpConnectionFutureOld future = new HttpConnectionFutureOld();
        final NetworkRunnableOld target = new NetworkRunnableOld(request, future);
        
        final NetworkThread networkThread = new NetworkThread(target) {
        	@Override
        	void releaseAndStop() {
        		try {
        			target.closeStreamAndConnection();
        		} catch (Throwable error) {
        		}
        	}
        };
        
        future.onCancelled(new Runnable() {
			
			@Override
			public void run() {
				networkThread.releaseAndStop();
			}
		});

        networkThread.start();

        return future;
    }
}
