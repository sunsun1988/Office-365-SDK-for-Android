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
package com.example.office365sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationSettings;
import com.microsoft.exchange.services.odata.model.IMessages;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.microsoft.exchange.services.odata.model.types.IMessage;
import com.microsoft.exchange.services.odata.model.types.Recipient;
import com.microsoft.office.core.Configuration;
import com.microsoft.office.core.auth.method.IAuthenticator;
import com.microsoft.office.core.net.NetworkException;
import com.msopentech.org.apache.http.client.HttpClient;
import com.msopentech.org.apache.http.client.methods.HttpUriRequest;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static final int MENU_RESET_TOKEN = Menu.FIRST;
    private static final int MENU_LOGIN = MENU_RESET_TOKEN + 1;
    private static final int MENU_READ_MESSAGES = MENU_LOGIN + 1;

    ListView listViewMessages = null;
    Button btnSendMessage = null;

    private AuthenticationContext mAuthContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate views and set initial state
        btnSendMessage = (Button) findViewById(R.id.button_sendSelectedMessage);
        btnSendMessage.setEnabled(false);
        listViewMessages = (ListView) findViewById(R.id.listview_Messages);
        listViewMessages.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Configuration.setServerBaseUrl(Constants.RESOURCE_ID + Constants.ODATA_ENDPOINT);
        AuthenticationSettings.INSTANCE.setSecretKey(Constants.STORAGE_KEY);

        listViewMessages.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new String[] { "Loading..." }));

        btnSendMessage.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                IMessage selectedMessage;
                int position = listViewMessages.getCheckedItemPosition();
                Log.i(TAG, "Selected Item: " + position);
                try {
                    selectedMessage = ((ArrayAdapter<IMessage>) listViewMessages.getAdapter()).getItem(position);
                } catch (ClassCastException e) {
                    Log.i(TAG, "Adapter not set properly");
                    throw e;
                }

                Log.i(TAG, "Sending message: " + selectedMessage.getSubject() + "\nto: " + selectedMessage.getToRecipients());
                Futures.addCallback(selectedMessage.sendAsync(), new FutureCallback<Void>() {
                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }
                    @Override
                    public void onSuccess(Void result) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        readMessages();
                    }
                });

            }
        });

        doLogin();
    }

    protected void doLogin() {
        try {
            mAuthContext = new AuthenticationContext(this, Constants.AUTHORITY_URL, false);
            mAuthContext.acquireToken(this, Constants.RESOURCE_ID, Constants.CLIENT_ID, Constants.REDIRECT_URL, Constants.USER_HINT,
                    new AuthenticationCallback<AuthenticationResult>() {

                        @Override
                        public void onSuccess(final AuthenticationResult result) {
                            if (result != null && !TextUtils.isEmpty(result.getAccessToken())) {

                                Configuration.setAuthenticator(new IAuthenticator() {
                                    @Override
                                    public void prepareRequest(HttpUriRequest request) {
                                        request.addHeader("Authorization", "Bearer " + result.getAccessToken());
                                    }

                                    @Override
                                    public void prepareClient(HttpClient client) throws NetworkException {}

                                });
                                Log.i(TAG, "Received access token, ready to work.");
                                MainActivity.this.readMessages();
                            }
                        }

                        @Override
                        public void onError(Exception exc) {
                            Log.i(TAG, exc.getMessage());
                            listViewMessages.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1,
                                    new String[] { "Error during authentication: ", exc.getMessage() }));
                        }
                    });
        } catch (Exception exc) {
            Log.i(TAG, exc.getMessage());
            listViewMessages.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, new String[] {
                    "Error during authentication:", exc.getMessage() }));
        }
    }

    public void readMessages() {
        Futures.addCallback(Me.getDraftsAsync(), new FutureCallback<IFolder>() {
            @Override
            public void onFailure(Throwable err) {
                Log.d(TAG, "Error retrieving messages: " + err.getMessage());
            }
            
            @Override
            public void onSuccess(IFolder drafts) {
                Futures.addCallback(drafts.getMessagesAsync(), new FutureCallback<IMessages>() {
                    @Override
                    public void onFailure(Throwable err) {
                        Log.d(TAG, "Error retrieving messages: " + err.getMessage());
                    }

                    @Override
                    public void onSuccess(final IMessages messages) {
                        Futures.addCallback(messages.fetchAsync(), new FutureCallback<Void>() {
                            @Override
                            public void onFailure(Throwable err) {
                                Log.d(TAG, "Error retrieving messages: " + err.getMessage());
                            }

                            @Override
                            public void onSuccess(Void arg0) {
                                List<IMessage> list = new ArrayList<IMessage>();

                                for (IMessage message : messages) {
                                    Log.i(TAG, "From: " + message.getFrom() + ";\nSubject: " + message.getSubject());
                                    list.add(message);
                                }

                                updateList(list);
                            }
                        });
                    }
                });
            }
        });
    }

    private void updateList(final List<IMessage> messages) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    ArrayAdapter<IMessage> adapter = new ArrayAdapter<IMessage>(MainActivity.this, android.R.layout.simple_list_item_activated_2,
                            android.R.id.text1, messages) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                            TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                            // get list of recipients and just display first one
                            Collection<Recipient> recipients = messages.get(position).getToRecipients();
                            if (recipients != null && recipients.size() > 0) {
                                Recipient first = ((Recipient) recipients.toArray()[0]);
                                String recipientEmail = first.getAddress();
                                text1.setText("To: " + recipientEmail);
                            } else {
                                text1.setText("<no recipients>");
                            }

                            // show subject
                            text2.setText("Subject: " + messages.get(position).getSubject());
                            return view;
                        }
                    };

                    listViewMessages.setAdapter(adapter);
                    if (!adapter.isEmpty()) {
                        btnSendMessage.setEnabled(true);
                    } else {
                        btnSendMessage.setEnabled(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.add(Menu.NONE, MENU_RESET_TOKEN, Menu.NONE, "Clear Token Cache");
        menu.add(Menu.NONE, MENU_LOGIN, Menu.NONE, "Login");
        menu.add(Menu.NONE, MENU_READ_MESSAGES, Menu.NONE, "Read Messages");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET_TOKEN: {
                resetToken();
                clearCookies();
                return true;
            }
            case MENU_LOGIN: {
                doLogin();
                return true;
            }
            case MENU_READ_MESSAGES: {
                readMessages();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void resetToken() {
        if (mAuthContext == null) {
            return;
        } else {
            Log.i(TAG, "Clearing cached tokens");
            mAuthContext.getCache().removeAll();
        }
    }

    public void clearCookies() {
        CookieSyncManager.createInstance(getApplicationContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mAuthContext != null) {
            mAuthContext.onActivityResult(requestCode, resultCode, data);
        }
    }

}
