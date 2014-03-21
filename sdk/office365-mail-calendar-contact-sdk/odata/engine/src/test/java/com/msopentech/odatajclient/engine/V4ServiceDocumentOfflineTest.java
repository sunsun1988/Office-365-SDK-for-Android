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
package com.msopentech.odatajclient.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.msopentech.odatajclient.engine.client.ODataClientFactory;
import com.msopentech.odatajclient.engine.data.impl.AbstractServiceDocument;
import com.msopentech.odatajclient.engine.format.ODataFormat;

public class V4ServiceDocumentOfflineTest extends AbstractTest {
    
    @Test
    public void serviceDocumentV4Test() {
        AbstractServiceDocument doc = ODataClientFactory.getV4().getReader().
                readServiceDocument(getClass().getResourceAsStream("v4/exchange-service-document.xml"), 
                        ODataFormat.XML);
        
        assertEquals("https://outlook.office365.com/EWS/OData/$metadata", doc.getMetadataContext());
        assertEquals(1, doc.getEntitySets().size());
        assertEquals(1, doc.getSingletons().size());
        assertEquals(0, doc.getFunctionImports().size());
        assertEquals(0, doc.getRelatedServiceDocuments().size());
        assertTrue(StringUtils.isEmpty(doc.getMetadataETag()));
    }
}
