/**
 * Copyright � Microsoft Open Technologies, Inc.
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
package com.example.office.adapters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.office.R;
import com.microsoft.exchange.services.odata.model.types.IEvent;

/**
 * Adapter for displaying MailItem in ListView
 */
public class EventAdapter extends SearchableAdapter<IEvent> {

    /**
     * Default constructor.
     *
     * @param context Application context.
     * @param resource List item resource id.
     * @param data Data to populate.
     */
    public EventAdapter(Context context, int resource, List<IEvent> data) {
        super(context, resource, data);
    }

    @Override
    protected boolean isMatch(IEvent item, CharSequence constraint) {
        if (item != null && !TextUtils.isEmpty(constraint)) {
            List<String> list = new ArrayList<String>();

            list.add(item.getBodyPreview());

            Date start = new Date(item.getStart().getTimestamp().getTime());
            Date end = new Date(item.getEnd().getTimestamp().getTime());
            list.add(DateFormatUtils.ISO_TIME_FORMAT.format(start));
            list.add(DateFormatUtils.ISO_TIME_FORMAT.format(end));

            for (String value : list) {
                if (!TextUtils.isEmpty(value) && value.contains(constraint)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Constructs and returns View for filling ListView
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            EventHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(mItemResource, null);

                holder = new EventHolder();
                holder.date = (TextView) convertView.findViewById(R.id.event_timeframe);
                holder.subject = (TextView) convertView.findViewById(R.id.event_subject);
                holder.hasAttachments = (ImageView) convertView.findViewById(R.id.event_attachment_icon);

                convertView.setTag(holder);
            } else {
                holder = (EventHolder) convertView.getTag();
            }

            IEvent item = getItem(position);
            if (item != null) {
                Date start = new Date(item.getStart().getTimestamp().getTime());
                Date end = new Date(item.getEnd().getTimestamp().getTime());
//                String timeframe = String.format("%1$s - %2$s", DateFormatUtils.ISO_TIME_FORMAT.format(start), DateFormatUtils.ISO_TIME_FORMAT.format(end));
//                setViewText(holder.date, timeframe);

                String subject = item.getSubject() == null ? "" : item.getSubject();
                setViewText(holder.subject, subject);

                holder.hasAttachments.setVisibility(item.getHasAttachments() ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {}
        return convertView;
    }

    /**
     * Represents an inner structure of single ListView item
     */
    private class EventHolder {
        TextView date;
        TextView subject;
        ImageView hasAttachments;
    }

}