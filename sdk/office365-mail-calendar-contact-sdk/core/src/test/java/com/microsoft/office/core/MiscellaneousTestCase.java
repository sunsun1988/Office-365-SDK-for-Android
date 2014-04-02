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

import org.junit.Test;

import com.microsoft.exchange.services.odata.model.IMessages;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.Messages;
import com.microsoft.exchange.services.odata.model.types.IMessage;

public class MiscellaneousTestCase extends AbstractTest {

    @Test
    public void fetchTest() {
        IMessages messages = Me.getDrafts().getMessages();
        // actual server request for messages will be executed in this line by calling size; response will be cached
        int size = messages.size();
        
        IMessage message = Messages.newMessage();
        message.setSubject("fetch test");
        // flush() updates server side, not the client side
        Me.flush();
            
        // verify that local cache has no changes after flush (size will return old value)
        try {
            assertEquals(size, messages.size());   
            messages.fetch();
            assertEquals(size + 1, messages.size());
        } finally {
            Me.getMessages().delete(message.getId());
            Me.flush();
        }
    }
}
