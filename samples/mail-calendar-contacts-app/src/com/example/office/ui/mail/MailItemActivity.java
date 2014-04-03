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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

import com.example.office.R;
import com.example.office.auth.AbstractOfficeAuthenticator;
import com.example.office.data.MailConfig;
import com.example.office.data.MailItem;
import com.example.office.logger.Logger;
import com.example.office.storage.AuthPreferences;
import com.example.office.storage.MailConfigPreferences;
import com.example.office.ui.BaseActivity;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.types.IMessage;
import com.microsoft.exchange.services.odata.model.types.Importance;
import com.microsoft.office.core.auth.IOfficeCredentials;

/**
 * Activity managing specific email details.
 */
public class MailItemActivity extends BaseActivity {

    /** The Constant CAMERA_REQUEST_CODE. */
    public final static int CAMERA_REQUEST_CODE = 1000;

    /** The Constant SELECT_PHOTO. */
    public final static int SELECT_PHOTO = 1001;

    /** Path to uploaded file. */
    private String mCurrentPhotoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mail_item_activity);
        try {
            ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        } catch (Exception e) {
            Logger.logApplicationException(e, getClass().getSimpleName() + ".onCreate(): Error.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mail_item_options, menu);

        menu.findItem(R.id.action_attach).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_attach:
                        showAttachImageDialog();
                        return true;

                    default:
                        return false;
                }
            }
        });

        menu.findItem(R.id.action_send).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_send:
                        sendMail();
                        finish();
                        return true;

                    default:
                        return false;
                }
            }
        });

        menu.findItem(R.id.action_mark_as_important).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_mark_as_important:
                        Importance importance = getCurrentFragment().getMail().getImportance();
                        if (importance == Importance.High) {
                            importance = Importance.Normal;
                        } else {
                            importance = Importance.High;
                        }

                        getCurrentFragment().setEmailImportance(importance);

                        if (importance == Importance.High) {
                            item.setIcon(android.R.drawable.star_on);
                        } else {
                            item.setIcon(android.R.drawable.star_off);
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Shows a dialog allows user to attach file to a message.
     */
    public void showAttachImageDialog() {
        final Activity that = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence[] sources = { "From Library", "From Camera" };
                AlertDialog.Builder builder = new AlertDialog.Builder(that);
                builder.setTitle("Select an option:").setSingleChoiceItems(sources, 0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                dialog.dismiss();
                                openPhotoSource(item);
                            }

                            private void openPhotoSource(int itemSelected) {
                                switch (itemSelected) {
                                case 0:
                                    invokePhotoLibrayIntent();
                                    break;
                                case 1:
                                    invokeFromCameraIntent();
                                    break;
                                default:
                                    break;
                                }
                            }

                            private void invokeFromCameraIntent() {
                                dispatchTakePictureIntent();
                            }

                            private void invokePhotoLibrayIntent() {
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            }
                        });
                builder.create().show();
            }
        });
    }

    /**
     * Dispatch take picture intent.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Asset", ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    /**
     * Creates the image file.
     *
     * @return the file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressLint("SimpleDateFormat")
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        storageDir.mkdirs(); // avoid ENOENT if this is first run
        File image = File.createTempFile(imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }



    /**
     * Sends email given in intent in different thread.
     */
    public void sendMail() {
        final MailItem mail = (MailItem) getIntent().getExtras().get(getString(R.string.intent_mail_key));
        Futures.addCallback(Me.getMessages().getAsync(mail.getId()), new FutureCallback<IMessage>() {
            @Override
            public void onFailure(Throwable t) {
                Logger.logApplicationException(new Exception(t), getClass().getSimpleName() + ".onContextItemSelected(): Error.");                        
            }
            
            @Override
            public void onSuccess(IMessage msg) {
                msg.send();
            }
        });
        MailConfig config = MailConfigPreferences.loadConfig();
        config.removeMailById(mail.getId());
        MailConfigPreferences.saveConfiguration(config);
    }

    /**
     * Shows toast with error message.
     */
    protected void showErrorDuringSending(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MailItemActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected MailItemFragment getCurrentFragment() {
        return (MailItemFragment) getFragmentManager().findFragmentById(R.id.mail_details);
    }

    /**
     * Gets path to current photo.
     *
     * @return current photo path.
     */
    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    @Override
    public AbstractOfficeAuthenticator getAuthenticator() {
        return new AbstractOfficeAuthenticator() {

            @Override
            protected IOfficeCredentials getCredentials() {
                IOfficeCredentials creds = AuthPreferences.loadCredentials();
                return creds == null ? createNewCredentials() : creds;
            }

            @Override
            protected Activity getActivity() {
                return MailItemActivity.this;
            }

            @Override
            public void onDone(AuthenticationResult result) {
                super.onDone(result);
                AuthPreferences.storeCredentials(getCredentials().setToken(result.getAccessToken()).setRefreshToken(result.getRefreshToken()));
                ((MailItemFragment) getCurrentFragment()).getMessageAndAttachData();
            }
        };
    }

}
