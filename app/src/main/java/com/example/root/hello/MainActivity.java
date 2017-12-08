package com.example.root.hello;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tv;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashHandler.getInstance().init(this);
        Button btn = (Button) findViewById(R.id.button);
        iv = (ImageView) findViewById(R.id.iv);
        tv = (TextView) findViewById(R.id.tv);
        final ListView lv = (ListView) findViewById(R.id.lv);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MANAGE_DOCUMENTS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.MANAGE_DOCUMENTS)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.MANAGE_DOCUMENTS}, 1);
            Toast.makeText(MainActivity.this, "请求权限", Toast.LENGTH_LONG).show();
        }
        tv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(tv.getText()); //将内容放入粘贴管理器,在别的地方长按选择"粘贴"即可
                return true;
            }
        });
        adapter = new FileListAdapter(MainActivity.this, list);
        lv.setAdapter(adapter);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    Uri uri=Uri.parse("content://com.android.mtp.documents");
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    tv.append("cursor=" + cursor);
//                    savePicFromUri(uri);
                } catch (Exception e) {
                    tv.append("异常--：" + e.getMessage());
                }*/
                Intent intent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                }
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                startActivityForResult(intent, 0);
            }
        });
    }

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
           /* // 得到uri，后面就是将uri转化成file的过程。
            Uri uri = data.getData();
            int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
//            iv.setImageURI(uri);
            String s = uri.toString();
            int k = s.lastIndexOf("/");
            s = s.substring(0, k);
            StringBuffer sb = new StringBuffer();
            sb.append("当前图片uri:" + uri + "\n");
            sb.append(s + "\n");
            iv.setImageURI(uri);
           *//* String[] proj = {MediaStore.Images.Media.DATA};*//*
            Cursor cursor = getContentResolver().query(Uri.parse("content://com.android.mtp.documents"), null, null, null, null);//managedQuery(uri, proj, null, null, null);
            if (cursor != null) {
                int columnCount = cursor.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    sb.append(cursor.getColumnName(i) + "|");
                }
            } else {
                sb.append("cursor为空！");
            }
            tv.setText(sb.toString());*/
           /* ContentObserver observer = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    tv.append("新增了一张图片：" + uri + "\n");
                }
            };
            getContentResolver().registerContentObserver(uri2, true, observer);*/
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < grantResults.length; i++) {
//            PackageManager.PERMISSION_DENIED
            Toast.makeText(this, "状态：" + "permissions=" + permissions[0] + grantResults[i], Toast.LENGTH_LONG).show();
        }
    }

    List<FileItem> list = new ArrayList<>();
    DocumentFile curFile;
    FileListAdapter adapter;

    private void updateViews1(DocumentFile documentFile) {
        if (documentFile == null) {
            tv.append("文件为空！");
            return;
        }
        if (documentFile.isDirectory()) {
            tv.append("documentFile.isDirectory");
            list.clear();
            curFile = documentFile;
            DocumentFile[] documentFiles = documentFile.listFiles();
            for (DocumentFile file : documentFiles) {
                FileItem item = new FileItem();
                item.file = file;
                item.fileName = file.getName();
                item.lastModified = file.lastModified();
                item.type = file.getType();
                item.parentFile = file.getParentFile();
                item.uri = file.getUri();
                item.size = file.length();
                list.add(item);
                tv.append("uri=" + file.getUri());
                savePicFromUri(file.getUri());
            }
            adapter.setList(list);
            adapter.notifyDataSetChanged();
        } else {
            tv.append("!documentFile.isDirectory");
            updateViews1(documentFile = documentFile.getParentFile());
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
}


