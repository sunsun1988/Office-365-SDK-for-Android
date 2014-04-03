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
package com.example.office.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.office.R;
import com.microsoft.exchange.services.odata.model.types.IContact;

public class ContactAdapter extends SearchableAdapter<IContact> {

    /**
     * Default constructor.
     *
     * @param context Application context.
     * @param resource List item resource id.
     * @param data Data to populate.
     */
    public ContactAdapter(Context context, int resource, List<IContact> data) {
        super(context, resource, data);
    }
    
    @Override
    protected boolean isMatch(IContact item, CharSequence constraint) {
        if (item != null && !TextUtils.isEmpty(constraint)) {
            List<String> list = new ArrayList<String>();
            
            list.add(item.getBodyPreview());
            list.add(item.getAssistantName());
            list.add(item.getBusinessPhone1());
            list.add(item.getBusinessPhone2());
            list.add(item.getCompanyName());
            list.add(item.getDisplayName());
            list.add(item.getEmailAddress1());
            list.add(item.getEmailAddress2());
            list.add(item.getEmailAddress3());
            list.add(item.getHomePhone1());
            list.add(item.getHomePhone2());
            list.add(item.getMobilePhone1());
            list.add(item.getNickName());
            
            for (String value: list) {
                if (!TextUtils.isEmpty(value) && value.contains(constraint)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            ContactHolder holder;
            
            if (convertView == null) {
                convertView = mInflater.inflate(mItemResource, null);
                
                holder = new ContactHolder();
                holder.displayName = (TextView) convertView.findViewById(R.id.contact_display_name);
                holder.subject = (TextView) convertView.findViewById(R.id.contact_subject);
                holder.hasAttachments = (ImageView) convertView.findViewById(R.id.contact_attachment_icon);
                
                convertView.setTag(holder);
            } else {
                holder = (ContactHolder) convertView.getTag();
            }
            
            IContact item = getItem(position);
            if (item != null) {
                setViewText(holder.displayName, item.getDisplayName());
                setViewText(holder.subject, item.getSubject());
                holder.hasAttachments.setVisibility(item.getHasAttachments() ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e){}
        return convertView;
    }
    
    private class ContactHolder {
        TextView displayName;
        TextView subject;
        ImageView hasAttachments;
    }
}
