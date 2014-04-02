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

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Events;
import com.microsoft.exchange.services.odata.model.ICalendars;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.ICalendar;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataProperty;

public class EventsAsyncTestCase extends AbstractAsyncTest {
    private static ICalendar calendar = null;

    private ODataEntity sourceEvent;

    private IEvent event;
    
    @BeforeClass
    public static void retrieveCalendar() throws Exception {
        final ICalendars cals = Me.getCalendars();
        final CountDownLatch cdl = new CountDownLatch(1);
        // an empty iterator will be returned for any entity set unless you call fetch()
        Futures.addCallback(cals.fetchAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                Iterator<ICalendar> iterator = cals.iterator();
                if (iterator.hasNext()) {
                    calendar = iterator.next();
                }
                cdl.countDown();
            }
        });
        
        cdl.await(60000, TimeUnit.MILLISECONDS);
        if (calendar == null) {
            fail("No calendar found");
        }
    }
    
    @Test(timeout = 60000)
    public void createTest() throws Exception {
        createAndCheck();
        removeEvent();
    }
    
    @Test(timeout = 60000)
    public void readTest() throws Exception {
        prepareEvent();
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
               try {
                   readAndCheck();
                   removeEvent();
               } catch (Throwable t) {
                   reportError(t);
               }
               
               counter.countDown();
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void updateTest() throws Exception {
        prepareEvent();
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
               try {
                   updateAndCheck();
                   removeEvent();
               } catch (Throwable t) {
                   reportError(t);
               }
               
               counter.countDown();
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void deleteTest() throws Exception {
        prepareEvent();
        Me.flush();
        deleteAndCheck();
    }
    
    private void deleteAndCheck() throws Exception {
        removeEvent();
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getEvents().getAsync(event.getId()), new FutureCallback<IEvent>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IEvent result) {
                try {
                    assertNull(result);
                } catch (Throwable t) {
                    reportError(t);
                }
                
                cdl.countDown();
            }
        });
        cdl.await();
    }
    
    private void updateAndCheck() throws Exception {
        final String newSubject = "new subject";
        event.setSubject(newSubject);
        Me.flush();
        assertEquals(newSubject, event.getSubject());
        // reread an event to make sure changes were sent to server
        event = Me.getEvents().getAsync(event.getId()).get();
        assertEquals(newSubject, event.getSubject());
    }
    
    private void readAndCheck() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getEvents().getAsync(event.getId()), new FutureCallback<IEvent>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IEvent result) {
                try {
                    event = result;
                    Class<?> cls = event.getClass();
                    Class<?>[] emptyParametersArray = new Class<?>[0];
                    for (ODataProperty property: sourceEvent.getProperties()) {
                        try {
                            Method getter = cls.getMethod("get" + property.getName(), emptyParametersArray);
                            assertEquals(getter.invoke(event), property.getPrimitiveValue().toValue());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (Throwable t) {
                    reportError(t);
                }
                
                cdl.countDown();
            }
        });
        cdl.await();
    }
    
    private void createAndCheck() throws Exception {
        prepareEvent();
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                try {
                    assertTrue(StringUtils.isNotEmpty(EventsAsyncTestCase.this.event.getId()));
                } catch (Throwable t) {
                    reportError(t);
                }
                
                cdl.countDown();
            }
        });
        
        cdl.await();
    }
    
    private void prepareEvent() {
        sourceEvent = getEntityFromResource("testEvent.json");
        event = Events.newEvent(calendar);
        event.setSubject(sourceEvent.getProperty("Subject").getPrimitiveValue().toString());
    }
    
    private void removeEvent() {
        Me.getEvents().delete(event.getId());
        Me.flush();
    }
}
