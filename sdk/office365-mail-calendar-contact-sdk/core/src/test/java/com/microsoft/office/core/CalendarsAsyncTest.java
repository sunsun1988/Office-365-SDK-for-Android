/**
 * Copyright © Microsoft Open Technologies, Inc.
 *
 * All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 *
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 */
package com.microsoft.office.core;

import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;

public class CalendarsAsyncTest extends AbstractAsyncTest {

    @Test(timeout = 60000)
    @Ignore(value = "$count is not implemented on server side")
    public void countTest() throws Exception {
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.getCalendars().countAsync(), new FutureCallback<Long>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Long result) {
                try {
                    assertTrue(result > 0); // at least one calendar always exists
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void sizeTest() throws Exception {
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.getCalendars().fetchAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                try {
                    assertTrue(Me.getCalendars().size() > 0); // at least one calendar always exists
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        counter.await();
    }
    
}
