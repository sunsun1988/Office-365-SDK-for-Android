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

public abstract class AbstractAsyncTest extends AbstractTest {
    
    protected CountDownLatch counter;
    protected Throwable error;
    
    protected void reportError(Throwable err) {
        error = err;
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        error = null;
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (error != null) {
            throw new Exception(error);
        }
    }
}
