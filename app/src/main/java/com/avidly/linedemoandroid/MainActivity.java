package com.avidly.linedemoandroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

public class MainActivity extends AppCompatActivity {

    private TextView txt;
    private int REQUEST_CODE = 7777;
    private String CHANNEL_ID = "1508354787";
    private static LineApiClient lineApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LineApiClientBuilder apiClientBuilder = new LineApiClientBuilder(this, CHANNEL_ID);
        lineApiClient = apiClientBuilder.build();

        bindView();
    }

    private void bindView() {

        txt = (TextView)findViewById(R.id.txt_show);

        findViewById(R.id.btn_lineLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = LineLoginApi.getLoginIntent(MainActivity.this, CHANNEL_ID);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
        findViewById(R.id.btn_lineLogin2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = LineLoginApi.getLoginIntentWithoutLineAppAuth(MainActivity.this, CHANNEL_ID);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
        findViewById(R.id.btn_lineLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LogoutTask().execute();
                finish();
            }
        });
        findViewById(R.id.btn_lineShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPkgInstalled("jp.naver.line.android")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                showInfo("Please grant the permission this time");
                            }
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }else {
                            lineShare();
                        }
                    }else {
                        lineShare();
                    }
                }else {
                    Uri uri = Uri.parse("market://details?id=jp.naver.line.android"); //id为包名
                    Intent it = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(it);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE) {
            return;
        }
        LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);
        switch (result.getResponseCode()) {
            case SUCCESS:
                String accessToken = result.getLineCredential().getAccessToken().getAccessToken();
                showInfo("accessToken: "+accessToken);
                LineProfile profile = result.getLineProfile();
                String name = profile.getDisplayName();
                String id = profile.getUserId();
                String status = profile.getStatusMessage();
                Uri picUrl = profile.getPictureUrl();
                showInfo("LineLogin Success!\nid: "+id+"\nname: "+name+"\nstatus: "+status+"\npicUri: "+picUrl);
                break;
            case CANCEL:
                showInfo("LineLogin Cancel!");
                break;
            case SERVER_ERROR:
                showInfo("LineLogin SERVER_ERROR!");
                break;
            case NETWORK_ERROR:
                showInfo("LineLogin NETWORK_ERROR!");
                break;
            case INTERNAL_ERROR:
                showInfo("LineLogin INTERNAL_ERROR!");
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            int grantResult = grantResults[0];
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                lineShare();
            }else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                    showPermissionSettingsDialog();
                }
            }
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this).setCancelable(false).setTitle("温馨提示").
                setMessage("由於您已拒绝授权，无法使用此功能，如需分享，請前往设置-权限-存储设备打开权限。")
                .setNegativeButton("下次吧", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        jump2PermissionSettings();
                     }
                }).show();
    }

    /**
     * 跳转到应用程序信息详情页面
     */
    private void jump2PermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    /**
     * 分享功能（单独分享文字，单独分享图片，同时分享文字和图片）
     *  LINE
     *  ComponentName(String pkg, String cls)
     *  line的包名，line的接收资料的类名--》 </intent-filter> MainFist里面
     */
    private void lineShare() {
        ComponentName cn = new ComponentName("jp.naver.line.android", "jp.naver.line.android.activity.selectchat.SelectChatActivity");
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mmmm);
        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
        shareIntent.setType("image/jpeg"); //图片分享
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("text/plain"); // 纯文本
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享的标题");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "分享的内容");
        shareIntent.setComponent(cn);//跳到指定APP的Activity
        startActivity(Intent.createChooser(shareIntent,""));
    }

    private boolean isPkgInstalled(String pkgName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if (packageInfo == null) {
            return false;
        } else {
            return true;
        }
    }

    private void showInfo(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        Log.i("LineLogin", info);
        txt.setText(info);
    }

    public class GetProfileTask extends AsyncTask<Void, Void, LineApiResponse<LineProfile>> {

        @Override
        protected LineApiResponse<LineProfile> doInBackground(Void... voids) {
            return lineApiClient.getProfile();
        }
    }

    public class LogoutTask extends AsyncTask<Void, Void, LineApiResponse> {

        @Override
        protected LineApiResponse doInBackground(Void... voids) {
            return lineApiClient.logout();
        }
    }
}
