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
package com.example.office.ui.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.office.Constants.UI;
import com.example.office.OfficeApplication;
import com.example.office.R;
import com.example.office.adapters.EventAdapter;
import com.example.office.logger.Logger;
import com.example.office.mail.data.Event;
import com.example.office.ui.calendar.EventActivity;
import com.example.office.utils.NetworkState;
import com.example.office.utils.NetworkUtils;
import com.microsoft.exchange.services.odata.model.IEvents;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.msopentech.odatajclient.engine.client.ODataClientFactory;
import com.msopentech.odatajclient.proxy.api.AsyncCall;

/**
 * Contains events.
 */
public class CalendarFragment extends ItemsFragment<IEvent, EventAdapter> {

    @Override
    protected int getListItemLayoutId() {
        return R.layout.event_list_item;
    }

    @Override
    protected UI.Screen getScreen() {
        return UI.Screen.CALENDAR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        try {
            final Activity activity = getActivity();
            final ListView listView = (ListView) rootView.findViewById(getListViewId());
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        IEvent event = getListAdapterInstance().getItem(position);

                        Intent intent = new Intent(OfficeApplication.getContext(), EventActivity.class);
                        intent.putExtra(activity.getString(R.string.intent_event_key), new Event(event));
                        activity.startActivity(intent);

                    } catch (Exception e) {
                        Logger.logApplicationException(e, getClass().getSimpleName() + ".listView.onItemClick(): Error.");
                    }
                }
            });
            registerForContextMenu(listView);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateView(): Error.");
        }

        return rootView;
    }

    @Override
    protected List<IEvent> getListData() {
        // We do not cache events in local persistence.
        return null;
    }

    @Override
    public EventAdapter getListAdapterInstance(List<IEvent> data) {
        try {
            if (mAdapter == null) {
                mAdapter = new EventAdapter(getActivity(), getListItemLayoutId(), data != null ? data : getListData());
            }
            return mAdapter;
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".getListAdapter(): Error.");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initList() {
        try {
            // Should have checked for persisted data but we don't do caching for Events.

            NetworkState nState = NetworkUtils.getNetworkState(getActivity());
            if (nState.getWifiConnectedState() || nState.getDataState() == NetworkUtils.NETWORK_UTILS_CONNECTION_STATE_CONNECTED) {
                showWorkInProgress(true, true);

                // TODO: wrap this implementation
                final Future<ArrayList<IEvent>> contacts = new AsyncCall<ArrayList<IEvent>>(ODataClientFactory.getV4().getConfiguration()) {
                    @Override
                    public ArrayList<IEvent> call() {
                        IEvents events = Me.getEvents();
                        // if this is not first call, Me.getEvents() returned CACHED copy of events and this copy will be
                        // passed to ArrayList constructor so we need to update them here
                        events.fetch();
                        return new ArrayList<IEvent>(events);
                    }
                };

                new AsyncTask<Future<ArrayList<IEvent>>, Void, Void>() {
                    @Override
                    protected Void doInBackground(final Future<ArrayList<IEvent>>... params) {
                        try {
                            final ArrayList<IEvent> result = contacts.get(12000, TimeUnit.SECONDS);
                            if (result != null) {
                                onDone(result);
                            } else {
                                onError(new Exception("Error while processing Events request"));
                            }
                        } catch (final Exception e) {
                            onError(e);
                        } finally {
                            isInitializing = false;
                        }

                        return null;
                    }
                }.execute(contacts);
            } else {
                Toast.makeText(getActivity(), R.string.data_connection_no_data_connection, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + "initList(): Error.");
        }
    }

    /**
     * Invoked when Events retrieving operation has been succeeded.
     *
     * @param result Result of operation.
     */
    public void onDone(final ArrayList<IEvent> items) {
        // You would add caching new items here.

        // Update UI
        updateList(items);
    }

}
