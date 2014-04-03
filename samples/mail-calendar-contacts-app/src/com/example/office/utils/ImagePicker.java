package com.example.office.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.example.office.R;

public abstract class ImagePicker {

    /** The Constant CAMERA_REQUEST_CODE. */
    public final static int CAMERA_REQUEST_CODE = 1000;

    /** The Constant SELECT_PHOTO. */
    public final static int SELECT_PHOTO = 1001;

    /** JPEG compression quality. */
    private final static int JPEG_COMPRESSION_QUALITY = 100;

    /** Default date formatter */
    @SuppressLint("SimpleDateFormat")
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public enum Status {
        UPLOAD_STARTED, UPLOAD_FAILED, UPLOAD_SUCCESS, CAMERA_GET_IMAGE_FAILED, FILE_IMAGE_CREATE_FAIL;
    }

    /** Activity to use for Intent manipulations. */
    public final Activity mActivity;

    /** Path to uploaded file. */
    private String mCurrentPhotoPath = null;

    /** Raw image content. */
    private byte[] mImageBytes;

    /** Picture file name. */
    private String mFilename;

    /** Key to retrieve argument from Intent extras passed on activity result. */
    private String mIntentArgKey;

    /**
     *
     * @param activity Activity to use for Intent manipulations.
     * @param intentArgKey Key to retrieve argument from Intent extras passed on activity result.
     *
     * @throws IllegalArgumentException Thrown if Activity argument is <code>null</code>.
     */
    public ImagePicker(Activity activity, String intentArgKey) throws IllegalArgumentException {
        if(activity == null) throw new IllegalArgumentException("Activity argument can't be null");

        mActivity = activity;
        mIntentArgKey = intentArgKey;
    }

    /**
     * Method that should be implemented by child to process results of image retrieval.
     *
     * @param imageBytes Raw image data.
     * @param fileName Name of the image.
     * @param intentArg Argument retrieved from the intent (intent to take image).
     */
    public abstract void processImage(byte[] imageBytes, String fileName, Object intentArg);

    /**
     * Should be called from within <code>onActivityResult()</code> of your Activity to handle Sytem response.
     * You <b>MUST</b> call your default <code>onActivityResult()</code> method if this returns <code>false</code>.
     *
     * @return <code>true</code> if result has been processed and consumed, <code>false</code> otherwise.
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Bitmap bmp = Utility.compressImage(mCurrentPhotoPath, Utility.IMAGE_MAX_SIDE);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream);

                        Object intentArg = collectIntentArg();
                        showStatusToast(Status.UPLOAD_STARTED);

                        mImageBytes = stream.toByteArray();
                        mFilename = StringUtils.substringAfterLast(mCurrentPhotoPath, "/");

                        processImage(mImageBytes, mFilename, intentArg);
                    } catch (Exception e) {
                        Utility.showToastNotification(mActivity.getString(R.string.camera_image_get_failure));
                    }

                }
                return true;

            case SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = data.getData();
                        InputStream imageStream = mActivity.getContentResolver().openInputStream(selectedImage);
                        Bitmap bmp = Utility.compressImage(IOUtils.toByteArray(imageStream), Utility.IMAGE_MAX_SIDE);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream);

                        Object intentArg = collectIntentArg();
                        showStatusToast(Status.UPLOAD_STARTED);

                        mImageBytes = stream.toByteArray();
                        //TODO: remove adding postfix later since we can't know for sure if image is gonna be saved as jpeg (3-d party camera apps).
                        mFilename = selectedImage.getLastPathSegment() + ".jpeg";

                        processImage(mImageBytes, mFilename, intentArg);
                    } catch (Throwable t) {
                        Utility.showToastNotification(mActivity.getString(R.string.file_image_retrieve_error));
                    }
                }
        }
        return false;
    }

    /**
     * Shows a dialog allows user to attach file to a message.
     */
    public void showAttachImageDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CharSequence[] sources = { "From Library", "From Camera" };
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
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
                                mActivity.startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            }
                        });
                builder.create().show();
            }
        });
    }

    /**
     * Generates toast message sith appropriate message for success/failure cases.
     *
     * @param status Current status to pick appropriate message. No message is generated if argument is <code>null</code>.
     */
    public void showStatusToast(Status status) {
        if(status == null) return;

        // Default value will not be used since switch default option 'returns'.
        int messageId = 0;
        switch (status) {
            case UPLOAD_STARTED:
                messageId = R.string.file_upload_start;
                break;
            case UPLOAD_FAILED:
                messageId = R.string.file_upload_error;
                break;
            case UPLOAD_SUCCESS:
                messageId = R.string.file_upload_success;
                break;
            case CAMERA_GET_IMAGE_FAILED:
                messageId = R.string.camera_image_get_failure;
                break;
            case FILE_IMAGE_CREATE_FAIL:
                messageId = R.string.file_image_create_error;
                break;
            default:
                return;
        }

        final int message = messageId;
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Utility.showToastNotification(mActivity.getString(message));
            }
        });
    }

    /**
     * Creates the image file.
     *
     * @return the file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = DATE_FORMAT.format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        storageDir.mkdirs(); // avoid ENOENT if this is first run
        File image = File.createTempFile(imageFileName, /* prefix */ ".jpg", /* suffix */ storageDir /* directory */);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Dispatch take picture intent.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Failure creating image file.", ex.getMessage());
                showStatusToast(Status.FILE_IMAGE_CREATE_FAIL);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                mActivity.startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    /**
     * Extracts argument from Intent extras.
     *
     * @return Passed argument.
     */
    private Object collectIntentArg() {
        Object intentArg = null;
        if(!TextUtils.isEmpty(mIntentArgKey)) {
            intentArg = mActivity.getIntent().getExtras().get(mIntentArgKey);
        }
        return intentArg;
    }

}
