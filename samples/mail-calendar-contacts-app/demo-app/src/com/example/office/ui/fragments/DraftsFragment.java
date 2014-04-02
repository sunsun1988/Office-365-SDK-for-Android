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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.office.Constants.UI;
import com.example.office.OfficeApplication;
import com.example.office.R;
import com.example.office.adapters.MailItemAdapter;
import com.example.office.data.MailConfig;
import com.example.office.data.MailItem;
import com.example.office.logger.Logger;
import com.example.office.storage.MailConfigPreferences;
import com.example.office.ui.mail.MailItemActivity;
import com.example.office.utils.NetworkState;
import com.example.office.utils.NetworkUtils;
import com.microsoft.exchange.services.odata.model.IMessages;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.microsoft.exchange.services.odata.model.types.IMessage;
import com.msopentech.odatajclient.engine.client.ODataClientFactory;
import com.msopentech.odatajclient.proxy.api.AsyncCall;

/**
 * 'Drafts' fragment containing logic related to managing drafts emails.
 */
public class DraftsFragment extends ItemsFragment<MailItem, MailItemAdapter> {

    @Override
    protected int getListItemLayoutId() {
        return R.layout.mail_list_item;
    }

    @Override
    protected UI.Screen getScreen() {
        return UI.Screen.MAILBOX;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        try {
            final Activity activity = getActivity();

            final ListView mailListView = (ListView) rootView.findViewById(getListViewId());
            mailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        MailItem mail = getListAdapterInstance().getItem(position);
                        mail.setIsRead(true);
                        MailConfig config = MailConfigPreferences.loadConfig();
                        config.updateMailById(mail.getId(), mail);
                        MailConfigPreferences.saveConfiguration(config);

                        Intent intent = new Intent(OfficeApplication.getContext(), MailItemActivity.class);
                        intent.putExtra(getActivity().getString(R.string.intent_mail_key), mail);
                        activity.startActivity(intent);

                    } catch (Exception e) {
                        Logger.logApplicationException(e, getClass().getSimpleName() + ".listView.onItemClick(): Error.");
                    }
                }
            });
            registerForContextMenu(mailListView);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateView(): Error.");
        }

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        try {
            if (v.getId() == getListViewId()) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.event_menu, menu);
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateView(): Error.");
        }
    }

    @Override
    protected List<MailItem> getListData() {
        try {
            MailConfig config = MailConfigPreferences.loadConfig();
            boolean isValidList = false;
            if (config != null) {
                List<MailItem> mails = config.getMails();
                isValidList = mails != null && !mails.isEmpty();
                if (isValidList) {
                    return mails;
                }
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".getListData(): Error.");
        }
        return null;
    }

    @Override
    public MailItemAdapter getListAdapterInstance(List<MailItem> data) {
        try {
            if (mAdapter == null) {
                mAdapter = new MailItemAdapter(getActivity(), getListItemLayoutId(), data != null ? data : getListData());
            }
            return mAdapter;
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".getListAdapter(): Error.");
        }
        return null;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.mail_menu_send:
                final String id = getListAdapterInstance().getItem(info.position).getId();
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        Me.getMessages().get(id).send();
                    }
                });
                MailConfig config = MailConfigPreferences.loadConfig();
                config.removeMailById(id);
                MailConfigPreferences.saveConfiguration(config);
                getListAdapterInstance().remove(info.position);
                getListAdapterInstance().notifyDataSetChanged();
                ((TextView) getListFooterViewInstance().findViewById(R.id.footer_item_count)).setText(String.valueOf(config.getMails().size()));
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initList() {
        try {
            // Update list from the web.
            NetworkState nState = NetworkUtils.getNetworkState(getActivity());
            if (nState.getWifiConnectedState() || nState.getDataState() == NetworkUtils.NETWORK_UTILS_CONNECTION_STATE_CONNECTED) {

                //TODO: wrap this implementation
                final Future<ArrayList<IMessage>> emails = new AsyncCall<ArrayList<IMessage>>(
                		ODataClientFactory.getV4().getConfiguration()) {
                    @Override
                    public ArrayList<IMessage> call() {
                        IFolder drafts = Me.getDrafts();
                        IMessages messages = drafts.getMessages();
                        // if this is not a first call, drafts.getMessages() returned CACHED copy of messages and this copy will be
                        // passed to ArrayList constructor so we need to update them here
                        messages.fetch();
                        return new ArrayList<IMessage>(messages);
                    }
                };

                new AsyncTask<Future<ArrayList<IMessage>>, Void, Void>() {
                    @Override
                    protected Void doInBackground(final Future<ArrayList<IMessage>>... params) {
                        try {
                            final ArrayList<IMessage> result = emails.get(12000, TimeUnit.SECONDS);
                            if (result != null) {
                                onDone(result);
                            } else {
                                onError(new Exception("Error while processing Emails request"));
                            }
                        } catch (final Exception e) {
                            onError(e);
                        } finally {
                            isInitializing = false;
                        }

                        return null;
                    }
                }.execute(emails);
            } else {
                Toast.makeText(getActivity(), R.string.data_connection_no_data_connection, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + "initList(): Error.");
        }
    }

    public void onDone(final List<IMessage> result) {
        MailConfig newConfig = new MailConfig(System.currentTimeMillis());
        final List<MailItem> boxedMails = new ArrayList<MailItem>();
        for (IMessage mail : result) {
            boxedMails.add(new MailItem(mail, getScreen()));
        }

        newConfig.setMails(boxedMails);
        MailConfigPreferences.updateConfiguration(newConfig);
        OfficeApplication.getHandler().post(new Runnable() {
            @Override
            public void run() {
                updateList(boxedMails);
            }
        });
    }

}
