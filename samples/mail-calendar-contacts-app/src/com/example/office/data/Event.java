package com.example.office.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import com.microsoft.exchange.services.odata.model.types.Attendee;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.microsoft.exchange.services.odata.model.types.ItemBody;
import com.microsoft.exchange.services.odata.model.types.Location;

public class Event implements Serializable {

    private static final long serialVersionUID = -2697773519002404709L;

    private boolean mHasAttachments;
    private String mSubject;
    private Collection<Attendee> mAttendees;
    private Date mStart;
    private Date mEnd;
    private ItemBody mBody;
    private String mId;
    private Location mLocation;

    public Event(IEvent source) {
        mHasAttachments = source.getHasAttachments();
        mSubject = source.getSubject();
        mAttendees = source.getAttendees();
        mStart = new Date(source.getStart().getTimestamp().getTime());
        mEnd= new Date(source.getEnd().getTimestamp().getTime());
        mBody = source.getBody();
        mId = source.getId();
        mLocation = source.getLocation();
    }

    public boolean getHasAttachments() {
        return mHasAttachments;
    }
    
    public String getSubject() {
        return mSubject;
    }
    
    public Collection<Attendee> getAttendees() {
        return mAttendees;
    }
    
    public Date getStart() {
        return mStart;
    }
    
    public Date getEnd() {
        return mEnd;
    }
    
    public ItemBody getBody() {
        return mBody;
    }
    
    public String getId() {
        return mId;
    }
    
    public Location getLocation() {
        return mLocation;
    }
}
