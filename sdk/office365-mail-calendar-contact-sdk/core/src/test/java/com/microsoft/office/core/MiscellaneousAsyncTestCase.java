package com.microsoft.office.core;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.IMessages;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.Messages;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.microsoft.exchange.services.odata.model.types.IMessage;

public class MiscellaneousAsyncTestCase extends AbstractAsyncTest {

    @Test(timeout = 60000)
    public void fetchTest() throws Exception {
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.getDraftsAsync(),  new FutureCallback<IFolder>() {
            @Override
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            @Override
            public void onSuccess(IFolder drafts) {
                try {
                    final IMessages messages = drafts.getMessagesAsync().get();
                    // actual server request for messages will be executed in this line by calling size; response will be cached
                    final int size = messages.size();
                    
                    final IMessage message = Messages.newMessage();
                    message.setSubject("fetch test");
                    Me.flush();
                    
                    // verify that local cache has no changes after flush (size will return old value)
                    assertEquals(size, messages.size());   
                    
                    final CountDownLatch cdl = new CountDownLatch(1);
                    Futures.addCallback(messages.fetchAsync(), new FutureCallback<Void>() {
                        @Override
                        public void onFailure(Throwable t) {
                            reportError(t);
                            cdl.countDown();
                        }
                        
                        @Override
                        public void onSuccess(Void result) {
                            try {
                                assertEquals(size + 1, messages.size());
                                Me.getMessages().delete(message.getId());
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
}
