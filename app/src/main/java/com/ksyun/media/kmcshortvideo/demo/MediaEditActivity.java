package com.ksyun.media.kmcshortvideo.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.kmcshortvideo.Constants;
import com.ksyun.media.kmcshortvideo.KMCAudioConfig;
import com.ksyun.media.kmcshortvideo.KMCMaterial;
import com.ksyun.media.kmcshortvideo.KMCMaterialManager;
import com.ksyun.media.kmcshortvideo.KMCRational;
import com.ksyun.media.kmcshortvideo.KMCShortVideo;
import com.ksyun.media.kmcshortvideo.KMCVideoConfig;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaItem;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaOptions;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.activities.MediaPickerActivity;
import com.meicam.sdk.NvsLiveWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.ksyun.media.kmcshortvideo.demo.mediapicker.activities.MediaPickerActivity.EXTRA_MEDIA_SELECTED;

/**
 * Created by sujia on 2017/6/15.
 */

public class MediaEditActivity extends Activity implements KMCShortVideo.PlaybackCallback {
    private static final String TAG = "MediaEditActivity";
    private KMCShortVideo mEditor;
    private NvsLiveWindow mLiveWindow;

    private ImageView mButtonBackOff;
    private ImageView mButtonEditTitle;
    private EditText mEditText;
    private ImageView mButtonPlay;
    private SeekBar mSeekBarPlay;
    private TextView mPlayedTime;
    private TextView mLeftTime;
    private Button mButtonAddTheme;
    private Button mButtonAddMusic;
    private Button mButtonAddFilter;
    private Button mButtonEditVideo;
    private Button mButtonSave;
    private ButtonObserver mObserverButton;

    private KMCMaterial mMaterial = null;
    private RecyclerView mRecyclerView = null;
    private MaterialListViewAdapter mMaterialListViewAdapter = null;
    private MediaListViewAdapter mMediaListViewAdapter = null;

    private List<MaterialInfoItem> mThemeMaterialList = null;
    private List<MaterialInfoItem> mMusicMaterialList = null;
    private List<MaterialInfoItem> mFilterMaterialList = null;
    private List<MaterialInfoItem> mCurrentMaterialList = null;

    private static int mThemeSelectIndex = -1;
    private static int mMusicSelectIndex = -1;
    private static int mFilterSelectIndex = -1;
    private static int mCurrentMaterialIndex = -1;

    private static int mCurrentTabIndex = 0;

    private boolean mPause = false;
    private boolean mIsFirstFetchMaterialList = true;
    private Bitmap mNullBitmap = null;

    private Handler mMainHandler;
    private final static int MSG_LOAD_THUMB = 0;
    private final static int MSG_DOWNLOAD_SUCCESS = 1;
    private final static int MSG_START_DOWNLOAD = 2;
    private final static int MSG_GET_LIST_SIZE = 3;

    private boolean mSeekBarPlayLocked = true;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private ArrayList<MediaItem> mMediaItemList = null;

    private static final int REQUEST_MEDIA = 100;
    private static final int SINGLE_VIDEO_EDIT = 102;
    private static final int SINGLE_PHOTO_EDIT = 103;
    private int mMediaItemInsertIndex;

    //auth
    private boolean authorized = false;
    private int mCurrentPosition;
    private String mCompilePath;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainHandler = new Handler();

