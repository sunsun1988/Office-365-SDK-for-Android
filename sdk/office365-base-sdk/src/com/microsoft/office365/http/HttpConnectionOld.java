/*******************************************************************************
 * Copyright (c) Microsoft Open Technologies, Inc.
 * All Rights Reserved
 * See License.txt in the project root for license information.
 ******************************************************************************/
package com.microsoft.office365.http;


/**
 * Interface that defines a generic HttpConnection
 */
public interface HttpConnectionOld {
	
	/**
	 * Executes an request
	 * @param request The request to execute
	 * @param responseCallback The callback to invoke when the response is returned
	 * @return A Future for the operation
	 */
	public HttpConnectionFutureOld execute(final Request request);
}
