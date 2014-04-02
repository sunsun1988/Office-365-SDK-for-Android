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
import com.microsoft.exchange.services.odata.model.IFolders;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataProperty;

public class FolderAsyncTestCase extends AbstractAsyncTest {
    private ODataEntity sourceFolder;

    private IFolder folder;

    @Test(timeout = 60000)
    public void createTest() throws Exception {
        createAndCheck();
        removeFolder();
    }
    
    @Test(timeout = 60000)
    public void readTest() throws Exception {
        prepareFolder();
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
                    FolderAsyncTestCase.this.readAndCheck();
                    FolderAsyncTestCase.this.removeFolder();
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
        prepareFolder();
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
                    removeFolder();
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
        prepareFolder();
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
                    deleteAndCheck();
                } catch (Throwable t) { 
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        counter.await();
    }
    
    private void deleteAndCheck() throws Exception {
        removeFolder();
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getFolders().getAsync(folder.getId()), new FutureCallback<IFolder>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IFolder result) {
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
    
    @Test
    public void moveAndCopyTest() throws Exception {
        final String name = "move and copy test" + (int) (Math.random() * 1000000);
        counter = new CountDownLatch(1);
        
        Futures.addCallback(Me.getRootFolderAsync().get().getChildFoldersAsync(), new FutureCallback<IFolders>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(IFolders result) {
                try {
                    FolderAsyncTestCase.this.folder = result.newFolder();
                    FolderAsyncTestCase.this.folder.setDisplayName(name);
                    FolderAsyncTestCase.this.folder = FolderAsyncTestCase.this.folder.moveAsync(Me.getDraftsAsync().get().getId()).get();
                    final CountDownLatch cdl = new CountDownLatch(1);
                    Futures.addCallback(FolderAsyncTestCase.this.folder.copyAsync(Me.getRootFolder().getId()), new FutureCallback<IFolder>() {
                        @Override
                        public void onFailure(Throwable t) {
                            reportError(t);
                            cdl.countDown();
                        }
                        
                        @Override
                        public void onSuccess(IFolder copied) {
                          try {
                              Me.getFolders().delete(FolderAsyncTestCase.this.folder.getId());
                              if (copied != null) {
                                  Me.getFolders().delete(copied.getId());
                              }
                                  
                              Me.flush();
                          } catch (Throwable t) {
                              reportError(t);
                          }
                          
                          cdl.countDown();
                        }
                    });
                    cdl.await();
                } catch (Throwable t) {
                    reportError(t);
                }
                counter.countDown();
            }
        });
        counter.await();
    }
    
    private void updateAndCheck() throws Exception {
        final String newName = "new name";
        folder.setDisplayName(newName);
        Me.flushAsync().get();
        assertEquals(newName, folder.getDisplayName());
        // ensure that changes were pushed to endpoint
        folder = Me.getFolders().getAsync(folder.getId()).get();
        assertEquals(newName, folder.getDisplayName());
    }
    
    private void readAndCheck() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getFolders().getAsync(folder.getId()), new FutureCallback<IFolder>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IFolder result) {
                try {
                    folder = result;
                    Class<?> cls = folder.getClass();
                    Class<?>[] emptyParametersArray = new Class<?>[0];
                    for (ODataProperty property : sourceFolder.getProperties()) {
                        try {
                            Method getter = cls.getMethod("get" + property.getName(), emptyParametersArray);
                            assertEquals(property.getPrimitiveValue().toValue(), getter.invoke(folder));
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
        prepareFolder();
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
                    assertTrue(StringUtils.isNotEmpty(folder.getId()));
                } catch (Throwable t) {
                    reportError(t);
                }
                
                cdl.countDown();
            }
        });
        cdl.await();
    }
    
    private void prepareFolder() {
        sourceFolder = getEntityFromResource("testFolder.json");
        folder = Me.getRootFolder().getChildFolders().newFolder();
        folder.setDisplayName(sourceFolder.getProperty("DisplayName").getValue().toString());
    }
    
    private void removeFolder() throws Exception {
        Me.getFolders().delete(folder.getId());
        Me.flushAsync().get();
    }
}
