package com.threeglasses.threebox.usbmanager.utils;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;

/**
 * Created by shuangwei on 18-8-30.
 */

public class UsbFileHelper {
    private static final String TAG = "UsbFileHelper";
    public static final String COPYPATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/3glasses/CopyFiles";
    public static final String CACHEPATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/3glasses/Cache";
    private static Toast mToast;

    public static String getFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " kB";
        } else {
            return (size / 1024 / 1024) + " MB";
        }
    }

    public static String getAbsolutePath(UsbFile file) {
        if (file.getParent().isRoot()) {
            return "/" + file.getName();
        }
        return getAbsolutePath(file.getParent()) + UsbFile.separator + file.getName();
    }

    public static String getDirectoryPath(UsbFile pickfile, UsbFile dir) {
        String ori = COPYPATH + File.separator;
        Log.d(TAG, "getDirectoryPath: ori=" + ori);
        String path = getAbsolutePath(dir);
        Log.d(TAG, "getDirectoryPath: path=" + path);
        String extra = path.substring(path.indexOf(pickfile.getName()));
        Log.d(TAG, "getDirectoryPath: extra=" + extra);
        return ori + extra;
    }

    public static void showToast(final Activity ac, final CharSequence str) {
        ac.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(ac, str, Toast.LENGTH_LONG);
                } else {
                    mToast.setText(str);
                }
                mToast.show();
            }
        });
    }
}
