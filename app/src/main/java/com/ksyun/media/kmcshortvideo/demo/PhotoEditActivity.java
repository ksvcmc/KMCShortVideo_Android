package com.ksyun.media.kmcshortvideo.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.kmcshortvideo.KMCAudioConfig;
import com.ksyun.media.kmcshortvideo.KMCRational;
import com.ksyun.media.kmcshortvideo.KMCShortVideo;
import com.ksyun.media.kmcshortvideo.KMCVideoConfig;
import com.meicam.sdk.NvsLiveWindow;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sujia on 2017/7/5.
 */

public class PhotoEditActivity extends Activity implements KMCShortVideo.PlaybackCallback {
    private static final String TAG = "VideoEditActivity";
    private KMCShortVideo mEditor;
    private NvsLiveWindow mLiveWindow;

    private ImageView mButtonBackOff;
    private ImageView mButtonPlay;
    private SeekBar mSeekBarPlay;
    private TextView mPlayedTime;
    private TextView mLeftTime;
    private Button mButtonChangeDuration;
    private Button mButtonChangeColor;
    private Button mButtonRotate;

    private LinearLayout mChangeDurationLayout;
    private LinearLayout mChangeColorLayout;
    private LinearLayout mRotateLayout;

    private SeekBar mChangeDuration;
    private SeekBar mChangeBright;
    private SeekBar mChangeContrast;
    private SeekBar mChangeSaturation;
    private ImageView mRotateAntiClockWise;
    private ImageView mRotateClockWise;

    private Button mButtonSave;
    private ButtonObserver mObserverButton;

    private boolean mPause = false;

    private boolean mSeekBarPlayLocked = true;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private long mVideoDuration = 0;

    private PhotoConfig mConfig;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEditor = new KMCShortVideo();
        mEditor.init(this);
        setContentView(R.layout.activity_photo_edit);

        initUI();
        initEditor();
        setControlListener();

        //get clips path
        Intent intent = getIntent();
        mConfig = intent
                .getParcelableExtra("config");

        // 移除所有片段
        mEditor.removeAllVideoClips();

        boolean selectSuccess = false;
        // 片段路径
        String mediaPath = mConfig.filePath;
        // 追加视频片段
        if(!mEditor.insertVideoClip(mediaPath, 0)) {
            Toast.makeText(this, "素材有错误"+ mediaPath , Toast.LENGTH_LONG).show();
        }
        else {
            selectSuccess = true;
        }

        if(selectSuccess) {
            resetUIInfo();

            if (mEditor == null)
                return;

            mEditor.changeTrimOutPoint(0, mConfig.trimOut);

            mEditor.changeBrightness(0, mConfig.bright);
            mEditor.changeContrast(0, mConfig.contrast);
            mEditor.changeSaturation(0, mConfig.saturation);

            mEditor.changeRotation(0, mConfig.rotate);

            mVideoDuration = mEditor.getDuration();
            long currentTime = mEditor.getCurrentPlaybackTime();
            mPlayedTime.setText(getFormatedTime(currentTime));
            mLeftTime.setText(getFormatedTime(mVideoDuration - currentTime));

            //定位预览所添加的视频片段
            seek();
        }

