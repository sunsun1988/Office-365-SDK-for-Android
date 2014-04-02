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
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IContact;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataProperty;

public class ContactsAsyncTestCase extends AbstractAsyncTest {

    private IContact contact;

    private ODataEntity sourceContact;
    
    @Test(timeout = 60000)
    public void createTest() throws Exception {
        createAndCheck();
        // clean-up
        removeContact();
    }
    
    @Test(timeout = 60000)
    public void readTest() throws Exception {
        // create contact first
        prepareContact();
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
                    // clean-up
                    removeContact();
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
        counter = new CountDownLatch(1);
        // create contact first
        prepareContact();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            public void onSuccess(Void result) {
                try {
                    updateAndCheck();
                    // clean up
                    removeContact();
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            };
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void deleteTest() throws Exception {
        // create contact first
        prepareContact();
        Me.flush();
        // then remove
        deleteAndCheck();
    }
    
    private void deleteAndCheck() throws Exception {
        removeContact();
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getContacts().getAsync(contact.getId()), new FutureCallback<IContact>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IContact result) {
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
        final String newName = "new name";
        contact.setGivenName(newName);
        Me.flushAsync().get();
        assertEquals(newName, contact.getGivenName());
        // ensure that changes were pushed to endpoint
        contact = Me.getContacts().getAsync(contact.getId()).get();
        assertEquals(newName, contact.getGivenName());
    }
    
    private void readAndCheck() throws Exception {
        // reread a contact
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getContacts().getAsync(contact.getId()), new FutureCallback<IContact>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IContact result) {
                contact = result;
                try {
                    Class<?> cls = contact.getClass();
                    Class<?>[] emptyParametersArray = new Class<?>[0];
                    for (ODataProperty property : sourceContact.getProperties()) {
                        try {
                            Method getter = cls.getMethod("get" + property.getName(), emptyParametersArray);
                            assertEquals(property.getPrimitiveValue().toValue(), getter.invoke(contact));
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
        prepareContact();
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
                    assertTrue(StringUtils.isNotEmpty(contact.getId()));
                } catch (Throwable t) {
                    reportError(t);
                }
                
                cdl.countDown();
            }
        });
        
        cdl.await();
    }
    
    private void prepareContact() {
        sourceContact = getEntityFromResource("testContact.json");
        contact = Me.getContacts().newContact();
        contact.setGivenName((String)sourceContact.getProperty("GivenName").getPrimitiveValue().toValue());
    }
    
    private void removeContact() {
        Me.getContacts().delete(contact.getId());
        Me.flush();
    }
}
