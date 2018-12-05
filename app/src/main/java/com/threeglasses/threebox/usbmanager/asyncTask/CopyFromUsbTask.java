package com.threeglasses.threebox.usbmanager.asyncTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.util.Log;

import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import com.threeglasses.threebox.usbmanager.MainActivity;
import com.threeglasses.threebox.usbmanager.R;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by shuangwei on 18-9-5.
 */

public class CopyFromUsbTask extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = CopyFromUsbTask.class.getSimpleName();
    private ProgressDialog dialog;
    private Activity mActivity;
    private FileSystem mFileSystem;

    private UsbFile usbFile;
    private long size = -1;

    public CopyFromUsbTask(UsbFile file, FileSystem fileSystem, Activity activity) {
        mFileSystem = fileSystem;
        mActivity = activity;
        usbFile = file;
        size = file.getLength();
        dialog = new ProgressDialog(mActivity);
        dialog.setCustomTitle(DialogHelper.createTitle(mActivity, 0, mActivity.getString(R.string.dialog_copy_title)));
        dialog.setMessage(mActivity.getString(R.string.dialog_copy_to_local));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }


    private String initSavePath() {
        File f = new File(UsbFileHelper.COPYPATH);
        f.mkdirs();
        return f.getAbsolutePath();
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        long time = System.currentTimeMillis();
        try {
            String savepath = initSavePath() + File.separator + usbFile.getName();
            Log.d(TAG, "doInBackground: savepath=" + savepath);
            InputStream uis = UsbFileStreamFactory.createBufferedInputStream(usbFile, mFileSystem);
            FileOutputStream fos = new FileOutputStream(savepath);
            byte[] bytes = new byte[1337];
            int count;
            long total = 0;

            while ((count = uis.read(bytes)) != -1) {
                fos.write(bytes, 0, count);
                if (size > 0) {
                    total += count;
                    int progress = (int) total;
                    if (size > Integer.MAX_VALUE) {
                        progress = (int) (total / 1024);
                    }
                    publishProgress(progress);
                }
            }
            MediaScannerConnection.scanFile(mActivity, new String[]{savepath}, null, null);
            fos.close();
            uis.close();
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_local_success), savepath));
        } catch (IOException e) {
            Log.e(TAG, "error copying!", e);
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_usb_failed), usbFile.getName()));
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
