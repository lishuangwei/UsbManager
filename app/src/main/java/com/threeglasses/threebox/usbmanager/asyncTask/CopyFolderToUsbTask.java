package com.threeglasses.threebox.usbmanager.asyncTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
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

public class CopyFolderToUsbTask extends AsyncTask<CopyToUsbTaskParam, Integer, Void> {
    private static final String TAG = CopyFolderToUsbTask.class.getSimpleName();
    private ProgressDialog dialog;
    private CopyToUsbTaskParam param;
    private Activity mActivity;
    private FileSystem mFileSystem;

    private long size = -1;
    private DocumentFile pickedDir;

    public CopyFolderToUsbTask(Activity activity, FileSystem fileSystem) {
        mActivity = activity;
        mFileSystem = fileSystem;
        dialog = new ProgressDialog(mActivity);
        dialog.setCustomTitle(DialogHelper.createTitle(mActivity, 0, mActivity.getString(R.string.dialog_copy_afolder)));
        dialog.setMessage(mActivity.getString(R.string.dialog_copy_afolder_message));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    private void copyDir(DocumentFile dir, UsbFile currentUsbDir) throws IOException {
        for (DocumentFile file : dir.listFiles()) {
            Log.d(TAG, "Found file " + file.getName() + " with size " + file.length());
            if (file.isDirectory()) {
                copyDir(file, currentUsbDir.createDirectory(file.getName()));
            } else {
                copyFile(file, currentUsbDir);
            }
        }
    }

    private void copyFile(DocumentFile file, UsbFile currentUsbDir) {
        try {
            UsbFile usbFile = currentUsbDir.createFile(file.getName());
            size = file.length();
            usbFile.setLength(file.length());

            InputStream inputStream = mActivity.getContentResolver().openInputStream(file.getUri());
            OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(usbFile, mFileSystem);

            byte[] bytes = new byte[1337];
            int count;
            long total = 0;

            while ((count = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, count);
                if (size > 0) {
                    total += count;
                    int progress = (int) total;
                    if (file.length() > Integer.MAX_VALUE) {
                        progress = (int) (total / 1024);
                    }
                    publishProgress(progress);
                }
            }
            outputStream.close();
            inputStream.close();
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_usb_success), pickedDir.getName()));
        } catch (IOException e) {
            Log.e(TAG, "error copying!", e);
            UsbFileHelper.showToast(mActivity, String.format(mActivity.getString(R.string.copy_to_usb_failed), pickedDir.getName()));
        }
    }

    @Override
    protected Void doInBackground(CopyToUsbTaskParam... params) {
        long time = System.currentTimeMillis();
        param = params[0];
        pickedDir = DocumentFile.fromTreeUri(mActivity, param.from);

        try {
            copyDir(pickedDir, ((MainActivity) mActivity).adapter.getCurrentDir().createDirectory(pickedDir.getName()));
        } catch (IOException e) {
            Log.e(TAG, "could not copy directory", e);
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
