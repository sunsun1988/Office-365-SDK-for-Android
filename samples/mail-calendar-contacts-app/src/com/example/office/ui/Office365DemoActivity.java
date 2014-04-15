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
package com.example.office.ui;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.example.office.Constants;
import com.example.office.Constants.UI;
import com.example.office.Constants.UI.Screen;
import com.example.office.Constants.UI.ScreenGroup;
import com.example.office.OfficeApplication;
import com.example.office.R;
import com.example.office.adapters.SlidingDrawerAdapter;
import com.example.office.auth.OfficeAuthenticator;
import com.example.office.auth.OfficeCredentials;
import com.example.office.logger.Logger;
import com.example.office.storage.AuthPreferences;
import com.example.office.storage.MailConfigPreferences;
import com.example.office.ui.fragments.AuthFragment;
import com.example.office.ui.fragments.CalendarFragment;
import com.example.office.ui.fragments.ContactsFragment;
import com.example.office.ui.fragments.DraftsFragment;
import com.example.office.ui.fragments.ItemsFragment;
import com.example.office.ui.fragments.ListFragment;
import com.example.office.utils.Utility;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.adal.AuthenticationCancelError;
import com.microsoft.adal.AuthenticationSettings;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IEvent;
import com.microsoft.exchange.services.odata.model.types.IEventCollection;
import com.microsoft.exchange.services.odata.model.types.IFileAttachment;

/**
 * Activity that common application UI logic related to Action Bar, Sliding Drawer and Fragments providing main content.
 */
public class Office365DemoActivity extends BaseActivity implements SearchView.OnQueryTextListener, IFragmentNavigator {

    /**
     * Default email box to be used.
     */
    private static final UI.Screen DEFAULT_SCREEN = Screen.MAILBOX;

    /**
     * Tag to pass current fragment.
     */
    private static final String STATE_FRAGMENT_TAG = "current_fragment_tag";

    /**
     * Search view.
     */
    private SearchView mSearchView;

    /**
     * Left-side drawer.
     */
    private DrawerLayout mDrawerLayout;

    /**
     * List view to populate the drawer.
     */
    private ListView mDrawerList;

    /**
     * Toggle t ohandle the Drawer
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Title displayed in the action bar.
     */
    private CharSequence mTitle;

    /**
     * Title displayed when the drawer is shown.
     */
    private CharSequence mDrawerTitle;

    /**
     * Current fragment tag.
     */
    private static String mCurrentFragmentTag;

    /**
     * Saved fragment tag.
     */
    private String mSavedFragmentTag = null;

