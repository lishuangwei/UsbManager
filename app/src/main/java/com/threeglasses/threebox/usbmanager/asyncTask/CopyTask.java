package com.threeglasses.threebox.usbmanager.asyncTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.threeglasses.threebox.usbmanager.R;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by shuangwei on 18-9-5.
 */

public class CopyTask extends AsyncTask<CopyTaskParam, Integer, Void> {
    private static final String TAG = CopyTask.class.getSimpleName();

    private ProgressDialog dialog;
    private CopyTaskParam param;
    private Activity mActivity;
    private FileSystem mFileSystem;

    public CopyTask(Activity activity, FileSystem fileSystem) {
        mActivity = activity;
        mFileSystem = fileSystem;
        dialog = new ProgressDialog(activity);
        dialog.setCustomTitle(DialogHelper.createTitle(activity, 0, activity.getString(R.string.dialog_copy_title)));
        dialog.setMessage(activity.getString(R.string.dialog_copy_message));
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    protected void onPreExecute() {
        dialog.show();
    }

    protected Void doInBackground(CopyTaskParam... params) {
        long time = System.currentTimeMillis();
        param = params[0];
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(param.to));
            InputStream inputStream = new UsbFileInputStream(param.from);
            byte[] bytes = new byte[mFileSystem.getChunkSize()];
            int count;
            long total = 0;

            Log.d(TAG, "Copy file with length: " + param.from.getLength() + "chunksize:" + bytes);

            while ((count = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, count);
                total += count;
                int progress = (int) total;
                if (param.from.getLength() > Integer.MAX_VALUE) {
                    progress = (int) (total / 1024);
                }
                publishProgress(progress);
            }

            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "error copying!", e);
        }
        Log.d(TAG, "copy time: " + (System.currentTimeMillis() - time));
        return null;
    }

    protected void onPostExecute(Void result) {
        dialog.dismiss();

        Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(param.to.getAbsolutePath());
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri
                .fromFile(file).toString());
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                extension);

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            myIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(mActivity,
                    mActivity.getApplicationContext().getPackageName() + ".provider",
                    file);
        } else {
            uri = Uri.fromFile(file);
        }
        Log.d(TAG, "onPostExecute: uri=" + uri + "mimetype=" + mimetype + "ex=" + extension);
        myIntent.setDataAndType(uri, mimetype);
        try {
            mActivity.startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            UsbFileHelper.showToast(mActivity,mActivity.getString(R.string.open_error));
        }
    }

    protected void onProgressUpdate(Integer... values) {
        int max = (int) param.from.getLength();
        if (param.from.getLength() > Integer.MAX_VALUE) {
            max = (int) (param.from.getLength() / 1024);
        }
        dialog.setMax(max);
        dialog.setProgress(values[0]);
    }
}