        initActivity();
    }

    private void initActivity() {
        mEditor = new KMCShortVideo();
        mEditor.init(this);
        setContentView(R.layout.activity_media_edit);

        //要生成视频的路径
        File compileDir = new File(Environment.getExternalStorageDirectory(), "KMCSV" + File.separator + "Compile");
        if (!compileDir.exists() && !compileDir.mkdirs()) {
            Log.d(TAG, "Failed to make Compile directory");
            return;
        }

        File file = new File(compileDir, "video.mp4");
        mCompilePath = file.getAbsolutePath();

        initUI();
        initEditor();
        setControlListener();

        //get clips path
        Intent intent = getIntent();
        mMediaItemList = intent
                .getParcelableArrayListExtra(EXTRA_MEDIA_SELECTED);
        if (mMediaItemList != null) {
            // 移除所有片段
            mEditor.removeAllVideoClips();

            boolean selectSuccess = false;
            for (MediaItem media: mMediaItemList) {
                // 片段路径
                String mediaPath = media.getPathOrigin(this);
                // 追加视频片段
                if(!mEditor.appendVideoClip(mediaPath)) {
                    Toast.makeText(this, "素材有错误"+ mediaPath , Toast.LENGTH_LONG).show();
                }
                else {
                    selectSuccess = true;
                }
            }

            if(selectSuccess) {
                resetUIInfo();
                long currentTime = mEditor.getCurrentPlaybackTime();
                long duration = mEditor.getDuration();
                mPlayedTime.setText(getFormatedTime(currentTime));
                mLeftTime.setText(getFormatedTime(duration - currentTime));

                //定位预览所添加的视频片段
                seek();
            }
        }

        onAddTheme();
    }

    private void makeToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MediaEditActivity.this, str, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    private void initUI() {
        mLiveWindow = (NvsLiveWindow) findViewById(R.id.liveWindow);
        mButtonBackOff = (ImageView) findViewById(R.id.button_back);
        mButtonEditTitle = (ImageView) findViewById(R.id.button_edit_title);
        mEditText = (EditText) findViewById(R.id.edit_title_txt);
        mButtonAddTheme = (Button) findViewById(R.id.button_add_theme);
        mButtonAddMusic = (Button) findViewById(R.id.button_add_music);
        mButtonAddFilter = (Button) findViewById(R.id.button_add_filter);
        mButtonEditVideo = (Button) findViewById(R.id.button_edit_video);
        mButtonPlay = (ImageView) findViewById(R.id.player_play);
        mPlayedTime = (TextView) findViewById(R.id.player_played_time);
        mLeftTime = (TextView) findViewById(R.id.player_left_time);

        mSeekBarPlay = (SeekBar) findViewById(R.id.seekBar_Play);
        mSeekBarPlay.getProgressDrawable().setColorFilter(getResources().getColor(R.color.purple),
                PorterDuff.Mode.SRC_ATOP);
        mSeekBarPlay.getThumb().setColorFilter(getResources().getColor(R.color.white),
                PorterDuff.Mode.SRC_ATOP);

        mButtonSave = (Button) findViewById(R.id.button_save);
        mObserverButton = new ButtonObserver();

        mEditText.setVisibility(View.GONE);
        mButtonAddTheme.setEnabled(false);
        mButtonAddMusic.setEnabled(false);
        mButtonAddMusic.setEnabled(false);
        mButtonEditVideo.setEnabled(false);
        mSeekBarPlay.setEnabled(false);
        mButtonPlay.setEnabled(false);

        //init recyclerview
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        //创建默认的线性LayoutManager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(5));
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mRecyclerView.setHasFixedSize(true);
    }

    private void setControlListener() {
        mButtonBackOff.setOnClickListener(mObserverButton);
        mButtonEditTitle.setOnClickListener(mObserverButton);
        mButtonPlay.setOnClickListener(mObserverButton);
        mButtonAddTheme.setOnClickListener(mObserverButton);
        mButtonAddMusic.setOnClickListener(mObserverButton);
        mButtonAddFilter.setOnClickListener(mObserverButton);
        mButtonEditVideo.setOnClickListener(mObserverButton);
        mButtonSave.setOnClickListener(mObserverButton);

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
        mButtonAddTheme.setEnabled(true);
        mButtonAddMusic.setEnabled(true);
        mButtonAddMusic.setEnabled(true);
        mButtonEditVideo.setEnabled(true);
        mSeekBarPlay.setEnabled(true);
        mButtonPlay.setEnabled(true);
        mSeekBarPlay.setProgress(0);
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

        mEditor.seekTo(mSeekBarPlay.getProgress() * mEditor.getDuration()/100);
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
                    if(duration != 0) {
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
        String time = String.format("%02d:%02d", hours*60 + minutes, seconds);
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
    protected void onDestroy() {
        if (mEditor != null) {
            mEditor.release();
        }

        super.onDestroy();
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
                case R.id.button_back:
                    onBackoffClick();
                    break;
                case R.id.button_edit_title:
                    onEditTitle();
                    break;
                case R.id.player_play:
                    onPlay();
                    break;
                case R.id.button_add_theme:
                    onAddTheme();
                    break;
                case R.id.button_add_music:
                    onAddMusic();
                    break;
                case R.id.button_add_filter:
                    onAddFilter();
                    break;
                case R.id.button_edit_video:
                    onEditVideo();
                    break;
                case R.id.button_save:
                    onSave();
                default:
                    break;
            }
        }
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(MediaEditActivity.this).setCancelable(true)
                .setTitle("结束编辑?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        MediaEditActivity.this.finish();
                    }
                }).show();
    }

    private void onEditTitle() {
        mEditText.setVisibility(View.VISIBLE);
        mEditText.getBackground().setAlpha(50*255/100);
        mEditText.requestFocus();

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (mEditor != null) {
                        mEditor.setThemeTitleCaptionText(mEditText.getText().toString());
                    }
                    mEditText.setVisibility(View.GONE);
                    return true;
                }

                return false;
            }

        });
    }

    private void onPlay() {
        //判断当前引擎状态是否是播放状态
        if(getCurrentEngineState() != KMCShortVideo.STATE_PLAYBACK) {
            long startTime = mEditor.getCurrentPlaybackTime();
            // 播放视频
            mEditor.play(startTime, mEditor.getDuration());
            mButtonPlay.setImageResource(R.drawable.pause);
            startTimer();
        } else {
            stopTimer();
            mEditor.stop();
            mButtonPlay.setImageResource(R.drawable.play);
        }
    }

    private void onAddTheme() {
        mButtonAddTheme.setSelected(true);
        mButtonAddMusic.setSelected(false);
        mButtonAddFilter.setSelected(false);
        mButtonEditVideo.setSelected(false);
        mButtonAddTheme.setTextColor(getResources().getColor(R.color.white));
        mButtonAddMusic.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddFilter.setTextColor(getResources().getColor(R.color.grey));
        mButtonEditVideo.setTextColor(getResources().getColor(R.color.grey));

        updateTabView(0);
    }

    private void onAddMusic() {
        mButtonAddTheme.setSelected(false);
        mButtonAddMusic.setSelected(true);
        mButtonAddFilter.setSelected(false);
        mButtonEditVideo.setSelected(false);
        mButtonAddTheme.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddMusic.setTextColor(getResources().getColor(R.color.white));
        mButtonAddFilter.setTextColor(getResources().getColor(R.color.grey));
        mButtonEditVideo.setTextColor(getResources().getColor(R.color.grey));

        updateTabView(1);
    }

    private void onAddFilter() {
        mButtonAddTheme.setSelected(false);
        mButtonAddMusic.setSelected(false);
        mButtonAddFilter.setSelected(true);
        mButtonEditVideo.setSelected(false);
        mButtonAddTheme.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddMusic.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddFilter.setTextColor(getResources().getColor(R.color.white));
        mButtonEditVideo.setTextColor(getResources().getColor(R.color.grey));

        updateTabView(2);
    }

    private void onEditVideo() {
        mButtonAddTheme.setSelected(false);
        mButtonAddMusic.setSelected(false);
        mButtonAddFilter.setSelected(false);
        mButtonEditVideo.setSelected(true);
        mButtonAddTheme.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddMusic.setTextColor(getResources().getColor(R.color.grey));
        mButtonAddFilter.setTextColor(getResources().getColor(R.color.grey));
        mButtonEditVideo.setTextColor(getResources().getColor(R.color.white));

        updateTabView(3);
    }

    private void onSave() {
        new AlertDialog.Builder(MediaEditActivity.this).setCancelable(true)
                .setTitle("开始合成?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        onCompile();
                    }
                }).show();
    }

    private void onCompile() {
        if (mEditor != null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setTitle("合成视频");
            mProgressDialog.setMessage("进行中，请等待...");
            mProgressDialog.setMax(100);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);

            mEditor.setCompileCallback(new KMCShortVideo.CompileCallback() {
                @Override
                public void onCompileProgress(int progress) {
                    mProgressDialog.setProgress(progress);
                }

                @Override
                public void onCompileFinished() {
                    mProgressDialog.dismiss();
                    makeToast("合成文件: " + mCompilePath);
                }

                @Override
                public void onCompileFailed() {
                    mProgressDialog.dismiss();
                    makeToast("合成失败");
                }
            });

            /**
             * compile用于将合成。
             * 生成素材的分辨率的高。可以是360，480，720，1080。
             它和attachWindow时设置的的分辨率共同决定了生成素材的分辨率。
             假如此处高是480，低于设置的高，那么生成的素材的分辨率的高就是480，宽是480乘以设置的分辨率的横纵比。
             如果此处的高大于设置分辨率的高，则生成视频的分辨率就是设置的分辨率。
             */
            if (mEditor.compile(0, mEditor.getDuration(), mCompilePath, KMCShortVideo.COMPILE_VIDEO_RESOLUTION_GRADE_720,
                    KMCShortVideo.COMPILE_BITRATE_GRADE_HIGH, KMCShortVideo.COMPILE_FLAG_ENABLE_HARDWARE_ENCODER) ){
                mProgressDialog.show();
            }
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


    /**
     * 获取贴纸列表, 从AR服务器获取到当前热门/或者符合主播属性的贴纸列表
     */
    protected void startGetMaterialList() {
        if (mMusicMaterialList != null && mThemeMaterialList != null &&
                mMusicMaterialList.size() > 1 && mThemeMaterialList.size() > 1) {
            return;
        }
        mThemeMaterialList = new ArrayList<>();
        mMusicMaterialList = new ArrayList<>();
        mFilterMaterialList = new ArrayList<>();

        if (mNullBitmap == null) {
            mNullBitmap = getNullEffectBitmap();
        }
        mThemeMaterialList.add(getNullMaterial(Constants.MATERIAL_TYPE_THEME));
        mMusicMaterialList.add(getNullMaterial(Constants.MATERIAL_TYPE_MUSIC));
        mFilterMaterialList.add(getNullMaterial(Constants.MATERIAL_TYPE_FILTER));

        fetchMaterialList();
    }

    private MaterialInfoItem getNullMaterial(int type) {
        if (mNullBitmap == null) {
            mNullBitmap = getNullEffectBitmap();
        }
        MaterialInfoItem nullItem = new MaterialInfoItem(new KMCMaterial(), mNullBitmap);
        nullItem.setHasDownload(true);
        nullItem.material.thumbnailName = "无";
        nullItem.material.type = type;
        return nullItem;
    }

    private Bitmap getNullEffectBitmap() {
        mNullBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.null_effect);
        return mNullBitmap;
    }

    private void fetchMaterialList() {
        fetchMaterial(Constants.MATERIAL_TYPE_THEME);
        fetchMaterial(Constants.MATERIAL_TYPE_MUSIC);
        fetchMaterial(Constants.MATERIAL_TYPE_FILTER);
    }

    private void fetchMaterial(int materialType) {
        // 从AR服务器获取贴纸列表, 并保存其信息
        KMCMaterialManager.getInstance().fetchMaterials(getApplicationContext(), materialType,
                new KMCMaterialManager.FetchMaterialListener() {
                    @Override
                    public void onSuccess(List<KMCMaterial> list) {
                        List<KMCMaterial> materialList = list;
                        for (int i = 0; i < materialList.size(); i++) {
                            KMCMaterial material = materialList.get(i);
                            MaterialInfoItem materialInfoItem = new MaterialInfoItem(material, null);

                            if (KMCMaterialManager.getInstance().isMaterialDownloaded(material)) {
                                materialInfoItem.setHasDownload(true);
                            } else {
                                materialInfoItem.setHasDownload(false);
                            }

                            if (material.type == Constants.MATERIAL_TYPE_THEME) {
                                mThemeMaterialList.add(materialInfoItem);
                            } else if (material.type == Constants.MATERIAL_TYPE_MUSIC){
                                mMusicMaterialList.add(materialInfoItem);
                            } else if (material.type == Constants.MATERIAL_TYPE_FILTER) {
                                mFilterMaterialList.add(materialInfoItem);
                            }

                            Message msg = mHandler.obtainMessage(MSG_GET_LIST_SIZE);
                            mHandler.sendMessage(msg);
                        }

                        loadThumbnail();
                    }

                    @Override
                    public void onFailure(int errorCode, String msg) {
                        makeToast("fetch material list failed, msg: " + msg);
                    }
                });
    }

    private void loadThumbnail() {
        for (int i = 1; i < mThemeMaterialList.size(); i++) {
            MaterialInfoItem materialInfoItem = mThemeMaterialList.get(i);

            String thumbnailurl = materialInfoItem.material.thumbnailURL;
            Bitmap thumbnail = null;
            try {
                thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurl);
            } catch (Exception e) {
                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                makeToast("get material thumbnail failed");
            }
            materialInfoItem.thumbnail = thumbnail;
            mThemeMaterialList.set(i, materialInfoItem);

            Message msg = mHandler.obtainMessage(MSG_LOAD_THUMB);
            msg.arg2 = i+1;
            mHandler.sendMessage(msg);
        }

        for (int i = 1; i < mMusicMaterialList.size(); i++) {
            MaterialInfoItem materialInfoItem = mMusicMaterialList.get(i);

            String thumbnailurl = materialInfoItem.material.thumbnailURL;
            Bitmap thumbnail = null;
            try {
                thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurl);
            } catch (Exception e) {
                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                makeToast("get material thumbnail failed");
            }
            materialInfoItem.thumbnail = thumbnail;
            mMusicMaterialList.set(i, materialInfoItem);

            Message msg = mHandler.obtainMessage(MSG_LOAD_THUMB);
            msg.arg2 = i+1;
            mHandler.sendMessage(msg);
        }

        for (int i = 1; i < mFilterMaterialList.size(); i++) {
            MaterialInfoItem materialInfoItem = mFilterMaterialList.get(i);

            String thumbnailurl = materialInfoItem.material.thumbnailURL;
            Bitmap thumbnail = null;
            try {
                thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurl);
            } catch (Exception e) {
                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                makeToast("get material thumbnail failed");
            }
            materialInfoItem.thumbnail = thumbnail;
            mFilterMaterialList.set(i, materialInfoItem);

            Message msg = mHandler.obtainMessage(MSG_LOAD_THUMB);
            msg.arg2 = i+1;
            mHandler.sendMessage(msg);
        }
    }

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_LIST_SIZE:
                    initMaterialTabTypeView();
                    break;
                case MSG_LOAD_THUMB:
                    mMaterialListViewAdapter.setItemState(msg.arg2,
                            MaterialListViewAdapter.STATE_DOWNLOAD_THUMBNAIL);
                    updateListView(msg.arg2);
                    mMaterialListViewAdapter.notifyDataSetChanged();
                    break;
                case MSG_DOWNLOAD_SUCCESS:
                    mMaterialListViewAdapter.setItemState(msg.arg1,
                            MaterialListViewAdapter.STATE_DOWNLOADED);
                    updateListView(msg.arg1);
                    mMaterialListViewAdapter.notifyDataSetChanged();
                    break;
                case MSG_START_DOWNLOAD:
                    mMaterialListViewAdapter.setItemState(msg.arg1,
                            MaterialListViewAdapter.STATE_DOWNLOADING);
                    updateListView(msg.arg1);
                    mMaterialListViewAdapter.notifyDataSetChanged();
                    break;
                default:
                    Log.e(TAG, "Invalid message");
                    break;
            }
        }
    };


    private void initMaterialTabTypeView() {
        updateTabView(mCurrentTabIndex);
    }

    private void updateTabView(int tabIndex) {
        if(mIsFirstFetchMaterialList){
            startGetMaterialList();
            mIsFirstFetchMaterialList = false;
        }

        mCurrentTabIndex = tabIndex;
        if (tabIndex == 0) {
            mCurrentMaterialList = mThemeMaterialList;
            mCurrentMaterialIndex = mThemeSelectIndex;
        } else if (tabIndex == 1) {
            mCurrentMaterialList = mMusicMaterialList;
            mCurrentMaterialIndex = mMusicSelectIndex;
        } else if (tabIndex == 2) {
            mCurrentMaterialList = mFilterMaterialList;
            mCurrentMaterialIndex = mFilterSelectIndex;
        } else if (tabIndex == 3) {

        }

        updateMaterialsRecyclerView();
    }


    private void updateMaterialsRecyclerView() {
        if (mCurrentMaterialList == null) {
            Log.e(TAG, "The material list is null");
            return;
        }

        if (mCurrentTabIndex == 3) {
            mMediaListViewAdapter = new MediaListViewAdapter(mMediaItemList, getApplicationContext());
            mMediaListViewAdapter.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mMediaListViewAdapter);

            mMediaListViewAdapter.setOnItemClickListener(new MediaListViewAdapter.OnRecyclerViewListener() {
                @Override
                public void onItemClick(int position) {
                    onMediaItemClicked(position);
                }

                @Override
                public boolean onItemLongClick(int position) {
                    onMediaItemClicked(position);
                    return false;
                }

                @Override
                public void onDeleteClick(int position) {
                    onDeleteMediaItemClicked(position);
                }
            });
        } else {
            mMaterialListViewAdapter = new MaterialListViewAdapter(mCurrentMaterialList,
                    getApplicationContext());
            mMaterialListViewAdapter.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mMaterialListViewAdapter);

            mMaterialListViewAdapter.setSelectIndex(mCurrentMaterialIndex);

            mMaterialListViewAdapter.setOnItemClickListener(new MaterialListViewAdapter.OnRecyclerViewListener() {
                @Override
                public boolean onItemLongClick(int position) {
                    onMaterialItemClick(position);
                    return false;
                }

                @Override
                public void onItemClick(int position) {
                    onMaterialItemClick(position);
                }
            });
        }
    }

    private void onDeleteMediaItemClicked(int position) {
        if (position %2 != 0) {
            int mediaIndex = (position - 1 )/2;

            mEditor.removeVideoClip(mediaIndex);
            mMediaItemList.remove(mediaIndex);
            if (mCurrentTabIndex == 3) {
                mMediaListViewAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onMediaItemClicked(int position) {
        mCurrentPosition = position;
        if (position %2 == 0) {
            //start activity to add media item
            mMediaItemInsertIndex = position / 2;

            MediaOptions.Builder builder = new MediaOptions.Builder();
            MediaOptions options = builder.canSelectBothPhotoVideo().
                    canSelectMultiPhoto(true).canSelectMultiVideo(true).build();
            MediaPickerActivity.open(MediaEditActivity.this, REQUEST_MEDIA, options);
        } else {
            int mediaIndex = (position - 1 )/2;
            MediaItem item = mMediaItemList.get(mediaIndex);
            if (item.isPhoto()) {
                //edit photo
                Intent intent = new Intent(MediaEditActivity.this, PhotoEditActivity.class);
                PhotoConfig config = new PhotoConfig();
                config.index = mediaIndex;
                config.filePath = item.getPathOrigin(getApplicationContext());

                config.trimOut = mEditor.getTrimOut(mediaIndex);

                config.bright = mEditor.getBrightness(mediaIndex);
                config.contrast = mEditor.getContrast(mediaIndex);
                config.saturation = mEditor.getSaturation(mediaIndex);

                config.rotate = mEditor.getRotation(mediaIndex);
                intent.putExtra("config", config);

                startActivityForResult(intent, SINGLE_PHOTO_EDIT);
            } else if (item.isVideo()) {
                //edit video
                Intent intent = new Intent(MediaEditActivity.this, VideoEditActivity.class);
                VideoConfig config = new VideoConfig();
                config.index = mediaIndex;
                config.filePath = item.getPathOrigin(getApplicationContext());

                config.trimIn = mEditor.getTrimIn(mediaIndex);
                config.trimOut = mEditor.getTrimOut(mediaIndex);

                config.speed = mEditor.getSpeed(mediaIndex);

                config.bright = mEditor.getBrightness(mediaIndex);
                config.contrast = mEditor.getContrast(mediaIndex);
                config.saturation = mEditor.getSaturation(mediaIndex);

                config.rotate = mEditor.getRotation(mediaIndex);
                config.scale = mEditor.getScale(mediaIndex);
                intent.putExtra("config", config);
                startActivityForResult(intent, SINGLE_VIDEO_EDIT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_MEDIA) {
            ArrayList<MediaItem> mediaItemList = data
                    .getParcelableArrayListExtra(EXTRA_MEDIA_SELECTED);

            for (MediaItem item: mediaItemList) {
                mMediaItemList.add(mMediaItemInsertIndex, item);

                mEditor.insertVideoClip(item.getPathOrigin(this), mMediaItemInsertIndex);
                mMediaItemInsertIndex += 1;
            }
            if (mCurrentTabIndex == 3) {
                mMediaListViewAdapter.notifyDataSetChanged();
            }
        } else if (requestCode == SINGLE_VIDEO_EDIT) {
            VideoConfig config = data.getParcelableExtra("config");
            if (config != null && mEditor != null) {
                mEditor.changeTrimInPoint(config.index, config.trimIn);
                mEditor.changeTrimOutPoint(config.index, config.trimOut);

                mEditor.changeSpeed(config.index, config.speed);

                mEditor.changeBrightness(config.index, config.bright);
                mEditor.changeContrast(config.index, config.contrast);
                mEditor.changeSaturation(config.index, config.saturation);

                mEditor.changeRotation(config.index, config.rotate);
                mEditor.changeScale(config.index, config.scale);
            }

        } else if (requestCode == SINGLE_PHOTO_EDIT) {
            PhotoConfig config = data.getParcelableExtra("config");
            if (config != null && mEditor != null) {
                mEditor.changeTrimOutPoint(config.index, config.trimOut);

                mEditor.changeBrightness(config.index, config.bright);
                mEditor.changeContrast(config.index, config.contrast);
                mEditor.changeSaturation(config.index, config.saturation);

                mEditor.changeRotation(config.index, config.rotate);
            }

        }
    }

    private void onMaterialItemClick(int position) {
        MaterialInfoItem materialInfoItem = mCurrentMaterialList.get(position);

        if (position == 0) {
            mMaterial = null;
            mMaterialListViewAdapter.setSelectIndex(position);
            saveSelectedIndex(position);
            mMaterialListViewAdapter.notifyDataSetChanged();
            if (mEditor != null) {
                mEditor.removeMaterial(materialInfoItem.material);
            }
            return;
        }

        mMaterial = materialInfoItem.material;

        if(KMCMaterialManager.getInstance().isMaterialDownloaded(materialInfoItem.material)) {

            if(mMaterialListViewAdapter.getItemState(position) != MSG_DOWNLOAD_SUCCESS){
                mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOAD_SUCCESS, position, 0));
            }

            saveSelectedIndex(position);
            mMaterialListViewAdapter.setSelectIndex(position);
            if (mEditor != null) {
                mEditor.applyMaterial(mMaterial);
                play();
            }
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_START_DOWNLOAD, position, 0));
            KMCMaterialManager.getInstance().downloadMaterial(getApplicationContext(),
                    materialInfoItem.material, mDownloadListener);
        }
    }

    private void play() {
        if(getCurrentEngineState() != KMCShortVideo.STATE_PLAYBACK) {
            stopTimer();

            long startTime = mEditor.getCurrentPlaybackTime();
            // 播放视频
            mEditor.play(startTime, mEditor.getDuration());
            mButtonPlay.setImageResource(R.drawable.pause);

            startTimer();
        }
    }

    private void updateListView(int position) {
        if (mCurrentTabIndex != 3) {
            mMaterialListViewAdapter.updateItemView(position);
        }
    }

    private void saveSelectedIndex(int position) {
        if (mCurrentMaterialList == mThemeMaterialList) {
            mThemeSelectIndex = position;
            mMusicSelectIndex = -1;
        } else {
            mThemeSelectIndex = -1;
            mMusicSelectIndex = position;
        }
        mCurrentMaterialIndex = position;
    }

    /**
     * 单独下载贴纸素材的回调对象
     */
    private KMCMaterialManager.DownloadMaterialListener mDownloadListener = new KMCMaterialManager.DownloadMaterialListener() {
        /**
         * 下载成功
         * @param material 下载成功的素材
         */
        @Override
        public void onSuccess(KMCMaterial material) {
            int position = 0;

            for (int j = 0; j < mCurrentMaterialList.size(); j++) {
                String id = mCurrentMaterialList.get(j).material.id;
                if (id != null && id.equals(material.id)) {
                    position = j;
                    mCurrentMaterialList.get(j).setHasDownload(true);
                }
            }
            Log.d(TAG, "download success for position " + position);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOAD_SUCCESS, position, 0));

        }

        /**
         * 下载失败
         * @param material 下载失败的素材
         * @param code 失败原因的错误代码
         * @param message 失败原因的解释
         */
        @Override
        public void onFailure(KMCMaterial material, int code, String message) {
            mMaterial = null;

        }

        /**
         * 下载过程中的进度回调
         * @param material  正在下载素材
         * @param progress 当前下载的进度
         * @param size 已经下载素材的大小, 单位byte
         */
        @Override
        public void onProgress(KMCMaterial material, float progress, int size) {
        }
    };
}
