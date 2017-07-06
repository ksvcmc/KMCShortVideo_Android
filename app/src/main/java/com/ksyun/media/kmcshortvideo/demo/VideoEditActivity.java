package com.ksyun.media.kmcshortvideo.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.kmcshortvideo.KMCAudioConfig;
import com.ksyun.media.kmcshortvideo.KMCRational;
import com.ksyun.media.kmcshortvideo.KMCShortVideo;
import com.ksyun.media.kmcshortvideo.KMCVideoConfig;
import com.ksyun.media.kmcshortvideo.demo.videorangeslider.RangeSlider;
import com.meicam.sdk.NvsLiveWindow;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sujia on 2017/7/1.
 */

public class VideoEditActivity extends Activity implements KMCShortVideo.PlaybackCallback {
    private static final String TAG = "VideoEditActivity";
    private KMCShortVideo mEditor;
    private NvsLiveWindow mLiveWindow;

    private ImageView mButtonBackOff;
    private ImageView mButtonPlay;
    private SeekBar mSeekBarPlay;
    private TextView mPlayedTime;
    private TextView mLeftTime;
    private Button mButtonCut;
    private Button mButtonChangeSpeed;
    private Button mButtonChangeColor;
    private Button mButtonRotate;
    private Button mButtonScale;

    private RelativeLayout mCutVideoLayout;
    private LinearLayout mChangSpeedLayout;
    private LinearLayout mChangeColorLayout;
    private LinearLayout mRotateLayout;
    private TextView mScaleLayout;

    private ImageView mSpeedDown;
    private ImageView mSpeedUp;
    private TextView mCurrentSpeed;
    private SeekBar mChangeBright;
    private SeekBar mChangeContrast;
    private SeekBar mChangeSaturation;
    private ImageView mRotateAntiClockWise;
    private ImageView mRotateClockWise;

    private Button mButtonSave;
    private RecyclerView mRecyclerView;
    private RangeSlider mSlider;
    private TextView mLeftText;
    private TextView mRightText;
    private TextView mTipTxt;
    private ButtonObserver mObserverButton;

    private boolean mPause = false;

    private boolean mSeekBarPlayLocked = true;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private long mVideoDuration = 0;

    private VideoConfig mConfig;

    private int mRightIndex = 0;
    private int mLeftIndex = 0;

    private ScaleGestureDetector mScaleDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEditor = new KMCShortVideo();
        mEditor.init(this);
        setContentView(R.layout.activity_video_edit);

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

            mEditor.changeSpeed(mConfig.index, mConfig.speed);
            mEditor.changeBrightness(mConfig.index, mConfig.bright);
            mEditor.changeContrast(mConfig.index, mConfig.contrast);
            mEditor.changeSaturation(mConfig.index, mConfig.saturation);
            mEditor.changeRotation(mConfig.index, mConfig.rotate);
            mEditor.changeScale(mConfig.index, mConfig.scale);

            mVideoDuration = mEditor.getDuration();
            long currentTime = mEditor.getCurrentPlaybackTime();
            mPlayedTime.setText(getFormatedTime(currentTime));
            mLeftTime.setText(getFormatedTime(mVideoDuration - currentTime));

            String speed = String.format(Locale.getDefault(), "%.2f x",
                    mEditor.getSpeed(mConfig.index));
            mCurrentSpeed.setText(speed);

