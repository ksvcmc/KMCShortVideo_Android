package com.ksyun.media.kmcshortvideo.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.ksyun.media.kmcshortvideo.KMCAuthManager;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaOptions;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.activities.MediaPickerActivity;

/**
 * Created by sujia on 2017/6/15.
 */

public class ClipImportActivity extends Activity {
    private static final String TAG = "ClipImportActivity";
    private static final int REQUEST_MEDIA = 100;
    ImageView mSelectBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_import);
        mSelectBtn = (ImageView) findViewById(R.id.select_clip);
        mSelectBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaOptions.Builder builder = new MediaOptions.Builder();
                MediaOptions options = builder.canSelectBothPhotoVideo().
                        canSelectMultiPhoto(true).canSelectMultiVideo(true).build();
                MediaPickerActivity.open(ClipImportActivity.this, REQUEST_MEDIA, options);
            }
        });

        mSelectBtn.setEnabled(false);
        //鉴权成功后才能使用SDK
        doAuth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, MediaEditActivity.class);
                intent.putParcelableArrayListExtra(MediaPickerActivity.EXTRA_MEDIA_SELECTED,
                        data.getParcelableArrayListExtra(MediaPickerActivity.EXTRA_MEDIA_SELECTED));
                startActivity(intent);
            }
        }
    }

    private void doAuth() {
        String token = "cd1b62c91553e61ea729b78a3a656f29";
        KMCAuthManager.getInstance().authorize(getApplicationContext(),
                token, mCheckAuthResultListener);
    }

    private KMCAuthManager.AuthResultListener mCheckAuthResultListener = new KMCAuthManager
            .AuthResultListener() {
        @Override
        public void onSuccess() {
            mSelectBtn.setEnabled(true);

            KMCAuthManager.getInstance().removeAuthResultListener(mCheckAuthResultListener);
            makeToast("鉴权成功，可以使用魔方贴纸功能");
        }

        @Override
        public void onFailure(int errCode) {
            KMCAuthManager.getInstance().removeAuthResultListener(mCheckAuthResultListener);
            makeToast("鉴权失败! 错误码: " + errCode);
        }
    };


    private void makeToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(ClipImportActivity.this, str, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }
}