    /**
     * Indicates if activity UI elements have been initialized.
     */
    private boolean mIsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthenticator = new OfficeAuthenticator(this) {
            @Override
            @SuppressWarnings("rawtypes")
            public void onError(Throwable error) {
                super.onError(error);
                if (error instanceof AuthenticationCancelError) {
                    // user is not logged in
                    finish();
                } else {
                    String message = error.getMessage();
                    // make a message more human-readable: add a dot at the end if it is absent and suggest to retry
                    if (!TextUtils.isEmpty(message)) {
                        if (!message.endsWith(".")) {
                            message += ".";
                        }
                    } else {
                        message = "Error during authentication.";
                    }
                    new AlertDialog.Builder(mActivity)
                            .setTitle("Error during authentication")
                            .setMessage(message + " Would you like to retry?")
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.no, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    finish();
                                }
                            }).setPositiveButton(android.R.string.yes, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    tryAuthenticate();
                                }
                            }).show();
                }

                // Propagate to current fragment
                ItemsFragment fragment = ((ItemsFragment) getCurrentFragment());
                if (fragment != null) {
                    fragment.onError(error);
                }
            }
        };
        
        try {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();

            //Processing system intent of sharing an image
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("image/")) {
                    Bundle bundle = intent.getExtras();
                    Uri uri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                    // Retrieving path to the image and attaching it to current intent
                    attachImageToCurrentEvent(Utility.getRealPathFromURI(uri, this), type);
                }
            }

            if (savedInstanceState == null) {
                mSavedFragmentTag = null;
            } else {
                mSavedFragmentTag = savedInstanceState.getString(STATE_FRAGMENT_TAG);
            }

            setConfiguration();
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onNewIntent(): Error.");
        }
    }

    private void attachImageToCurrentEvent(final String url, final String type) {
        OfficeApplication.getHandler().post(new Runnable() {
            public void run() {
                Utility.showToastNotification("Start uploading");
            }
        });
        Futures.addCallback(Me.getEvents().getAllAsync(), new FutureCallback<IEventCollection>() {
            @Override
            public void onFailure(Throwable t) {
                OfficeApplication.getHandler().post(new Runnable() {
                    public void run() {
                        Utility.showToastNotification("Error during uploading file");
                    }
                });
            }
            @Override
            public void onSuccess(IEventCollection events) {
                try {
                    for (IEvent e: events) {
                        Date currentDate = new Date(System.currentTimeMillis());
                        if (e.getStart().getTimestamp().before(currentDate) && e.getEnd().getTimestamp().after(currentDate)) {
                            Bitmap bmp = Utility.compressImage(url, Utility.IMAGE_MAX_SIDE);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bmp.compress(CompressFormat.JPEG, 100, stream);

                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(currentDate);
                            String imageFileName = "JPEG_" + timeStamp + ".jpg";

                            // Attach an image to event
                            IFileAttachment attachment = e.getAttachments().newFileAttachment();
                            attachment.setContentBytes(stream.toByteArray()).setName(imageFileName);

                            // Propagate changes to server
                            Me.flush();

                            // Provide visual feedback to the user
                            OfficeApplication.getHandler().post(new Runnable() {
                                public void run() {
                                    Utility.showToastNotification("Uploaded successfully");
                                }
                            });
                            return;
                        }
                    }
                } catch (Exception e) {
                    onFailure(e);
                }
            }
        });
    }

    /**
     * Initializes activity UI elements.
     */
    private void initUi() {
        if (!mIsInitialized) {
            setContentView(R.layout.main_activity);

            mTitle = mDrawerTitle = getTitle();

            // Setting up Action Bar and Tabs.
            ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setLogo(R.drawable.ic_action_mail);

            Tab tab = actionBar.newTab().setText(UI.Screen.CONTACTS.getName(this)).setTag(UI.Screen.CONTACTS.getName(this))
                    .setTabListener(new TabListener<ContactsFragment>(this, UI.Screen.CONTACTS.getName(this), ContactsFragment.class));
            actionBar.addTab(tab);

            tab = actionBar.newTab().setText(UI.Screen.MAILBOX.getName(this)).setTag(UI.Screen.MAILBOX.getName(this))
                    .setTabListener(new TabListener<DraftsFragment>(this, UI.Screen.MAILBOX.getName(this), DraftsFragment.class));
            actionBar.addTab(tab, true);

            tab = actionBar.newTab().setText(UI.Screen.CALENDAR.getName(this)).setTag(UI.Screen.CALENDAR.getName(this))
                    .setTabListener(new TabListener<CalendarFragment>(this, UI.Screen.CALENDAR.getName(this), CalendarFragment.class));
            actionBar.addTab(tab);

            // Setting up sliding drawer.
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
            mDrawerList = (ListView) findViewById(R.id.left_drawer);

            SlidingDrawerAdapter drawerAdapter = new SlidingDrawerAdapter(OfficeApplication.getContext(), R.layout.drawer_list_item, R.layout.drawer_delimiter);
            mDrawerList.setAdapter(drawerAdapter);

            mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        Screen[] drawerScreens = ScreenGroup.DRAWER.getMembers().toArray(new Screen[0]);
                        Screen currentScreen = DEFAULT_SCREEN;

                        // use id instead of position here because some positions used by delimiters, id contains real index of clicked item
                        if (drawerScreens != null && drawerScreens.length - 1 >= id) {
                            currentScreen = drawerScreens[(int) id];
                        }
                        switchScreen(currentScreen);
                    } catch (Exception e) {
                        Logger.logApplicationException(e, getClass().getSimpleName() + "onItemClick(): Error.");
                    }
                }
            });

            actionBar.setHomeButtonEnabled(true);

            // ActionBarDrawerToggle ties together the the proper interactions
            // between the sliding drawer and the action bar app icon
            mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */mDrawerLayout, /* DrawerLayout object */
            R.drawable.ic_drawer, /* drawer navigation image replacing '<' */
            R.string.drawer_open, R.string.drawer_close) {
                public void onDrawerClosed(View view) {
                    getActionBar().setTitle(mTitle);
                    invalidateOptionsMenu();
                }

                public void onDrawerOpened(View drawerView) {
                    getActionBar().setTitle(mDrawerTitle);
                    invalidateOptionsMenu();
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mIsInitialized = true;
        }

        if (mSavedFragmentTag == null) {
            switchScreen(DEFAULT_SCREEN);
        } else {
            // This is not used mostly as we're going back to this activity when it
            // is at the top of the back stack. So (as it does have 'singleTop' in parameters) it is
            // simply restored and onNewIntent() is called instead of onCreate().
            // So savedInstanceState will usually be null. This is added to anticipate other future use cases.
            switchScreen(Screen.getByTag(mSavedFragmentTag, this));
        }
    }

    /**
     * Sets application configuration, like endpoint and credentials.
     */
    public void setConfiguration() {
        com.microsoft.office.core.Configuration.setServerBaseUrl(getEndpoint());

        // AADAL caches retrieved tokens in SharedPreferences and encrypts them using provided key.
        // Key must be 32 bytes length.
        // You must use the same key for encryption/decryption during whole application life cycle.
        AuthenticationSettings.INSTANCE.setSecretKey(Constants.STORAGE_KEY);

        tryAuthenticate();
    }

    /**
     * Checks if user has logged in. If yes shows main activity content, otherwise starts authentication flow.
     */
    private void tryAuthenticate() {
        OfficeCredentials creds = (OfficeCredentials) AuthPreferences.loadCredentials();
        // First time
        if (creds == null) {
            creds = createNewCredentials();
        }

        setAuthenticator(getAuthenticator());

        // Try to obtain authentication token using ADAL
        if (creds.getToken() == null) {
            try {
                mAuthenticator.acquireToken(this);
            } catch (Exception e) {
                mAuthenticator.onError(e);
            }
        } else {
            initUi();
        }
    }

    /**
     * Returns end point to retrieve list of emails in the Drafts.
     *
     * @return URL to retrieve list of emails in the inbox.
     */
    private String getEndpoint() {
        return Constants.OUTLOOK_ODATA_ENDPOINT;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FRAGMENT_TAG, mCurrentFragmentTag);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_inbox, menu);

            // search
            MenuItem searchItem = menu.findItem(R.id.inbox_menu_search);
            mSearchView = (SearchView) searchItem.getActionView();
            mSearchView.setOnQueryTextListener(this);

            searchItem.setOnActionExpandListener(new OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    return true; // Return true to collapse action view
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true; // Return true to expand action view
                }
            });

            // logout
            MenuItem logoutItem = menu.findItem(R.id.menu_log_out);
            logoutItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_log_out:
                            // remove stored credentials
                            AuthPreferences.storeCredentials(null);
                            // remove stored mails
                            MailConfigPreferences.saveConfiguration(null);
                            // clear authentication context
                            resetToken();
                            clearCookies();
                            // clear currently displayed items
                            ((ItemsFragment) getCurrentFragment()).updateList(Collections.emptyList());
                            // clear default screen
                            ((ItemsFragment) getFragmentManager().findFragmentByTag(DEFAULT_SCREEN.getName(Office365DemoActivity.this))).updateList(Collections.emptyList());
                            // notify current fragment that user logged out
                            ((ItemsFragment) getCurrentFragment()).notifyUserLoggedOut();

                            // open authentication activity
                            tryAuthenticate();

                        default:
                            return false;
                    }
                }

            });

            return true;
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateOptionsMenu(): Error.");
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            // The action bar home/up action should open or close the drawer.
            if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }

            switch (item.getItemId()) {
                case R.id.inbox_menu_search: {
                    mSearchView.setIconified(false);
                    return true;
                }

                default: {
                    return super.onOptionsItemSelected(item);
                }
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onOptionsItemSelected(): Error.");
            return false;
        }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            // Hide search if drawer is open
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
            menu.findItem(R.id.inbox_menu_search).setVisible(!drawerOpen);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onPrepareOptionsMenu(): Error.");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            @SuppressWarnings("rawtypes")
            ItemsFragment fragment = ((ItemsFragment) getCurrentFragment());
            if (fragment != null) {
                boolean result;
                result = fragment.onKeyDown(keyCode, event);

                return result ? true : super.onKeyDown(keyCode, event);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Choose one of the available screens to display (via appropriate Fragment).
     *
     * @param newScreen Screen to be shown.
     */
    private void switchScreen(UI.Screen newScreen) {
        try {
            ActionBar actionBar = getActionBar();

            mDrawerList.setItemChecked(newScreen.ordinal(), true);
            setTitle(newScreen.getName(this));
            actionBar.setLogo(newScreen.getIcon(this));

            if (newScreen.in(ScreenGroup.MAIL)) {
                Screen currentScreen = Screen.getByTag(mCurrentFragmentTag, this);
                if (!currentScreen.in(ScreenGroup.MAIL)) {
                    Fragment newFragment;
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    switch (newScreen) {
                        case CONTACTS:
                            newFragment = new ContactsFragment();
                            break;
                        case MAILBOX:
                            newFragment = new DraftsFragment();
                            break;
                        case CALENDAR: {
                            newFragment = new CalendarFragment();
                            break;
                        }
                        default: {
                            newFragment = new DraftsFragment();
                            break;
                        }
                    }
                    fragmentTransaction.add(R.id.content_pane, newFragment, newScreen.getName(this));
                    fragmentTransaction.commit();
                }
                actionBar.selectTab(actionBar.getTabAt(newScreen.ordinal()));
                mCurrentFragmentTag = newScreen.getName(this);
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".switchBox(): Error.");
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        try {
            mTitle = title;
            getActionBar().setTitle(mTitle);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".setTitle(): Error.");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            // Sync state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onPostCreate(): Error.");
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onConfigurationChanged(): Error.");
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        try {
            ListFragment<?, ?> fragment = (ListFragment<?, ?>) getCurrentFragment();
            if (fragment != null) {
                fragment.onQueryTextChange(query);
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + "onQueryTextSubmit(): Error.");
        }
        return false;
    }

    @Override
    protected AuthFragment getCurrentFragment() {
        FragmentManager manager = getFragmentManager();
        if (!TextUtils.isEmpty(mCurrentFragmentTag)) {
            Fragment fragment = manager.findFragmentByTag(mCurrentFragmentTag);
            if (fragment != null) {
                return (AuthFragment) fragment;
            }
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mAuthenticator != null) {
            mAuthenticator.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Resets authentication token.
     */
    private void resetToken() {
        if (mAuthenticator == null) {
            return;
        } else {
            Log.i(Constants.APP_TAG, "Logging out");
            mAuthenticator.reset();
        }
    }

    /**
     * Clears cookies used for AADAL authentication.
     */
    private void clearCookies() {
        CookieSyncManager.createInstance(getApplicationContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();
    }

    /**
     * Manages tab interaction and content.
     *
     * @param <T> Class extending {@link Fragment} to be managed as a tab content.
     */
    private static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final BaseActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /**
         * Constructor used each time a new tab is created.
         *
         * @param activity The host Activity, used to instantiate the fragment
         * @param tag The identifier tag for the fragment
         * @param clazz The fragment's Class, used to instantiate the fragment
         */
        public TabListener(BaseActivity activity, String tag, Class<T> clazz) {
            mActivity = activity;
            mTag = tag;
            mClass = clazz;
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction transaction) {
            try {
                if (mFragment == null) {
                    mFragment = Fragment.instantiate(mActivity, mClass.getName());
                    transaction.add(R.id.content_pane, mFragment, mTag);
                } else {
                    transaction.attach(mFragment);
                }

                ActionBar actionBar = mActivity.getActionBar();
                actionBar.setIcon(Screen.getByTag(mTag, mActivity).getIcon(mActivity));
                actionBar.setTitle(Screen.getByTag(mTag, mActivity).getName(mActivity));

                mCurrentFragmentTag = mTag;
            } catch (Exception e) {
                Logger.logApplicationException(e, getClass().getSimpleName() + ".onTabSelected(): Error.");
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            try {
                if (mFragment != null) {
                    ft.detach(mFragment);
                }
            } catch (Exception e) {
                Logger.logApplicationException(e, getClass().getSimpleName() + ".onTabUnselected(): Error.");
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {}
    }

    @Override
    public void setCurrentFragmentTag(String tag) {
        if (!TextUtils.isEmpty(tag)) {
            mCurrentFragmentTag = tag;
        }
    }

    @Override
    public String getCurrentFragmentTag() {
        return mCurrentFragmentTag;
    }
    
    @Override
    public void onAuthenticated() {
        runOnUiThread(new Runnable() {
            public void run() {
                initUi();
            }
        });

        setAuthenticator(getAuthenticator());

        @SuppressWarnings("rawtypes")
        ItemsFragment fragment = ((ItemsFragment) getCurrentFragment());
        if (fragment != null) {
            fragment.notifyTokenAcquired();
        }
    }
}
