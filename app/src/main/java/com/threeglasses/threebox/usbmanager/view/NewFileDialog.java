package com.threeglasses.threebox.usbmanager.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.mjdev.libaums.fs.UsbFile;
import com.threeglasses.threebox.usbmanager.MainActivity;
import com.threeglasses.threebox.usbmanager.R;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;

import java.nio.ByteBuffer;

/**
 * Created by shuangwei on 18-9-5.
 */

public class NewFileDialog extends DialogFragment {
    private static final String TAG = NewFileDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View customTile = DialogHelper.createTitle(activity, 0, getString(R.string.dialog_new_file));
        builder.setCustomTitle(customTile);
        View parentview = activity.getLayoutInflater().inflate(R.layout.input_name_dialog, null);
        final EditText nameEt = parentview.findViewById(R.id.input_name_dialog_edit);
        nameEt.setHint(R.string.dialog_new_file_name_hint);
        final EditText contentEt = parentview.findViewById(R.id.input_content_dialog_edit);
        contentEt.setHint(R.string.dialog_new_file_conetnt_hint);
        TextView msg = parentview.findViewById(R.id.input_name_dialog_message);
        msg.setVisibility(View.VISIBLE);
        msg.setText(R.string.dialog_new_file_msg);
        builder.setView(parentview);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {

                UsbFile dir = activity.adapter.getCurrentDir();
                try {
                    UsbFile file = dir.createFile(nameEt.getText().toString());
                    file.write(0, ByteBuffer.wrap(contentEt.getText().toString().getBytes()));
                    file.close();
                    activity.adapter.refresh();
                    UsbFileHelper.showToast(activity, nameEt.getText() + getString(R.string.create_success));
                } catch (Exception e) {
                    Log.e(TAG, "error creating file!", e);
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
