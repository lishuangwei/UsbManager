package com.threeglasses.threebox.usbmanager.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.github.mjdev.libaums.fs.UsbFile;
import com.threeglasses.threebox.usbmanager.MainActivity;
import com.threeglasses.threebox.usbmanager.R;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;

/**
 * Created by shuangwei on 18-9-5.
 */

public class NewDirDialog extends DialogFragment {
    private static final String TAG = NewDirDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View customTile = DialogHelper.createTitle(activity, 0, getString(R.string.dialog_new_directory));
        builder.setCustomTitle(customTile);
        final EditText input = new EditText(activity);
        input.setHint(R.string.dialog_new_directory_msg);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                UsbFile dir = activity.adapter.getCurrentDir();
                try {
                    dir.createDirectory(input.getText().toString());
                    activity.adapter.refresh();
                    UsbFileHelper.showToast(activity, input.getText() + getString(R.string.create_success));
                } catch (Exception e) {
                    Log.e(TAG, "error creating dir!", e);
                    UsbFileHelper.showToast(activity, getString(R.string.create_fail));
                }

            }

        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        builder.setCancelable(false);
        return builder.create();
    }

}
