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
import com.microsoft.exchange.services.odata.model.IAttachments;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.Messages;
import com.microsoft.exchange.services.odata.model.types.IAttachment;
import com.microsoft.exchange.services.odata.model.types.IFileAttachment;
import com.microsoft.exchange.services.odata.model.types.IItem;
import com.microsoft.exchange.services.odata.model.types.IMessage;

public class AttachmentsAsyncTestCase extends AbstractAsyncTest {
    
    private static IMessage message;

    private final String attachmentName = "test attachment";

    private static IItem itemAttachment;

    private static final String itemAttachmentSubject = "item attachment test";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        message = Messages.newMessage();
        message.setSubject("Attachments test");
        Me.flush();

        itemAttachment = Messages.newMessage();
        itemAttachment.setSubject(itemAttachmentSubject);
        Me.flush();
    }
    
    public void testCreateFileAttachment() {
        final IFileAttachment attachment = createFileAttachment();
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            public void onSuccess(Void res) {
                try {
                    checkCreated(attachment);
                    removeAttachment(attachment);
                } catch (Throwable e) {
                    reportError(e);
                }
                counter.countDown();
            }
            
            public void onFailure(Throwable err) {
                reportError(err);
                counter.countDown();
            }
        });
        try {
            if (!counter.await(60000, TimeUnit.MILLISECONDS)) {
                fail("testCreateFileAttachment() timed out");
            }
        } catch (InterruptedException e) {
            fail("testCreateFileAttachment() has been interrupted");
        }
        
        try {
            removeMessages();
        } catch (Throwable t) {
            reportError(t);
        }
    }
    
    public void testReadFileAttachment() {
        counter = new CountDownLatch(1);
        final IFileAttachment attachment = createFileAttachment();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            public void onFailure(Throwable err) {
                reportError(err);
                counter.countDown();
            }
            public void onSuccess(Void arg0) {
                try {
                    checkFileAttachmentRead(attachment);
                    removeAttachment(attachment);
                } catch (Exception e) {
                    reportError(e);
                }
                counter.countDown();
            }
        });
        try {
            if (!counter.await(60000, TimeUnit.MILLISECONDS)) {
                fail("testReadFileAttachment() timed out");
            }
        } catch (InterruptedException e) {
            fail("testReadFileAttachment() has been interrupted");
        }
        
        try {
            removeMessages();
        } catch (Throwable t) {
            reportError(t);
        }
    }
    
    public void testDeleteFileAttachment() {
        counter = new CountDownLatch(1);
        final IFileAttachment attachment = createFileAttachment();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            public void onFailure(Throwable err) {
                reportError(err);   
                counter.countDown();
            }
            
            public void onSuccess(Void arg0) {
                try {
                    removeAttachment(attachment);
                    checkDeleted(attachment);
                } catch (Exception e) {
                    reportError(e);
                }
                
                counter.countDown();
            }
        });

        try {
            if (!counter.await(60000, TimeUnit.MILLISECONDS)) {
                fail("testDeleteFileAttachment() timed out");
            }
        } catch (InterruptedException e) {
            fail("testDeleteFileAttachment() has been interrupted");
        }
        
        try {
            removeMessages();
        } catch (Throwable t) {
            reportError(t);
        }
    }
    
    private void checkDeleted(final IAttachment attachment) throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(message.getAttachmentsAsync(), new FutureCallback<IAttachments>() {
            public void onFailure(Throwable err) {
                reportError(err);
                cdl.countDown();
            }
            
            public void onSuccess(IAttachments attachments) {
                Futures.addCallback(attachments.getAsync(attachment.getId()), new FutureCallback<IAttachment>() {
                    public void onFailure(Throwable err) {
                        reportError(err);
                        cdl.countDown();
                    }
                    
                    public void onSuccess(IAttachment a) {
                        try {
                            assertNull(a);
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
    
    private void checkFileAttachmentRead(IFileAttachment attachment) throws Exception {
        byte[] content = getImage("images/mslogo.png");

        assertArrayEquals(content, attachment.getContentBytes());
        assertEquals(attachmentName, attachment.getName());
        assertArrayEquals(content, message.getAttachments().get(attachment.getId(), IFileAttachment.class).getContentBytes());
        assertEquals(attachmentName, message.getAttachments().get(attachment.getId()).getName());
    }
    
    private void removeAttachment(final IAttachment attachment) throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(message.getAttachmentsAsync(), new FutureCallback<IAttachments>() {
            public void onSuccess(IAttachments attachments) {
                try {
                    attachments.delete(attachment.getId());
                    Me.flush();
                } catch (Throwable t) {
                    reportError(t);
                }
                cdl.countDown();
            }; 
            public void onFailure(Throwable err) {
                reportError(err);
                cdl.countDown();
            }
        });  
        cdl.await();
    }
    
    private void removeMessages() {
        Me.getMessages().delete(message.getId());
        Me.getMessages().delete(itemAttachment.getId());
        Me.flush();
    }
    
    private void checkCreated(IAttachment attachment) {
        assertNotNull(attachment.getId());
    }
    
    private IFileAttachment createFileAttachment() {
        IFileAttachment attachment = message.getAttachments().newFileAttachment();
        attachment.setContentBytes(getImage("images/mslogo.png"));
        attachment.setName(attachmentName);
        return attachment;
    }
    
    private byte[] getImage(String path) {
        return getImageInByteFromResource(path);
    }
}
