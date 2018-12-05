package com.threeglasses.threebox.usbmanager.asyncTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;

import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import com.threeglasses.threebox.usbmanager.MainActivity;
import com.threeglasses.threebox.usbmanager.R;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by shuangwei on 18-9-5.
 */

public class CopyToUsbTask extends AsyncTask<CopyToUsbTaskParam, Integer, Void> {
    private static final String TAG = CopyToUsbTask.class.getSimpleName();
    private ProgressDialog dialog;
    private CopyToUsbTaskParam param;
    private Activity mActivity;
    private FileSystem mFileSystem;

    private String name;
    private long size = -1;

    public CopyToUsbTask(Activity activity, FileSystem fileSystem) {
        mActivity = activity;
        mFileSystem = fileSystem;
        dialog = new ProgressDialog(mActivity);
        dialog.setCustomTitle(DialogHelper.createTitle(mActivity, 0, mActivity.getString(R.string.dialog_copy_title)));
        dialog.setMessage(mActivity.getString(R.string.dialog_copy_to_usb));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    private void queryUriMetaData(Uri uri) {
        Cursor cursor = mActivity.getContentResolver().query(uri, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            Log.i(TAG, "Display Name: " + name);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (!cursor.isNull(sizeIndex)) {
                size = cursor.getLong(sizeIndex);
            }
            Log.i(TAG, "Size: " + size);
            cursor.close();
        }
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Void doInBackground(CopyToUsbTaskParam... params) {
        long time = System.currentTimeMillis();
        param = params[0];

        queryUriMetaData(param.from);

        if (name == null) {
            String[] segments = param.from.getPath().split("/");
            name = segments[segments.length - 1];
        }
        try {
            UsbFile file = ((MainActivity) mActivity).adapter.getCurrentDir().createFile(name);

            if (size > 0) {
                file.setLength(size);
            }

            InputStream inputStream = mActivity.getContentResolver().openInputStream(param.from);
            OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(file, mFileSystem);

            byte[] bytes = new byte[1337];
            int count;
            long total = 0;

            while ((count = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, count);
                if (size > 0) {
                    total += count;
                    int progress = (int) total;
                    if (size > Integer.MAX_VALUE) {
                        progress = (int) (total / 1024);
                    }
                    publishProgress(progress);
                }
            }

            outputStream.close();
            inputStream.close();
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_usb_success), name));
        } catch (IOException e) {
            Log.e(TAG, "error copying!", e);
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_usb_failed), name));
        }
        Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        dialog.dismiss();
        try {
            ((MainActivity) mActivity).adapter.refresh();
        } catch (IOException e) {
            Log.e(TAG, "Error refreshing adapter", e);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setIndeterminate(false);
        int max = (int) size;
        if (size > Integer.MAX_VALUE) {
            max = (int) (size / 1024);
        }
        dialog.setMax(max);
        dialog.setProgress(values[0]);
    }

}
