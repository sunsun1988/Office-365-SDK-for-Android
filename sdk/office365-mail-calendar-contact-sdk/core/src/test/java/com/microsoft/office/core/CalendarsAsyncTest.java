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
    
}
