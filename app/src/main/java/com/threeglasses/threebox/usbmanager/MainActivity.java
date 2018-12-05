/*
 * (C) Copyright 2014-2016 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.threeglasses.threebox.usbmanager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.threeglasses.threebox.usbmanager.adapter.DrawerListAdapter;
import com.threeglasses.threebox.usbmanager.adapter.UsbFileListAdapter;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyFolderFromUsbTask;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyFolderToUsbTask;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyFromUsbTask;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyTask;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyTaskParam;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyToUsbTask;
import com.threeglasses.threebox.usbmanager.asyncTask.CopyToUsbTaskParam;
import com.threeglasses.threebox.usbmanager.utils.DialogHelper;
import com.threeglasses.threebox.usbmanager.utils.MoveClipboard;
import com.threeglasses.threebox.usbmanager.utils.UsbFileHelper;
import com.threeglasses.threebox.usbmanager.view.NewDirDialog;
import com.threeglasses.threebox.usbmanager.view.NewFileDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.threeglasses.threebox.usbmanager.USB_PERMISSION";

    private static final int COPY_STORAGE_PROVIDER_RESULT = 0;
    private static final int OPEN_STORAGE_PROVIDER_RESULT = 1;
    private static final int OPEN_DOCUMENT_TREE_RESULT = 2;

    private static final int REQUEST_EXT_STORAGE_WRITE_PERM = 0;

    private ListView listView;
    private ListView drawerListView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    public UsbFileListAdapter adapter;
    private Deque<UsbFile> dirs = new ArrayDeque<UsbFile>();
    private FileSystem currentFs;

    UsbMassStorageDevice[] massStorageDevices;
    private int currentDevice = -1;
    private Toolbar mToolbar;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {

                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        Log.d(TAG, "USB device setup device");
                        setupDevice();
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "USB device attached");
                if (device != null) {
                    discoverDevice();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d(TAG, "USB device detached");
                if (device != null) {
                    if (MainActivity.this.currentDevice != -1) {
                        MainActivity.this.massStorageDevices[currentDevice].close();
                    }
                    discoverDevice();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.material_toolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_material_light_navigation_drawer);
        setSupportActionBar(mToolbar);
        getWindow().setStatusBarColor(getResources().getColor(R.color.material_palette_blue_primary_dark));
        listView = (ListView) findViewById(R.id.listview);
        drawerListView = (ListView) findViewById(R.id.left_drawer);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (massStorageDevices.length !=0 && massStorageDevices[currentDevice].getPartitions().size() != 0) {
                    String title = TextUtils.isEmpty(massStorageDevices[currentDevice].getPartitions().get(0).getVolumeLabel())
                            ? massStorageDevices[currentDevice].getUsbDevice().getManufacturerName()
                            : massStorageDevices[currentDevice].getPartitions().get(0).getVolumeLabel();
                    mToolbar.setTitle(title);
                    Log.d(TAG, "onDrawerClosed: " + massStorageDevices[currentDevice].getPartitions().get(0).getVolumeLabel());
                }
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mToolbar.setTitle(getString(R.string.title_device));
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);

        drawerListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: position=" + position);
                drawerLayout.closeDrawers();
                if (currentDevice == position && currentFs != null) {
                    UsbFile root = currentFs.getRootDirectory();
                    try {
                        listView.setAdapter(adapter = new UsbFileListAdapter(MainActivity.this, root));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    selectDevice(position);
                    drawerListView.setItemChecked(position, true);
                }
            }
        });

        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
        discoverDevice();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void selectDevice(int position) {
        currentDevice = position;
        setupDevice();
    }

    private void discoverDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        massStorageDevices = UsbMassStorageDevice.getMassStorageDevices(this);

        if (massStorageDevices.length == 0) {
            Log.w(TAG, "no device found!");
            mToolbar.setTitle(getString(R.string.title_no_device));
            listView.setAdapter(null);
            drawerListView.setAdapter(null);
            return;
        }

        drawerListView.setAdapter(new DrawerListAdapter(this, massStorageDevices));
        drawerListView.setItemChecked(0, true);
        currentDevice = 0;

        UsbDevice usbDevice = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "received usb device via intent");
            setupDevice();
        } else {
            Log.d(TAG, "request permission");
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                    ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(massStorageDevices[currentDevice].getUsbDevice(), permissionIntent);
        }
    }

    private void setupDevice() {
        try {
            massStorageDevices[currentDevice].init();

            if (massStorageDevices[currentDevice].getPartitions().size() > 0) {
                currentFs = massStorageDevices[currentDevice].getPartitions().get(0).getFileSystem();
                Log.d(TAG, "Capacity: " + currentFs.getCapacity());
                Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
                Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
                Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());
                Log.d(TAG, "volume label: " + currentFs.getVolumeLabel());
                UsbFile root = currentFs.getRootDirectory();

                mToolbar.setTitle(currentFs.getVolumeLabel());
                listView.setAdapter(adapter = new UsbFileListAdapter(this, root));
            }
        } catch (IOException e) {
            Log.e(TAG, "error setting up device", e);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MoveClipboard cl = MoveClipboard.getInstance();
        menu.findItem(R.id.paste).setEnabled(cl.getFile() != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.create_file:
                new NewFileDialog().show(getFragmentManager(), "NEW_FILE");
                return true;
            case R.id.create_dir:
                new NewDirDialog().show(getFragmentManager(), "NEW_DIR");
                return true;
            case R.id.paste:
                move();
                return true;
            case R.id.copy_from_storage_provider:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");

                    startActivityForResult(intent, COPY_STORAGE_PROVIDER_RESULT);
                }
                return true;
            case R.id.copy_folder_from_storage_provider:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);

                    startActivityForResult(intent, OPEN_DOCUMENT_TREE_RESULT);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final UsbFile entry = adapter.getItem((int) info.id);
        switch (item.getItemId()) {
            case R.id.delete_item:
                int curNum = adapter.getCount();
                try {
                    entry.delete();
                    adapter.refresh();
                    if (curNum > adapter.getCount()) {
                        Toast.makeText(MainActivity.this, R.string.delete_ok, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.delete_fail, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, R.string.delete_fail, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "error deleting!", e);
                }
                return true;
            case R.id.rename_item:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCustomTitle(DialogHelper.createTitle(MainActivity.this, 0, getString(R.string.rename)));
                builder.setMessage(getString(R.string.rename_msg));
                final EditText input = new EditText(this);
                input.setText(entry.getName());
                builder.setView(input);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            entry.setName(input.getText().toString());
                            adapter.refresh();
                        } catch (IOException e) {
                            Log.e(TAG, "error renaming!", e);
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
                builder.create().show();
                return true;
            case R.id.move_item:
                MoveClipboard cl = MoveClipboard.getInstance();
                cl.setFile(entry);
                return true;
            case R.id.copy_item:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_EXT_STORAGE_WRITE_PERM);
                    return true;
                }
                if (entry.isDirectory()) {
                    new CopyFolderFromUsbTask(entry, currentFs, this).execute();
                } else {
                    new CopyFromUsbTask(entry, currentFs, this).execute();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
        UsbFile entry = adapter.getItem(position);
        try {
            if (entry.isDirectory()) {
                dirs.push(adapter.getCurrentDir());
                listView.setAdapter(adapter = new UsbFileListAdapter(this, entry));
            } else {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, R.string.request_write_storage_perm, Toast.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_EXT_STORAGE_WRITE_PERM);
                    }

                    return;
                }

                CopyTaskParam param = new CopyTaskParam();
                param.from = entry;
                File f = new File(UsbFileHelper.CACHEPATH);
                f.mkdirs();
                Log.d(TAG, "onItemClick: path=" + Environment.getExternalStorageDirectory().getAbsolutePath());
                int index = entry.getName().lastIndexOf(".") > 0
                        ? entry.getName().lastIndexOf(".")
                        : entry.getName().length();
                String prefix = entry.getName().substring(0, index);
                String ext = entry.getName().substring(index);
                // prefix must be at least 3 characters
                if (prefix.length() < 3) {
                    prefix += "pad";
                }
                param.to = File.createTempFile(prefix, ext, f);
                new CopyTask(MainActivity.this, currentFs).execute(param);
            }
        } catch (IOException e) {
            Log.e(TAG, "error staring to copy!", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXT_STORAGE_WRITE_PERM: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "Activity result is not ok");
            return;
        }

        if (requestCode == OPEN_STORAGE_PROVIDER_RESULT) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(uri);
                startActivity(i);
            }
        } else if (requestCode == COPY_STORAGE_PROVIDER_RESULT) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                CopyToUsbTaskParam params = new CopyToUsbTaskParam();
                params.from = uri;

                new CopyToUsbTask(this, currentFs).execute(params);
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_RESULT) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());

                CopyToUsbTaskParam params = new CopyToUsbTaskParam();
                params.from = uri;

                new CopyFolderToUsbTask(this, currentFs).execute(params);
            }
        }
    }

    private void move() {
        MoveClipboard cl = MoveClipboard.getInstance();
        UsbFile file = cl.getFile();
        try {
            file.moveTo(adapter.getCurrentDir());
            adapter.refresh();
        } catch (IOException e) {
            Log.e(TAG, "error moving!", e);
        }
        cl.setFile(null);
    }

    @Override
    public void onBackPressed() {
        try {
            UsbFile dir = dirs.pop();
            listView.setAdapter(adapter = new UsbFileListAdapter(this, dir));
        } catch (NoSuchElementException e) {
            super.onBackPressed();
        } catch (IOException e) {
            Log.e(TAG, "error initializing adapter!", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }
}
