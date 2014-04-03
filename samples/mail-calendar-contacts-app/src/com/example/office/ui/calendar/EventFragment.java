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
package com.example.office.ui.calendar;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.example.office.R;
import com.example.office.data.Event;
import com.example.office.logger.Logger;
import com.example.office.ui.fragments.AuthFragment;
import com.example.office.utils.ImagePicker;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.Attendee;
import com.microsoft.exchange.services.odata.model.types.BodyType;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.microsoft.exchange.services.odata.model.types.IFileAttachment;
import com.msopentech.org.apache.commons.codec.CharEncoding;

/**
 * Event details fragment.
 */
public class EventFragment extends AuthFragment {

    private ImagePicker mImagePicker;

    /**
     * Currently displayed event
     */
    private Event event;

    protected LayoutInflater mInflater;

    protected int getFragmentLayoutId() {
        return R.layout.event_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = inflater.inflate(getFragmentLayoutId(), container, false);

        try {
            Activity activity = getActivity();
            activity.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

            Intent intent = getActivity().getIntent();
            event = (Event) intent.getExtras().get(getActivity().getString(R.string.intent_event_key));
            displayEvent(rootView);
            getActivity().setProgressBarIndeterminateVisibility(false);

            // Code below will be invoked when we receive system intent holding the path to shared image and this image is transformed into bytes[].
            mImagePicker = new ImagePicker(getActivity(), getActivity().getString(R.string.intent_event_key)) {
                @Override
                public void processImage(final byte[] imageBytes, final String fileName, final Object intentArg) {
                    try {
                        String itemId = "";
                        if(intentArg instanceof Event) {
                            itemId = ((Event) intentArg).getId();
                        }
                        
                        if (!TextUtils.isEmpty(itemId)) {
                            // Getting event by id
                            Futures.addCallback(Me.getEvents().getAsync(itemId), new FutureCallback<IEvent>() {
                                @Override
                                public void onFailure(Throwable t) {
                                    if (!onError(t)) {
                                        mImagePicker.showStatusToast(Status.UPLOAD_FAILED);
                                    }
                                }
                                
                                @Override
                                public void onSuccess(IEvent event) {
                                    try {
                                        IFileAttachment attachment = event.getAttachments().newFileAttachment();
                                        attachment.setContentBytes(imageBytes).setName(fileName);
                                        // Propagating changes to server
                                        Me.flush();
    
                                        mImagePicker.showStatusToast(Status.UPLOAD_SUCCESS);
                                    } catch (Exception e) {
                                        onFailure(e);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (!onError(e)) {
                            mImagePicker.showStatusToast(Status.UPLOAD_FAILED);
                        }
                    }
                }
            };
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateView(): Error.");
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.event_options, menu);

        // This will be called as soon as user presses the attach menu item to attach image to current event
        menu.findItem(R.id.action_attach).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_attach:
                        mImagePicker.showAttachImageDialog();
                        return true;

                    default:
                        return false;
                }
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

   /**
     * Fills fragment content with events
     *
     * @param root Root view for current fragment
     */
    private void displayEvent(View root) {
        try {
            //Populate fragment fields with event properties
            TextView subjectView = (TextView) root.findViewById(R.id.event_fragment_subject);
            subjectView.setText(event.getSubject());

            // Resolving Attendees
            TextView attendeesView = (TextView) root.findViewById(R.id.event_fragment_participants);
            StringBuilder attendeesStr = new StringBuilder(getActivity().getString(R.string.event_attendees));
            Collection<Attendee> attendees = event.getAttendees();
            if (attendees != null && !attendees.isEmpty()) {
                for (Attendee attendee : attendees) {
                    if(!TextUtils.isEmpty(attendee.getName())) {
                        attendeesStr.append(attendee.getName()).append(getActivity().getString(R.string.event_addressee_delimiter));
                    }
                }
            }
            attendeesView.setText(attendeesStr.toString());

            // Resolving Start and end of the event
            TextView dateStartView = (TextView) root.findViewById(R.id.event_fragment_date_start);
            TextView dateEndView = (TextView) root.findViewById(R.id.event_fragment_date_end);
            Date start = event.getStart();
            Date end = event.getEnd();
            final String pattern = "yyyy-MM-dd HH:mm";
            final SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
            if(start != null && end != null) {
                dateStartView.setText(String.format(getActivity().getString(R.string.event_date_start), formatter.format(start)));
                dateEndView.setText(String.format(getActivity().getString(R.string.event_date_end), formatter.format(end)));
            }

            // resolving location
            TextView locationView = (TextView) root.findViewById(R.id.event_fragment_location);
            String location = "Location: ";
            if (event.getLocation() == null || TextUtils.isEmpty(event.getLocation().getDisplayName())) {
                location += "unknown";
            } else {
                location += event.getLocation().getDisplayName();
            }
            locationView.setText(location);

            //Resolving event message
            WebView webview = (WebView) root.findViewById(R.id.event_fragment_content);
            if (event.getBody().getContentType() == BodyType.HTML) {
                webview.loadData(event.getBody().getContent(), getActivity().getString(R.string.mime_type_text_html), CharEncoding.UTF_8);
            } else {
                webview.loadData(event.getBody().getContent(), getActivity().getString(R.string.mime_type_text_plain), CharEncoding.UTF_8);
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".displayEvent(): Error.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(!mImagePicker.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Event getEvent() {
        return event;
    }

}
