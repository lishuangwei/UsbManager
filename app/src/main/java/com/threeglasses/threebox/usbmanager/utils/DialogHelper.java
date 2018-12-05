package com.threeglasses.threebox.usbmanager.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.threeglasses.threebox.usbmanager.R;

/**
 * Created by shuangwei on 18-8-31.
 */

public class DialogHelper {
    public static View createTitle(Context context, int icon, String title) {
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View lyTitle = li.inflate(R.layout.dialog_title, null);
        ImageView vIcon = (ImageView)lyTitle.findViewById(R.id.dialog_title_icon);
        if (icon != 0) {
            vIcon.setBackgroundResource(icon);
        } else {
            vIcon.setVisibility(View.GONE);
        }
        TextView vText = (TextView)lyTitle.findViewById(R.id.dialog_title_text);
        vText.setText(title);
        return lyTitle;
    }
}
