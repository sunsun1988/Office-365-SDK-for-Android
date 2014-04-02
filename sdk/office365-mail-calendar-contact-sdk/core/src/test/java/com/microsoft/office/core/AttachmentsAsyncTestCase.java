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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
    
    @BeforeClass
    public static void prepareMessage() {
        message = Messages.newMessage();
        message.setSubject("Attachments test");
        Me.flush();
    }
    
    @BeforeClass
    public static void prepareAttachedMessage() {
        itemAttachment = Messages.newMessage();
        itemAttachment.setSubject(itemAttachmentSubject);
        Me.flush();
    }

    @AfterClass
    public static void removeMessage() throws Exception {
        Me.getMessages().delete(message.getId());
        Me.flush();
    }

    @AfterClass
    public static void removeAttachedMessage() throws Exception {
        Me.getMessages().delete(itemAttachment.getId());
        Me.flush();
    }
    
    @Test(timeout = 60000)
    public void createFileAttachmentTest() throws Exception {
        counter = new CountDownLatch(1);
        final IFileAttachment attachment = createFileAttachment();

        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void res) {
                checkCreated(attachment);
                try {
                    removeAttachment(attachment);
                } catch (Exception e) {
                    reportError(e);
                }
                counter.countDown();
            }
            
            @Override
            public void onFailure(Throwable err) {
                reportError(err);
            }
        });
        counter.await();
    }
    
    @Test(timeout = 60000)
    public void readFileAttachmentTest() throws Exception {
        counter = new CountDownLatch(1);
        final IFileAttachment attachment = createFileAttachment();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable err) {
                reportError(err);
                counter.countDown();
            }
            @Override
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

        counter.await();
    }
    
    @Test(timeout = 60000)
    public void deleteFileAttachmentTest() throws Exception {
        counter = new CountDownLatch(1);
        final IFileAttachment attachment = createFileAttachment();
        Futures.addCallback(Me.flushAsync(), new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable err) {
                reportError(err);   
                counter.countDown();
            }
            
            @Override
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

        counter.await();
    }
    
    private void checkDeleted(final IAttachment attachment) throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(message.getAttachmentsAsync(), new FutureCallback<IAttachments>() {
            @Override
            public void onFailure(Throwable err) {
                reportError(err);
                cdl.countDown();
            }
            
            @Override
            public void onSuccess(IAttachments attachments) {
                Futures.addCallback(attachments.getAsync(attachment.getId()), new FutureCallback<IAttachment>() {
                    @Override
                    public void onFailure(Throwable err) {
                        reportError(err);
                        cdl.countDown();
                    }
                    
                    @Override
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
        byte[] content = getImage("/images/mslogo.png");

        assertArrayEquals(content, attachment.getContentBytes());
        assertEquals(attachmentName, attachment.getName());
        assertArrayEquals(content, message.getAttachments().get(attachment.getId(), IFileAttachment.class).getContentBytes());
        assertEquals(attachmentName, message.getAttachments().get(attachment.getId()).getName());
    }
    
    private IFileAttachment createFileAttachment() throws Exception {
        IFileAttachment attachment = message.getAttachmentsAsync().get().newFileAttachment();
        attachment.setContentBytes(getImage("/images/mslogo.png"));
        attachment.setName(attachmentName);
        return attachment;
    }
    
    private byte[] getImage(String path) {
        byte[] content = null;
        try {
            content = IOUtils.toByteArray(getClass().getResourceAsStream(path));
        } catch (IOException e) {
            fail("Unable to read attachment file");
        }
        return content;
    }
    
    private void checkCreated(IAttachment attachment) {
        assertNotNull(attachment.getId());
    }
    
    private void removeAttachment(final IAttachment attachment) throws Exception {
        final CountDownLatch cdl = new CountDownLatch(1);
        Futures.addCallback(message.getAttachmentsAsync(), new FutureCallback<IAttachments>() {
            public void onSuccess(IAttachments attachments) {
                attachments.delete(attachment.getId());
                Me.flush();
                cdl.countDown();
            }; 
            @Override
            public void onFailure(Throwable err) {
                reportError(err);
                cdl.countDown();
            }
        });  
        cdl.await();
    }
}
