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
package com.example.office.ui.mail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.example.office.R;
import com.example.office.data.MailConfig;
import com.example.office.data.MailItem;
import com.example.office.logger.Logger;
import com.example.office.storage.MailConfigPreferences;
import com.example.office.ui.fragments.AuthFragment;
import com.example.office.utils.Utility;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.BodyType;
import com.microsoft.exchange.services.odata.model.types.IFileAttachment;
import com.microsoft.exchange.services.odata.model.types.IMessage;
import com.microsoft.exchange.services.odata.model.types.Importance;
import com.microsoft.exchange.services.odata.model.types.Recipient;

/**
 * Email details fragment.
 */
public class MailItemFragment extends AuthFragment {

    private String mId;

    private byte[] mImageBytes;

    private String mFilename;

    /**
     * Currently displayed email
     */
    private MailItem mail;

    protected LayoutInflater mInflater;

    protected int getFragmentLayoutId() {
        return R.layout.mail_item_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = inflater.inflate(getFragmentLayoutId(), container, false);
        setHasOptionsMenu(true);

        try {
            Activity activity = getActivity();
            activity.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

            Intent intent = getActivity().getIntent();
            mail = (MailItem) intent.getExtras().get(getActivity().getString(R.string.intent_mail_key));
            displayMail(rootView);
            getActivity().setProgressBarIndeterminateVisibility(false);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreateView(): Error.");
        }

        return rootView;
    }

    /**
     * Fills fragment content with email fields
     *
     * @param root Root view for current fragment
     */
    private void displayMail(View root) {
        try {
            TextView subjectView = (TextView) root.findViewById(R.id.mail_fragment_subject);
            subjectView.setText(mail.getSubject());

            StringBuilder recipients = new StringBuilder(getActivity().getString(R.string.me_and_somebody_text_stub));
            if (mail.getRecipients() != null && mail.getRecipients().size() > 0) {
                for (Recipient r: mail.getRecipients()) {
                    recipients.append(r.getName());
                }
            } else {
                recipients.append("<unknown>");
            }
            TextView participantsView = (TextView) root.findViewById(R.id.mail_fragment_participants);
            participantsView.setText(recipients.toString());


            TextView dateView = (TextView) root.findViewById(R.id.mail_fragment_date);
            dateView.setText("");

            WebView webview = (WebView) root.findViewById(R.id.mail_fragment_content);
            if (mail.getBody().getContentType() == BodyType.HTML) {
                webview.loadData(mail.getBody().getContent(), getActivity().getString(R.string.mime_type_text_html), "utf8");
            } else {
                webview.loadData(mail.getBody().getContent(), getActivity().getString(R.string.mime_type_text_plain), "utf8");
            }
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".displayMail(): Error.");
        }
    }

    /**
     * Sets and saves current mail importance
     *
     * @param importance Indicates new importance.
     */
    public void setEmailImportance(Importance importance) {
        try {
            mail.setImportance(importance);
            MailConfig config = MailConfigPreferences.loadConfig();
            config.updateMailById(mail.getId(), mail);
            MailConfigPreferences.saveConfiguration(config);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".setEmailImportance(): Error.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MailItemActivity.CAMERA_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String currentPhotoPath = ((MailItemActivity) getActivity()).getCurrentPhotoPath();
                        Bitmap bmp = BitmapFactory.decodeFile(currentPhotoPath);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(CompressFormat.JPEG, 100, stream);

                        MailItem mail = (MailItem) getActivity().getIntent().getExtras().get(getString(R.string.intent_mail_key));
                        Utility.showToastNotification("Starting file uploading");
                        mId = mail.getId();
                        mImageBytes = stream.toByteArray();
                        mFilename = StringUtils.substringAfterLast(currentPhotoPath, "/");
                        getMessageAndAttachData();
                    } catch (Exception e) {
                        Utility.showToastNotification("Error during getting image from camera");
                    }

                }
                break;

            case MailItemActivity.SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = data.getData();
                        InputStream imageStream = getActivity().getContentResolver().openInputStream(selectedImage);
                        MailItem mail = (MailItem) getActivity().getIntent().getExtras().get(getString(R.string.intent_mail_key));
                        Utility.showToastNotification("Starting file uploading");
                        mId = mail.getId();
                        mImageBytes = IOUtils.toByteArray(imageStream);
                        mFilename = selectedImage.getLastPathSegment();
                        getMessageAndAttachData();
                    } catch (Throwable t) {
                        Utility.showToastNotification("Error during getting image from file");
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    public void getMessageAndAttachData() {
        Futures.addCallback(Me.getMessages().getAsync(mId), new FutureCallback<IMessage>() {
            @Override
            public void onFailure(Throwable t) {
                if (!onError(t)) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            Utility.showToastNotification("Error during uploading file");
                        }
                    });
                }
            }
            
            @Override
            public void onSuccess(IMessage message) {
                try {
                    IFileAttachment attachment = message.getAttachments().newFileAttachment();
                    attachment.setContentBytes(mImageBytes).setName(mFilename);
                    Me.flush();

                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            Utility.showToastNotification("Uploaded successfully");
                        }
                    });
                } catch (Exception e) {
                    onFailure(e);
                }
            }
        });
    }

    public MailItem getMail() {
        return mail;
    }

}