        switchToChangeDurationTab();
    }
    private void initUI() {
        mLiveWindow = (NvsLiveWindow) findViewById(R.id.photo_edit_liveWindow);
        mButtonBackOff = (ImageView) findViewById(R.id.photo_edit_button_back);
        
        mButtonChangeDuration = (Button) findViewById(R.id.photo_edit_button_change_duration);
        mButtonChangeColor = (Button) findViewById(R.id.photo_edit_button_change_color);
        mButtonRotate = (Button) findViewById(R.id.photo_edit_button_rotate);

        mChangeDurationLayout = (LinearLayout) findViewById(R.id.photo_edit_tab_duration);
        mChangeColorLayout = (LinearLayout) findViewById(R.id.photo_edit_tab_change_color);
        mRotateLayout = (LinearLayout) findViewById(R.id.photo_edit_tab_rotate);

        mChangeDuration = (SeekBar) findViewById(R.id.change_duration);
        mChangeDuration.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mChangeDuration.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mChangeBright = (SeekBar) findViewById(R.id.change_color_bright);
        mChangeBright.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mChangeBright.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mChangeContrast = (SeekBar) findViewById(R.id.change_color_contrast);
        mChangeContrast.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mChangeContrast.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mChangeSaturation = (SeekBar) findViewById(R.id.change_color_saturation);
        mChangeSaturation.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mChangeSaturation.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);
        mRotateAntiClockWise = (ImageView) findViewById(R.id.photo_edit_rotate_anti_clockwise);
        mRotateClockWise = (ImageView) findViewById(R.id.photo_edit_rotate_clockwise);

        mButtonPlay = (ImageView) findViewById(R.id.photo_edit_player_play);
        mPlayedTime = (TextView) findViewById(R.id.photo_edit_player_played_time);
        mLeftTime = (TextView) findViewById(R.id.photo_edit_player_left_time);

        mSeekBarPlay = (SeekBar) findViewById(R.id.photo_edit_seekBar_Play);
        mSeekBarPlay.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mSeekBarPlay.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mButtonSave = (Button) findViewById(R.id.photo_edit_button_save);
        mObserverButton = new ButtonObserver();

        mButtonChangeDuration.setEnabled(false);
        mButtonRotate.setEnabled(false);
        mButtonChangeColor.setEnabled(false);
        mSeekBarPlay.setEnabled(false);
        mButtonPlay.setEnabled(false);
    }

    private void setControlListener() {
        mButtonBackOff.setOnClickListener(mObserverButton);
        mButtonPlay.setOnClickListener(mObserverButton);
        mButtonChangeDuration.setOnClickListener(mObserverButton);
        mButtonChangeColor.setOnClickListener(mObserverButton);
        mButtonRotate.setOnClickListener(mObserverButton);
        mButtonSave.setOnClickListener(mObserverButton);

        mChangeDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChangeDuration(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mChangeBright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChangeBright(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mChangeContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChangContrast(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mChangeSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChangeSaturation(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mRotateAntiClockWise.setOnClickListener(mObserverButton);
        mRotateClockWise.setOnClickListener(mObserverButton);

        mSeekBarPlay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSeekBarPlayLocked = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSeekBarPlayLocked = false;
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if(!mSeekBarPlayLocked)
                    seek();
            }
        });
    }

    private void resetUIInfo() {
        mButtonChangeDuration.setEnabled(true);
        mButtonRotate.setEnabled(true);
        mButtonChangeColor.setEnabled(true);
        mSeekBarPlay.setEnabled(true);
        mButtonPlay.setEnabled(true);
    }

    private void initEditor() {
        KMCVideoConfig videoConfig = new KMCVideoConfig();
        videoConfig.width = 1280;
        videoConfig.height = 720;
        videoConfig.imagePAR = new KMCRational(1, 1);
        videoConfig.fps = new KMCRational(25, 1);
        KMCAudioConfig audioConfig = new KMCAudioConfig();
        audioConfig.sampleRate = 48000;
        audioConfig.channelCount = 2;
        mEditor.attachWindow(mLiveWindow, videoConfig, audioConfig);
        mEditor.setPlaybackCallback(this);//给Streaming context 设置播放回调接口
    }

    private void seek() {
        // 判定当前引擎状态是否是播放状态
        if(getCurrentEngineState() == KMCShortVideo.STATE_PLAYBACK) {
            mButtonPlay.setImageResource(R.drawable.play);
            stopTimer();
        }

        mEditor.seekTo(mSeekBarPlay.getProgress() * mVideoDuration/100);
    }

    // 获取当前引擎状态
    private int getCurrentEngineState() {
        return mEditor.getState();
    }

    private void startTimer(){
        if (mTimer == null) {
            mTimer = new Timer();
        }

        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }

        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    final long currentTime = mEditor.getCurrentPlaybackTime();
                    final long duration = mEditor.getDuration();
                    int progress = (int) (100 * currentTime / duration);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPlayedTime.setText(getFormatedTime(currentTime));
                            mLeftTime.setText(getFormatedTime(duration - currentTime));
                        }
                    });
                    mSeekBarPlay.setProgress(progress);
                }
            };
        }

        if(mTimer != null && mTimerTask != null )
            mTimer.schedule(mTimerTask, 0, 100);

    }

    private String getFormatedTime(long currentTime) {
        currentTime /= 1000*1000;
        long hours = currentTime / 3600;
        long minutes = (currentTime - hours * 3600) / 60;
        long seconds = currentTime - (hours * 3600 + minutes * 60);
        String time = String.format(Locale.getDefault(),
                "%02d:%02d", hours*60 + minutes, seconds);
        return time;
    }

    private void stopTimer(){
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    @Override
    protected void onPause() {
        stopTimer();
        //引擎停止
        if (mEditor != null) {
            mEditor.stop();
        }
        mButtonPlay.setImageResource(R.drawable.pause);
        mPause = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        mPause = false;
        super.onResume();
    }

    @Override
    public void onPlaybackPreloadingCompletion() {

    }

    @Override
    public void onPlaybackStopped() {
        stopTimer();
        mButtonPlay.setImageResource(R.drawable.play);
    }

    public void onPlaybackEOF() {
        mSeekBarPlay.setProgress(0);
        mEditor.seekTo(0);
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.photo_edit_button_back:
                    onBackoffClick();
                    break;
                case R.id.photo_edit_player_play:
                    onPlay();
                    break;
                case R.id.photo_edit_button_change_duration:
                    switchToChangeDurationTab();
                    break;
                case R.id.photo_edit_button_change_color:
                    switchToChangeColorTab();
                    break;
                case R.id.photo_edit_button_rotate:
                    switchToRotateTab();
                    break;
                case R.id.photo_edit_button_save:
                    onSave();
                case R.id.photo_edit_rotate_anti_clockwise:
                    onRotateAntiClockWise();
                    break;
                case R.id.photo_edit_rotate_clockwise:
                    onRotateClockWise();
                    break;
                default:
                    break;
            }
        }
    }

    private void switchToChangeDurationTab() {
        mChangeDurationLayout.setVisibility(View.VISIBLE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.GONE);

        mButtonChangeDuration.setSelected(true);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(false);

        mButtonChangeDuration.setTextColor(getResources().getColor(R.color.white));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
    }

    private void switchToChangeColorTab() {
        mChangeDurationLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.VISIBLE);
        mRotateLayout.setVisibility(View.GONE);

        mButtonChangeDuration.setSelected(false);
        mButtonChangeColor.setSelected(true);
        mButtonRotate.setSelected(false);

        mButtonChangeDuration.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.white));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
    }

    private void switchToRotateTab() {
        mChangeDurationLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.VISIBLE);

        mButtonChangeDuration.setSelected(false);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(true);

        mButtonChangeDuration.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.white));
    }

    private void onSave() {
        Intent data = new Intent();
        data.putExtra("config", mConfig);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void onChangeDuration(int progress) {
        if (mEditor != null) {
            mConfig.trimOut = progress * 1000000;
            mEditor.changeTrimOutPoint(0, mConfig.trimOut);
        }
    }

    private void onRotateClockWise() {
        if (mEditor != null) {
            mConfig.rotate -= 90;
            if (mConfig.rotate == -360) {
                mConfig.rotate = 0;
            }
            mEditor.changeRotation(0, mConfig.rotate);
        }
        onPlay();
    }

    private void onRotateAntiClockWise() {
        if (mEditor != null) {
            mConfig.rotate += 90;
            if (mConfig.rotate == 360) {
                mConfig.rotate = 0;
            }
            mEditor.changeRotation(0, mConfig.rotate);
        }
        onPlay();
    }

    private void onChangeSaturation(int progress) {
        if (mEditor != null) {
            mConfig.saturation = progress;
            mEditor.changeSaturation(0, mConfig.saturation);
        }
        onPlay();
    }

    private void onChangContrast(int progress) {
        if (mEditor != null) {
            mConfig.contrast = progress;
            mEditor.changeContrast(0, mConfig.contrast);
        }
        onPlay();
    }

    private void onChangeBright(int progress) {
        if (mEditor != null) {
            mConfig.bright = progress;
            mEditor.changeBrightness(0, mConfig.bright);
        }
        onPlay();
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(PhotoEditActivity.this).setCancelable(true)
                .setTitle("结束编辑?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        PhotoEditActivity.this.finish();
                    }
                }).show();
    }

    private void onPlay() {
        //判断当前引擎状态是否是播放状态
        if(getCurrentEngineState() != KMCShortVideo.STATE_PLAYBACK) {
            long startTime = mEditor.getCurrentPlaybackTime();
            mVideoDuration = mEditor.getDuration();
            // 播放视频
            mEditor.play(startTime, mVideoDuration);
            mButtonPlay.setImageResource(R.drawable.pause);
            startTimer();
        } else {
            stopTimer();
            mEditor.stop();
            mButtonPlay.setImageResource(R.drawable.play);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

}
