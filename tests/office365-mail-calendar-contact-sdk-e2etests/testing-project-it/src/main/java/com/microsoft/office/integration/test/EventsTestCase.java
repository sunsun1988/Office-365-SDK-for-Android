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

import java.lang.reflect.Method;
import java.util.Iterator;

//import android.text.TextUtils;
import org.apache.commons.lang.StringUtils;
import com.microsoft.exchange.services.odata.model.Events;
import com.microsoft.exchange.services.odata.model.ICalendars;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.ICalendar;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataProperty;
import com.msopentech.odatajclient.engine.data.ODataTimestamp;
import com.msopentech.odatajclient.engine.data.metadata.edm.EdmSimpleType;

public class EventsTestCase extends AbstractTest {

    private static ICalendar calendar;

    private ODataEntity sourceEvent;

    private IEvent event;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ICalendars cals = Me.getCalendars();
        // an empty iterator will be returned for any entity set unless you call fetch()
        cals.fetch();
        Iterator<ICalendar> iterator = cals.iterator();
        if (iterator.hasNext()) {
            calendar = iterator.next();
        } else {
            fail("No calendar found");
        }
    }

    public void testCreate() {
        createAndCheck();
        removeEvent();
    }

    public void testRead() {
        prepareEvent();
        Me.flush();
        readAndCheck();
        removeEvent();
    }

    public void testUpdate() {
        prepareEvent();
        Me.flush();
        updateAndCheck();
        removeEvent();
    }

    public void testDelete() {
        prepareEvent();
        Me.flush();
        deleteAndCheck();
    }

    public void testEventsCRUD() {
        try {
            createAndCheck();
            readAndCheck();
            updateAndCheck();
            deleteAndCheck();
        } catch (AssertionError e) {
            removeEvent();
            throw e;
        }
    }

    public void testDateTime() {
        prepareEvent();
        final ODataTimestamp start = ODataTimestamp.parse(EdmSimpleType.DateTimeOffset, "2015-01-01T00:00:00.000000+00:00"),
                             end   = ODataTimestamp.parse(EdmSimpleType.DateTimeOffset, "2016-01-01T00:00:00Z");
        event.setStart(start);
        event.setEnd(end);
        Me.flush();
        assertEquals(start, event.getStart());
        assertEquals(end, event.getEnd());
        removeEvent();
    }

    private void deleteAndCheck() {
        removeEvent();
        assertNull(Me.getEvents().get(event.getId()));
    }

    private void updateAndCheck() {
        final String newSubject = "new subject";
        event.setSubject(newSubject);
        Me.flush();
        assertEquals(newSubject, event.getSubject());
        // reread an event to make sure changes were sent to server
        event = Me.getEvents().get(event.getId());
        assertEquals(newSubject, event.getSubject());
    }

    private void readAndCheck() {
        event = Me.getEvents().get(event.getId());
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
    }

    private void createAndCheck() {
        prepareEvent();
        Me.flush();
        assertTrue(StringUtils.isNotEmpty(event.getId()));
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