            mSeekBarPlay.setProgress((int) (mConfig.trimIn * 100 / mVideoDuration));
            mSlider.setTickCount(100);
            mLeftIndex = (int) (mConfig.trimIn * 100 / mVideoDuration);
            mRightIndex = (int) (mConfig.trimOut * 100 / mVideoDuration);
            if (mLeftIndex >= 0 && mRightIndex < 100 &&
                    mLeftIndex < mRightIndex) {
                mSlider.setRangeIndex(mLeftIndex, mRightIndex);
            }
            //定位预览所添加的视频片段
            seek();
        }

        switchToCutVideoTab();
    }
    private void initUI() {
        mLiveWindow = (NvsLiveWindow) findViewById(R.id.video_edit_liveWindow);
        mButtonBackOff = (ImageView) findViewById(R.id.video_edit_button_back);

        mButtonCut = (Button) findViewById(R.id.video_edit_button_cut);
        mButtonChangeSpeed = (Button) findViewById(R.id.video_edit_button_change_speed);
        mButtonChangeColor = (Button) findViewById(R.id.video_edit_button_change_color);
        mButtonRotate = (Button) findViewById(R.id.video_edit_button_rotate);
        mButtonScale = (Button) findViewById(R.id.video_edit_button_scale);

        mCutVideoLayout = (RelativeLayout) findViewById(R.id.video_edit_tab_cut_video);
        mChangSpeedLayout = (LinearLayout) findViewById(R.id.video_edit_tab_change_speed);
        mChangeColorLayout = (LinearLayout) findViewById(R.id.video_edit_tab_change_color);
        mRotateLayout = (LinearLayout) findViewById(R.id.video_edit_tab_rotate);
        mScaleLayout = (TextView) findViewById(R.id.video_edit_tab_scale);

        mSpeedDown = (ImageView) findViewById(R.id.video_edit_tab_speed_down);
        mSpeedUp = (ImageView) findViewById(R.id.video_edit_tab_speed_up);
        mCurrentSpeed = (TextView) findViewById(R.id.video_edit_tab_speed_text);

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

        mRotateAntiClockWise = (ImageView) findViewById(R.id.video_edit_rotate_anti_clockwise);
        mRotateClockWise = (ImageView) findViewById(R.id.video_edit_rotate_clockwise);

        mButtonPlay = (ImageView) findViewById(R.id.video_edit_player_play);
        mPlayedTime = (TextView) findViewById(R.id.video_edit_player_played_time);
        mLeftTime = (TextView) findViewById(R.id.video_edit_player_left_time);

        mSeekBarPlay = (SeekBar) findViewById(R.id.video_edit_seekBar_Play);
        mSeekBarPlay.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mSeekBarPlay.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mButtonSave = (Button) findViewById(R.id.video_edit_button_save);
        mObserverButton = new ButtonObserver();

        mButtonCut.setEnabled(false);
        mButtonChangeSpeed.setEnabled(false);
        mButtonChangeSpeed.setEnabled(false);
        mButtonRotate.setEnabled(false);
        mButtonScale.setEnabled(false);
        mSeekBarPlay.setEnabled(false);
        mButtonPlay.setEnabled(false);

        mTipTxt = (TextView) findViewById(R.id.video_edit_tip);
        mLeftText = (TextView) findViewById(R.id.video_edit_left_text);
        mRightText = (TextView) findViewById(R.id.video_edit_right_text);
        mSlider = (RangeSlider) findViewById(R.id.range_slider);
        mSlider.setRangeChangeListener(new RangeSlider.OnRangeChangeListener() {
            @Override
            public void onRangeChange(RangeSlider view, int leftPinIndex, int rightPinIndex,
                                      float leftX, float rightX) {
                //TODO:
                mLeftText.setX(leftX);
                mLeftText.setText(getFormatedTime(leftPinIndex * mVideoDuration / 100));
                mRightText.setX(rightX);
                mRightText.setText(getFormatedTime(leftPinIndex * mVideoDuration / 100));

                long duration = (rightPinIndex - leftPinIndex) *  mVideoDuration/ 100;
                mTipTxt.setText(getText(R.string.tip) + getFormatedTime(duration));

                if (mEditor == null) {
                   return;
                }

                //定位预览某一时间戳上的视频图像
                if(mLeftIndex != leftPinIndex ) {
                    mLeftIndex = leftPinIndex;
                    mConfig.trimIn = (mLeftIndex + 1)* mVideoDuration / 100;
                    mEditor.seekTo(mConfig.trimIn);
                    mSeekBarPlay.setProgress(mLeftIndex);
                }
                if(mRightIndex != rightPinIndex ) {
                    mRightIndex = rightPinIndex;
                    mConfig.trimOut = (mRightIndex + 1) * mVideoDuration / 100;
                    mEditor.seekTo(mConfig.trimOut);
                    mSeekBarPlay.setProgress(mRightIndex);
                }

            }
        });

        //init recyclerview
        mRecyclerView = (RecyclerView) findViewById(R.id.video_edit_thumb_list);
        //创建默认的线性LayoutManager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(0));
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mRecyclerView.setHasFixedSize(true);
    }

    private void setControlListener() {
        mButtonBackOff.setOnClickListener(mObserverButton);
        mButtonPlay.setOnClickListener(mObserverButton);
        mButtonCut.setOnClickListener(mObserverButton);
        mButtonChangeSpeed.setOnClickListener(mObserverButton);
        mButtonChangeColor.setOnClickListener(mObserverButton);
        mButtonRotate.setOnClickListener(mObserverButton);
        mButtonScale.setOnClickListener(mObserverButton);
        mButtonSave.setOnClickListener(mObserverButton);

        mSpeedDown.setOnClickListener(mObserverButton);
        mSpeedUp.setOnClickListener(mObserverButton);
        mChangeBright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChangeBright(progress);
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
                onChangContrast(progress);
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
                onChangeSaturation(progress);
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

        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mConfig.scale *= detector.getScaleFactor();

                // Don't let the object get too small or too large.
                mConfig.scale = Math.max(0.1f, Math.min(mConfig.scale, 5.0f));
                onZoom();

                return true;
            }
        });
        mScaleLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Let the ScaleGestureDetector inspect all events.
                mScaleDetector.onTouchEvent(event);
                return true;
            }
        });

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
        mButtonCut.setEnabled(true);
        mButtonChangeSpeed.setEnabled(true);
        mButtonChangeSpeed.setEnabled(true);
        mButtonRotate.setEnabled(true);
        mButtonScale.setEnabled(true);
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
                    final long duration = mVideoDuration;
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
                case R.id.video_edit_button_back:
                    onBackoffClick();
                    break;
                case R.id.video_edit_player_play:
                    onPlay();
                    break;
                case R.id.video_edit_button_cut:
                    switchToCutVideoTab();
                    break;
                case R.id.video_edit_button_change_speed:
                    switchToChangeSpeedTab();
                    break;
                case R.id.video_edit_button_change_color:
                    switchToChangeColorTab();
                    break;
                case R.id.video_edit_button_rotate:
                    switchToRotateTab();
                    break;
                case R.id.video_edit_button_scale:
                    switchToScaleTab();
                    break;
                case R.id.video_edit_button_save:
                    onSave();
                case R.id.video_edit_tab_speed_down:
                    onSpeedDown();
                    break;
                case R.id.video_edit_tab_speed_up:
                    onSpeedUp();
                    break;
                case R.id.video_edit_rotate_anti_clockwise:
                    onRotateAntiClockWise();
                    break;
                case R.id.video_edit_rotate_clockwise:
                    onRotateClockWise();
                    break;
                default:
                    break;
            }
        }
    }

    private void switchToCutVideoTab() {
        mCutVideoLayout.setVisibility(View.VISIBLE);
        mChangSpeedLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.GONE);
        mScaleLayout.setVisibility(View.GONE);

        mButtonCut.setSelected(true);
        mButtonChangeSpeed.setSelected(false);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(false);
        mButtonScale.setSelected(false);

        mButtonCut.setTextColor(getResources().getColor(R.color.white));
        mButtonChangeSpeed.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
        mButtonScale.setTextColor(getResources().getColor(R.color.grey));

        final int itemCount = 10;
        final long duration = mVideoDuration;
        int padding = getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int thumbWidth = getResources().getDimensionPixelOffset(R.dimen.range_thumb_width);
        final int itemWidth = (screenWidth - (2*(padding + thumbWidth))) / itemCount;

        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ImageView view = new ImageView(parent.getContext());
                view.setLayoutParams(new ViewGroup.LayoutParams(itemWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                return new CutVideoViewHolder(view);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                long time = position * duration / itemCount;
                Bitmap bitmap = mEditor.getThumbAtTime(mConfig.filePath, time);
                ((CutVideoViewHolder)holder).setImage(bitmap);
            }

            @Override
            public int getItemCount() {
                return itemCount;
            }
        });
    }

    private static class CutVideoViewHolder extends RecyclerView.ViewHolder {

        public CutVideoViewHolder(View itemView) {
            super(itemView);
        }

        private void setImage(Bitmap bitmap) {
            ImageView view = (ImageView)itemView;
            view.setImageBitmap(bitmap);
            view.setScaleType(ImageView.ScaleType.FIT_XY);
        }
    }

    private void switchToChangeSpeedTab() {
        mCutVideoLayout.setVisibility(View.GONE);
        mChangSpeedLayout.setVisibility(View.VISIBLE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.GONE);
        mScaleLayout.setVisibility(View.GONE);

        mButtonCut.setSelected(false);
        mButtonChangeSpeed.setSelected(true);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(false);
        mButtonScale.setSelected(false);

        mButtonCut.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeSpeed.setTextColor(getResources().getColor(R.color.white));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
        mButtonScale.setTextColor(getResources().getColor(R.color.grey));
    }

    private void switchToChangeColorTab() {
        mCutVideoLayout.setVisibility(View.GONE);
        mChangSpeedLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.VISIBLE);
        mRotateLayout.setVisibility(View.GONE);
        mScaleLayout.setVisibility(View.GONE);

        mButtonCut.setSelected(false);
        mButtonChangeSpeed.setSelected(false);
        mButtonChangeColor.setSelected(true);
        mButtonRotate.setSelected(false);
        mButtonScale.setSelected(false);

        mButtonCut.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeSpeed.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.white));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
        mButtonScale.setTextColor(getResources().getColor(R.color.grey));
    }

    private void switchToRotateTab() {
        mCutVideoLayout.setVisibility(View.GONE);
        mChangSpeedLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.VISIBLE);
        mScaleLayout.setVisibility(View.GONE);

        mButtonCut.setSelected(false);
        mButtonChangeSpeed.setSelected(false);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(true);
        mButtonScale.setSelected(false);

        mButtonCut.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeSpeed.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.white));
        mButtonScale.setTextColor(getResources().getColor(R.color.grey));
    }

    private void switchToScaleTab() {
        mCutVideoLayout.setVisibility(View.GONE);
        mChangSpeedLayout.setVisibility(View.GONE);
        mChangeColorLayout.setVisibility(View.GONE);
        mRotateLayout.setVisibility(View.GONE);
        mScaleLayout.setVisibility(View.VISIBLE);

        mButtonCut.setSelected(false);
        mButtonChangeSpeed.setSelected(false);
        mButtonChangeColor.setSelected(false);
        mButtonRotate.setSelected(false);
        mButtonScale.setSelected(true);

        mButtonCut.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeSpeed.setTextColor(getResources().getColor(R.color.grey));
        mButtonChangeColor.setTextColor(getResources().getColor(R.color.grey));
        mButtonRotate.setTextColor(getResources().getColor(R.color.grey));
        mButtonScale.setTextColor(getResources().getColor(R.color.white));
    }

    private void onZoom() {
        if (mEditor != null) {
            mEditor.changeScale(mConfig.index, mConfig.scale);
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void onSave() {
        Intent data = new Intent();
        data.putExtra("config", mConfig);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void onRotateClockWise() {
        if (mEditor != null) {
            mConfig.rotate -= 90;
            if (mConfig.rotate == -360) {
                mConfig.rotate = 0;
            }
            mEditor.changeRotation(mConfig.index, mConfig.rotate);
        }
        play();
    }

    private void onRotateAntiClockWise() {
        if (mEditor != null) {
            mConfig.rotate += 90;
            if (mConfig.rotate == 360) {
                mConfig.rotate = 0;
            }
            mEditor.changeRotation(mConfig.index, mConfig.rotate);
        }
        play();
    }

    private void onSpeedUp() {
        if (mEditor != null) {
            mConfig.speed = mEditor.getSpeed(mConfig.index) + 1;
            mEditor.changeSpeed(mConfig.index, mConfig.speed);

            String speed = String.format(Locale.getDefault(), "%.1f x",
                    mEditor.getSpeed(mConfig.index));
            mCurrentSpeed.setText(speed);
        }
        play();
    }

    private void onSpeedDown() {
        if (mEditor != null) {
            if (mConfig.speed > 1) {
                mConfig.speed = mEditor.getSpeed(mConfig.index) - 1;
            } else {
                mConfig.speed = mEditor.getSpeed(mConfig.index) - 0.1;
            }
            mEditor.changeSpeed(mConfig.index, mConfig.speed);

            String speed = String.format(Locale.getDefault(), "%.1f x",
                    mEditor.getSpeed(mConfig.index));
            mCurrentSpeed.setText(speed);
        }
        play();
    }

    private void onChangeSaturation(int progress) {
        if (mEditor != null) {
            mConfig.saturation = progress;
            mEditor.changeSaturation(mConfig.index, mConfig.saturation);
        }
        play();
    }

    private void onChangContrast(int progress) {
        if (mEditor != null) {
            mConfig.contrast = progress;
            mEditor.changeContrast(mConfig.index, mConfig.contrast);
        }
        play();
    }

    private void onChangeBright(int progress) {
        if (mEditor != null) {
            mConfig.bright = progress;
            mEditor.changeBrightness(mConfig.index, mConfig.bright);
        }
        play();
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(VideoEditActivity.this).setCancelable(true)
                .setTitle("结束编辑?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        VideoEditActivity.this.finish();
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

    private void play() {
        if(getCurrentEngineState() != KMCShortVideo.STATE_PLAYBACK) {
            stopTimer();

            long startTime = mEditor.getCurrentPlaybackTime();
            mVideoDuration = mEditor.getDuration();
            // 播放视频
            mEditor.play(startTime, mVideoDuration);
            mButtonPlay.setImageResource(R.drawable.pause);

            startTimer();
        }
    }

}
