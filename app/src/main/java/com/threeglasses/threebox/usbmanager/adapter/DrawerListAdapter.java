package com.threeglasses.threebox.usbmanager.adapter;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.threeglasses.threebox.usbmanager.R;

/**
 * Created by shuangwei on 18-8-31.
 */

public class DrawerListAdapter extends ArrayAdapter<String> {
    UsbMassStorageDevice[] devices;
    private LayoutInflater inflater;
    public DrawerListAdapter(Context context, UsbMassStorageDevice[] devices) {
        super(context, R.layout.drawer_list_item);
        this.devices = devices;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Nullable
    @Override
    public String getItem(int position) {
        String title;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsbDevice usbDevice = devices[position].getUsbDevice();
            title = usbDevice.getManufacturerName() + " " + usbDevice.getProductName();
        } else {
            title = getContext().getString(R.string.storage_root);
        }
        return title;
    }

    @Override
    public int getCount() {
        return devices.length;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.drawer_list_item, parent, false);
        }
        TextView device = (TextView) view.findViewById(R.id.drawer_item_name);
        device.setText(getItem(position));
        return view;
    }
}
