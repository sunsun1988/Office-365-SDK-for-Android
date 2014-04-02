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
package com.microsoft.office.integration.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;

public class SingletonAsyncTestCase extends AbstractAsyncTest {
    
    public void testMeRetrieving() {
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.init(), new FutureCallback<Void>() {
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            public void onSuccess(Void result) {
                try {
                  assertTrue(Me.getId().equalsIgnoreCase(TestRunner.getUsername())); // as per spec  
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        try {
            if (!counter.await(60000, TimeUnit.MILLISECONDS)) {
                fail("testSize() timed out");
            }
        } catch (InterruptedException e) {
            fail("testSize() has been interrupted");
        }
    }
}
