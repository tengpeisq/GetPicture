package com.example.root.hello;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.example.root.hello.USB_PERMISSION";
    private TextView tv;
    private ImageView iv;
    private PendingIntent pendingIntent;
    private UsbManager manager;
    private static File dir;
    private static ScrollView sv;
    private static LinearLayout ll;
    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        sv = (ScrollView) findViewById(R.id.sv);
        ll = (LinearLayout) findViewById(R.id.ll);
        CrashHandler.getInstance().init(this);
        Button btn = (Button) findViewById(R.id.button);
        iv = (ImageView) findViewById(R.id.iv);
        tv = (TextView) findViewById(R.id.tv);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MANAGE_DOCUMENTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.MANAGE_DOCUMENTS)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.MANAGE_DOCUMENTS}, 1);
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
        }
        tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(tv.getText()); //将内容放入粘贴管理器,在别的地方长按选择"粘贴"即可
                return true;
            }
        });
        dir = new File(Environment.getExternalStorageDirectory(), "AAAAAA");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (deviceList != null) {
            for (String s : deviceList.keySet()) {
                device_add = deviceList.get(s);
            }
        }

        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        //otg插入 播出广播
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);//这里我用的碎片

        registerReceiver(receiver, new IntentFilter(ACTION_USB_PERMISSION));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                if (device_add == null) {
                    ToastUtil.showShort(MainActivity.this, "没有设备连接！");
                    return;
                }
                //申请读取usb设备的权限
                manager.requestPermission(device_add, pendingIntent);

            }
        });
    }

    /**
     * 收到权限回调
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            String productName = device.getProductName();
                            tv.append("当前设备型号：" + productName);
                        }
                    } catch (Exception e) {
                        tv.append("e=" + e.getMessage());
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            ToastUtil.showShort(MainActivity.this, "允许USB访问设备！");
                            UsbDeviceConnection usbDeviceConnection = manager.openDevice(device);
                            if (usbDeviceConnection == null) {
                                ToastUtil.showShort(MainActivity.this, "打开设备失败！");
                            } else {
                                MtpDevice mtpDevice = new MtpDevice(device);
                                boolean open = mtpDevice.open(usbDeviceConnection);
                                new SlideShowImageTask().execute(mtpDevice);
                            }
                        }
                    } else {
                        ToastUtil.showShort(MainActivity.this, "权限拒绝，请重试！");
                    }
                }
            }
        }
    };
    private UsbDevice device_add;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED://接收到存储设备插入广播
                    device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_add != null) {
                        //TShow("接收到存储设备插入广播");
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED://接收到存储设备拔出广播
                    UsbDevice device_remove = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_remove != null) {
                        //TShow("接收到存储设备拔出广播");
                    }
                    break;
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 是否选择，没选择就不会继续
        if (resultCode == Activity.RESULT_OK) {
            /*tv.append("当前uri="+uri.toString());
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
            DocumentFile documentFile = DocumentFile.fromTreeUri(this, *//*Uri.parse("content://com.android.mtp.documents/document")*//*uri);
            updateViews1(documentFile);*/
            Uri uri = data.getData();
            ContentResolver resolver = getContentResolver();
            final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                resolver.takePersistableUriPermission(uri, takeFlags);
            }
            Cursor cursor = resolver.query(uri, null, null, null, null);
            savePicFromUri(uri);
            StringBuffer sb = new StringBuffer();
            sb.append("当前图片uri:" + uri + "\n");
            sb.append("cursor=:" + cursor);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String document_id = cursor.getString(cursor.getColumnIndex("document_id"));
                    sb.append("document_id=" + document_id + "|");
                }
                sb.append("\n");
                int columnCount = cursor.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    sb.append(cursor.getColumnName(i) + "|");
                }
            }
            iv.setImageURI(uri);
            tv.setText(sb.toString());
        }
    }


    private void savePicFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), SystemClock.uptimeMillis() + "哈哈.jpg"));
            byte[] buf = new byte[1024];
            while (is.read(buf) != -1) {
                fos.write(buf, 0, buf.length);
            }
            is.close();
            fos.close();
            Toast.makeText(this, "保存成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private static class SlideShowImageTask extends AsyncTask<MtpDevice, Bitmap, Integer> {

        public SlideShowImageTask() {
        }

        @Override
        protected Integer doInBackground(MtpDevice... args) {
            MtpDevice mtpDevice = args[0];
            /*
             * acquire storage IDs in the MTP device
             */
            int[] storageIds = mtpDevice.getStorageIds();
            if (storageIds == null) {
                return null;
            }

            /*
             * scan each storage
             */
            for (int storageId : storageIds) {
                scanObjectsInStorage(mtpDevice, storageId, 0, 0);
            }

            /* close MTP device */
            mtpDevice.close();
            return null;
        }

        private void scanObjectsInStorage(MtpDevice mtpDevice, int storageId, int format, int parent) {
            int[] objectHandles = mtpDevice.getObjectHandles(storageId, format, parent);
            if (objectHandles == null) {
                return;
            }
            for (int objectHandle : objectHandles) {
                /*
                 *　It's an abnormal case that you can't acquire MtpObjectInfo from MTP device
                 */
                MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(objectHandle);
                if (mtpObjectInfo == null) {
                    continue;
                }

                /*
                 * Skip the object if parent doesn't match
                 */
                int parentOfObject = mtpObjectInfo.getParent();
                if (parentOfObject != parent) {
                    continue;
                }

                int associationType = mtpObjectInfo.getAssociationType();

                if (associationType == MtpConstants.ASSOCIATION_TYPE_GENERIC_FOLDER) {
                    /* Scan the child folder */
                    scanObjectsInStorage(mtpDevice, storageId, format, objectHandle);
                } else if (mtpObjectInfo.getFormat() == MtpConstants.FORMAT_EXIF_JPEG &&
                        mtpObjectInfo.getProtectionStatus() != MtpConstants.PROTECTION_STATUS_NON_TRANSFERABLE_DATA) {
                    /*
                     *  get bitmap data from the object
                     */
                    byte[] rawObject = mtpDevice.getObject(objectHandle, mtpObjectInfo.getCompressedSize());
                    try {
                        File[] files = dir.listFiles();
                        if (files == null || files.length == 0) {
                            //第一次保存文件
                            FileOutputStream fos = new FileOutputStream(new File(dir, mtpObjectInfo.getName()));
                            fos.write(rawObject, 0, rawObject.length);
                            fos.close();
                        } else {
                            for (File file : files) {
                                if (mtpObjectInfo.getName().equals(file.getName())) {
                                } else {
                                    //如果没有保存 就保存
                                    FileOutputStream fos = new FileOutputStream(new File(dir, mtpObjectInfo.getName()));
                                    fos.write(rawObject, 0, rawObject.length);
                                    fos.close();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Bitmap bitmap = null;
                    if (rawObject != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        int scaleW = (mtpObjectInfo.getImagePixWidth() - 1) / 600 + 1;
                        int scaleH = (mtpObjectInfo.getImagePixHeight() - 1) / 600 + 1;
                        int scale = Math.max(scaleW, scaleH);
                        if (scale > 0) {
                            options.inSampleSize = scale;
                            bitmap = BitmapFactory.decodeByteArray(rawObject, 0, rawObject.length, options);
                        }
                    }
                    if (bitmap != null) {
                        /* show the bitmap in UI thread */
                        ll.setTag(mtpObjectInfo);
                        publishProgress(bitmap);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            MtpObjectInfo mtpObjectInfo = (MtpObjectInfo) ll.getTag();
            View view = View.inflate(activity, R.layout.item, null);
            Bitmap bitmap = values[0];
            ImageView iv = (ImageView) view.findViewById(R.id.iv);
            TextView textView = (TextView) view.findViewById(R.id.text);
            if (mtpObjectInfo != null) {
                textView.setText(mtpObjectInfo.getName());
            }
            iv.setImageBitmap(bitmap);
            ll.addView(view);
            super.onProgressUpdate(values);
        }
    }
}


