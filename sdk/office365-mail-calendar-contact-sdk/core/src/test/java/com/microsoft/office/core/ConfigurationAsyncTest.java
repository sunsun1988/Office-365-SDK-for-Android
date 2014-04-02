package com.microsoft.office.core;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;

public class ConfigurationAsyncTest extends AbstractAsyncTest {

    @Test(timeout = 60000)
    public void authorizationTest() throws Exception {
        counter = new CountDownLatch(1);
        // try any request
        Futures.addCallback(Me.init(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                try {
                    Me.getAlias();
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        
        counter.await();
    }
}
