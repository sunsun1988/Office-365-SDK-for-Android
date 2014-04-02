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

import java.util.List;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.office.Constants;
import com.example.office.Constants.UI;
import com.example.office.OfficeApplication;
import com.example.office.R;
import com.example.office.adapters.SearchableAdapter;
import com.example.office.logger.Logger;
import com.example.office.ui.Office365DemoActivity;
import com.example.office.utils.Utility;

/**
 * Base fragment containing logic related to managing items.
 * 
 * @author maxim.kostin
 * 
 * @param <T>
 */
public abstract class ItemsFragment<T, A extends SearchableAdapter<T>> extends ListFragment<T, A> {

    /**
     * View used as a footer of the list;
     */
    protected View mListFooterView;

    /**
     * Layout inflater to inflate footer when mails list is being populated
     */
    protected LayoutInflater mInflater;

    /**
     * Indicates if current fragment currently initializes its content.
     */
    protected boolean isInitializing = false;

    @Override
    protected int getFragmentLayoutId() {
        return R.layout.list_fragment;
    }

    @Override
    protected int getListViewId() {
        return R.id.list;
    }

    @Override
    protected int getProgressViewId() {
        return R.id.list_progress;
    }

    @Override
    protected int getContentContainerId() {
        return R.id.list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = inflater.inflate(getFragmentLayoutId(), container, false);

        try {
            mListFooterView = getListFooterViewInstance();
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
    protected View getListFooterViewInstance() {
        try {
            if (mListFooterView == null) {
                mListFooterView = mInflater.inflate(R.layout.list_footer, null);
                ((TextView) mListFooterView.findViewById(R.id.footer_item_count)).setText(String.valueOf(getListAdapterInstance().getCount()));
            }
            return mListFooterView;
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".getListFooterView(): Error.");
        }
        return null;
    }

    /**
     * Sets footer to given ListView with given number of items.
     * 
     * @param listView ListView to set footer.
     * @param count number of items to be set as footer text.
     */
    protected void setFooter(ListView listView, int count) {
        if (mListFooterView != null) {
            if (count <= 0) {
                listView.removeFooterView(mListFooterView);
            } else {
                if (listView.getFooterViewsCount() == 0) {
                    listView.addFooterView(mListFooterView);
                }
                ((TextView) mListFooterView.findViewById(R.id.footer_item_count)).setText(String.valueOf(count));
            }
        }
    }

    /**
     * Returns {@link Constants.UI.Screen} that this fragment is describing.
     * 
     * @return Screen for this fragment, or <code>null</code> in case of error.
     */
    protected abstract UI.Screen getScreen();

    /**
     * To make super.onKeyDown() be called after your code return <code>false</code>. Otherwise return <code>true</code> and
     * <code>true</code> will be returned as a result of activity method.
     * 
     * @param keyCode Key code.
     * @param event Key event.
     * 
     * @return <code>true</code> to call super implementation, <code>false</code> otherwise.
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Notifies current fragment that access token is retrieved and fragment can begin requesting data from server.
     */
    public void notifyTokenAcquired() {
        mHasToken = true;
        initList();
    }

    /**
     * Notifies current fragment that user has logged out.
     */
    public void notifyUserLoggedOut() {
        mHasToken = false;
        // TODO cancel all background tasks.
        // Otherwise when task of current user will be finished its result will be displayed to next logged in.
    }

    /**
     * Updates list with new data.
     * 
     * @param items Items to be displayed in the list.
     */
    public void updateList(final List<T> items) {
        OfficeApplication.getHandler().post(new Runnable() {
            public void run() {
                try {
                    View rootView = getView();
                    showWorkInProgress(false, false);

                    getListAdapterInstance().update(items);

                    if (rootView != null) {

                        ListView mailListView = (ListView) rootView.findViewById(getListViewId());
                        // set footer before adapter, see http://stackoverflow.com/a/4318907
                        setFooter(mailListView, getListAdapterInstance().getCount());

                        if (mailListView != null) {
                            mailListView.setAdapter(getListAdapterInstance());
                        }
                    }
                } catch (Exception e) {
                    Logger.logApplicationException(e, getClass().getSimpleName() + ".updateList(): Error.");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setLogo(getScreen().getIcon(getActivity()));

        List<T> items = getListData();
        boolean hasData = (items != null && !items.isEmpty());
        if (hasData) {
            updateList(items);
        }

        // prevent initialization start on activity resume
        if (((Office365DemoActivity) getActivity()).getCurrentFragmentTag().equals(getScreen().getName(getActivity())) && !isInitializing && mHasToken) {
            isInitializing = true;
            initList();
        }

        if (isInitializing) {
            showWorkInProgress(true, !hasData);
        }
    }

    @Override
    public boolean onError(final Throwable e) {
        // first check for access token expiration
        if (!super.onError(e)) {
            Logger.logApplicationException(new Exception(e), getClass().getSimpleName() + ".onExecutionComplete(): Error.");
            OfficeApplication.getHandler().post(new Runnable() {
                public void run() {
                    showWorkInProgress(false, false);
                    Utility.showToastNotification(getActivity().getString(R.string.mails_retrieving_failure_message));
                }
            });
        }
        return true;
    }
}
