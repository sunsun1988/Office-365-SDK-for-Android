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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.DefaultFolder;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.Messages;
import com.microsoft.exchange.services.odata.model.types.BodyType;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.microsoft.exchange.services.odata.model.types.IMessage;
import com.microsoft.exchange.services.odata.model.types.IMessageCollection;
import com.microsoft.exchange.services.odata.model.types.Importance;
import com.microsoft.exchange.services.odata.model.types.ItemBody;
import com.microsoft.exchange.services.odata.model.types.Recipient;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataProperty;

public class MessagesAsyncTestCase extends AbstractAsyncTest {

    private IMessage message;

    private ODataEntity sourceMessage;
    
    @Test(timeout = 60000)
    public void createTest() throws Exception {
        try {
            createAndCheck();
        } finally {
            // clean-up
            removeMessage();
        }
    }
    
    @Test(timeout = 60000)
    public void readTest() throws Exception {
        // create message first
        prepareMessage();
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
                    try {
                        readAndCheck();
                    } finally {
                        // clean-up
                        removeMessage();
                    }
                } catch (Exception e) {
                    reportError(e);
                }
                
                counter.countDown();
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void updateTest() throws Exception {
        // create message first
        counter = new CountDownLatch(1);
        prepareMessage();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                try {
                    try {
                        updateAndCheck();
                    } finally {
                        // clean up
                        removeMessage();
                    }
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
        // create message first
        prepareMessage();
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
                    // then remove
                    deleteAndCheck();
                } catch (Throwable t) {
                    reportError(t);
                }
                counter.countDown();
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void createInDefaultFolderTest() throws Exception {
        try {
            message = Messages.newMessage();
            sourceMessage = getEntityFromResource("simpleMessage.json");
            String subject = sourceMessage.getProperty("Subject").getPrimitiveValue().toString();
            message.setSubject(subject);
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
                        assertTrue(StringUtils.isNotEmpty(MessagesAsyncTestCase.this.message.getId()));
                        final CountDownLatch cdl = new CountDownLatch(1);
                        Futures.addCallback(Me.getDraftsAsync(), new FutureCallback<IFolder>() {
                            @Override
                            public void onFailure(Throwable t) {
                                reportError(t);
                                cdl.countDown();
                            }
                            
                            public void onSuccess(IFolder result) {
                                assertEquals(MessagesAsyncTestCase.this.message.getParentFolderId(), result.getId());
                                cdl.countDown();
                            };
                        });
                        
                        cdl.await();
                    } catch (Throwable t) {
                        reportError(t);
                    }
                    counter.countDown();
                }
            });
            
            counter.await();
        } finally {
            removeMessage();
        }
    }
    
    @Test(timeout = 60000)
    public void enumsTest() throws Exception {
        prepareMessage();
        final ItemBody body = new ItemBody();
        body.setContent("<!DOCTYPE html><html><body><h1>test</h1></body></html>");
        body.setContentType(BodyType.HTML);
        message.setBody(body);
        assertEquals(message.getBody().getContentType(), body.getContentType());
        message.setImportance(Importance.Low);
        assertEquals(message.getImportance(), Importance.Low);
        counter = new CountDownLatch(1);
        try {
            Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable t) {
                    reportError(t);
                    counter.countDown();
                }
                
                @Override
                public void onSuccess(Void result) {
                    try {
                        MessagesAsyncTestCase.this.message = Me.getMessages().getAsync(MessagesAsyncTestCase.this.message.getId()).get();
                        assertEquals(MessagesAsyncTestCase.this.message.getBody().getContentType(), body.getContentType());
                        assertEquals(MessagesAsyncTestCase.this.message.getImportance(), Importance.Low);
                    } catch (Throwable t) {
                        reportError(t);
                    }
                    
                    counter.countDown();
                }
            });
            
            counter.await();
            
        } finally {
            removeMessage();
        }
    }
    
    @Test(timeout = 60000)
    public void messageCRUDTest() throws Exception {
        try {
            // CREATE
            createAndCheck();

            // READ
            readAndCheck();

            // UPDATE
            updateAndCheck();

            // DELETE
            deleteAndCheck();
        } catch (Exception e) {
            removeMessage();
        }
    }
    
    @Test(timeout = 60000)
    public void replyTest() throws Exception {
        // first send message to self
        final String subject = "reply test" + (int) (Math.random() * 1000000);
        message = (IMessage) Messages.newMessage()
                .setToRecipients(new ArrayList<Recipient>() {{ add(new Recipient().setAddress(username)); }})
                .setSubject(subject);
        
        counter = new CountDownLatch(1);
        Futures.addCallback(message.sendAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                try {
                    // find message in inbox after a little delay, otherwise service sometimes lags with message processing
                    IMessage inboxMessage = null;
                    for (int i = 0; i < 20; ++i) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}

                        if (Me.getInboxAsync().get().getMessagesAsync().get()
                                .createQuery().setFilter("Subject eq '" + subject + "'").getResult().size() > 0) {
                            inboxMessage = Me.getInbox().getMessages().createQuery().setFilter("Subject eq '" + subject + "'").getSingleResult();
                            break;
                        }
                    }

                    if (inboxMessage == null) {
                        fail("message did not send");
                    }
                    
                    final String reply = "reply on test message";
                    final CountDownLatch cdl = new CountDownLatch(1); 
                    Futures.addCallback(inboxMessage.replyAsync(reply), new FutureCallback<Void>() {
                        @Override
                        public void onFailure(Throwable t) {
                            reportError(t);
                            cdl.countDown();
                        }
                        
                        @Override
                        public void onSuccess(Void result) {
                            try {
                                // find reply after a little delay
                                IMessageCollection replies = null;
                                for (int i = 0; i < 20; ++i) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {}
    
                                    try {
                                        if (Me.getInbox().getMessages().createQuery().
                                                setFilter("Subject eq 'RE: " + subject + "'").getResult().size() > 0) {
                                            replies = Me.getInboxAsync().get().getMessagesAsync().get()
                                                    .createQuery().setFilter("Subject eq 'RE: " + subject + "'").getResult();
                                            break;
                                        }
                                    } catch (Throwable t) { 
                                        reportError(t);
                                    }
                                }
    
                                if (replies == null) {
                                    fail("reply did not send");
                                }
                                assertEquals(1, replies.size());
                            } catch (Throwable t) {
                                reportError(t);
                            } finally {
                                cdl.countDown();
                            }
                        }
                    });
                    cdl.await();
                } catch (Throwable e) {
                    reportError(e);
                } finally {
                    Iterator<IMessage> messages = Me.getSentItems().getMessages().createQuery()
                            .setFilter("contains(Subject, '" + subject + "')").getResult().iterator();

                    while (messages.hasNext()) {
                        Me.getMessages().delete(messages.next().getId());
                    }

                    messages = Me.getInbox().getMessages().createQuery()
                            .setFilter("contains(Subject, '" + subject + "')").getResult().iterator();
                    while (messages.hasNext()) {
                        Me.getMessages().delete(messages.next().getId());
                    }

                    Me.flush();
                    
                    counter.countDown();
                }
            }
        });
        
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void moveAndCopyTest() throws Exception {
        final String subject = "move and copy test" + (int) (Math.random() * 1000000);
        message = (IMessage) Messages.newMessage(DefaultFolder.ROOT).setSubject(subject);
        counter = new CountDownLatch(1);
        
        Futures.addCallback(message.moveAsync(Me.getDrafts().getId()), new FutureCallback<IMessage>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(IMessage result) {
                try {
                    MessagesAsyncTestCase.this.message = result;
                    final CountDownLatch cdl = new CountDownLatch(1);
                    Futures.addCallback(MessagesAsyncTestCase.this.message.copyAsync(Me.getRootFolderAsync().get().getId()), new FutureCallback<IMessage>() {
                        @Override
                        public void onFailure(Throwable t) {
                            reportError(t);
                            cdl.countDown();
                        }
                        
                        @Override
                        public void onSuccess(IMessage copied) {
                            if (copied != null) {
                                Me.getMessages().delete(copied.getId());
                            }
                            cdl.countDown();
                        }
                    });
                    cdl.await();
                } catch (Exception e) { 
                    reportError(e);
                } finally {
                    counter.countDown();
                }
            }
        });
        
        counter.await();
        Me.getMessages().delete(message.getId());
        Me.flush();
    }
    
    private void deleteAndCheck() throws Exception {
        removeMessage();
        assertNull(Me.getMessages().get(message.getId()));
    }
    
    private void updateAndCheck() throws Exception {
        final String content = "updated body text";
        ItemBody newBody = new ItemBody();
        newBody.setContent(content);
        newBody.setContentType(BodyType.Text);

        message.setBody(newBody);
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(Void result) {
                assertEquals(BodyType.Text, MessagesAsyncTestCase.this.message.getBody().getContentType());
                assertEquals(content, MessagesAsyncTestCase.this.message.getBody().getContent());
                // ensure that changes were pushed to endpoint
                Futures.addCallback(Me.getMessages().getAsync(MessagesAsyncTestCase.this.message.getId()), new FutureCallback<IMessage>() {
                    @Override
                    public void onFailure(Throwable t) {
                        reportError(t);
                        cdl.countDown();
                    }
                    
                    @Override
                    public void onSuccess(IMessage result) {
                        try {
                            MessagesAsyncTestCase.this.message = result;
                            assertEquals(BodyType.Text, MessagesAsyncTestCase.this.message.getBody().getContentType());
                            assertEquals(content, MessagesAsyncTestCase.this.message.getBody().getContent());
                        } catch (Throwable t) {
                            reportError(t);
                        }
                        
                        cdl.countDown();
                    }
                });
            }
        });

        cdl.await();
    }
    
    private void readAndCheck() throws Exception {
        // reread a message
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(Me.getMessages().getAsync(message.getId()), new FutureCallback<IMessage>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IMessage result) {
                try {
                    MessagesAsyncTestCase.this.message = result;
                    Class<?> cls = MessagesAsyncTestCase.this.message.getClass();
                    Class<?>[] emptyParametersArray = new Class<?>[0];
                    for (ODataProperty property : sourceMessage.getProperties()) {
                        try {
                            Method getter = cls.getMethod("get" + property.getName(), emptyParametersArray);
                            assertEquals(property.getPrimitiveValue().toValue(), getter.invoke(MessagesAsyncTestCase.this.message));
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
        final CountDownLatch cdl = new CountDownLatch(1);
        prepareMessage();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                cdl.countDown();
            }
            @Override
            public void onSuccess(Void result) {
                try {
                    assertTrue(StringUtils.isNotEmpty(MessagesAsyncTestCase.this.message.getId()));
                } catch (Throwable t) {
                    reportError(t);
                }
                cdl.countDown();
            }
        });
        cdl.await();
    }
    
    private void prepareMessage() {
        sourceMessage = getEntityFromResource("simpleMessage.json");
        message = Messages.newMessage(DefaultFolder.DRAFTS);
        String subject = sourceMessage.getProperty("Subject").getPrimitiveValue().toString();
        message.setSubject(subject);
    }
    
    private void removeMessage() throws Exception {
        Me.getMessages().delete(message.getId());
        Me.flush();
    }
}
