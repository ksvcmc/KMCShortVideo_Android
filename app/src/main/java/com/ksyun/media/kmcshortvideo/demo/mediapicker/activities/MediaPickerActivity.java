package com.ksyun.media.kmcshortvideo.demo.mediapicker.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.ksyun.media.kmcshortvideo.demo.R;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.CropListener;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaItem;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaOptions;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaSelectedListener;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.imageloader.MediaImageLoader;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.imageloader.MediaImageLoaderImpl;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.MediaUtils;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.MessageUtils;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.RecursiveFileObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author TUNGDX
 */

/**
 * Use this activity for pickup photos or videos (media).
 * <p/>
 * How to use:
 * <ul>
 * <li>
 * Step1: Open media picker: <br/>
 * - If using in activity use:
 * {@link MediaPickerActivity#open(Activity, int, MediaOptions)} or
 * {@link MediaPickerActivity#open(Activity, int)}</li>
 * - If using in fragment use:
 * {@link MediaPickerActivity#open(Fragment, int, MediaOptions)} or
 * {@link MediaPickerActivity#open(Fragment, int)} <br/>
 * </li>
 * <li>
 * Step2: Get out media that selected in
 * {@link Activity#onActivityResult(int, int, Intent)} of activity or fragment
 * that open media picker. Use
 * {@link MediaPickerActivity#getMediaItemSelected(Intent)} to get out media
 * list that selected.</li>
 * <p/>
 * <i>Note: Videos or photos return back depends on {@link MediaOptions} passed
 * to {@link #open(Activity, int, MediaOptions)} </i></li>
 * </ul>
 */
public class MediaPickerActivity extends AppCompatActivity implements
        MediaSelectedListener, CropListener, FragmentManager.OnBackStackChangedListener, FragmentHost {
    private static final String TAG = "MediaPickerActivity";

    public static final String EXTRA_MEDIA_OPTIONS = "extra_media_options";
    /**
     * Intent extra included when return back data in
     * {@link Activity#onActivityResult(int, int, Intent)} of activity or fragment
     * that open media picker. Always return {@link ArrayList} of
     * {@link MediaItem}. You must always check null and size of this list
     * before handle your logic.
     */
    public static final String EXTRA_MEDIA_SELECTED = "extra_media_selected";
    private static final int REQUEST_PHOTO_CAPTURE = 100;
    private static final int REQUEST_VIDEO_CAPTURE = 200;

    private static final String KEY_PHOTOFILE_CAPTURE = "key_photofile_capture";
    private static final int REQUEST_CAMERA_PERMISSION = 300;

    private MediaOptions mMediaOptions;

    private File mPhotoFileCapture;
    private List<File> mFilesCreatedWhileCapturePhoto;
    private RecursiveFileObserver mFileObserver;
    private FileObserverTask mFileObserverTask;
    private boolean takePhotoPending;
    private boolean takeVideoPending;

    private ImageView mBackOff;
    private Button mSelectAll;
    private Button mSelectVideo;
    private Button mSelectPhoto;
    private Button mSelectDone;
    private ButtonObserver mButtonObserver;

    /**
     * Start {@link MediaPickerActivity} in {@link Activity} to pick photo or
     * video that depends on {@link MediaOptions} passed.
     *
     * @param activity
     * @param requestCode
     * @param options
     */
    public static void open(Activity activity, int requestCode,
                            MediaOptions options) {
        Intent intent = new Intent(activity, MediaPickerActivity.class);
        intent.putExtra(EXTRA_MEDIA_OPTIONS, options);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Start {@link MediaPickerActivity} in {@link Activity} with default media
     * option: {@link MediaOptions#createDefault()}
     *
     * @param activity
     * @param requestCode
     */
    public static void open(Activity activity, int requestCode) {
        open(activity, requestCode, MediaOptions.createDefault());
    }

    /**
     * Start {@link MediaPickerActivity} in {@link Fragment} to pick photo or
     * video that depends on {@link MediaOptions} passed.
     *
     * @param fragment
     * @param requestCode
     * @param options
     */
    public static void open(Fragment fragment, int requestCode,
                            MediaOptions options) {
        Intent intent = new Intent(fragment.getActivity(),
                MediaPickerActivity.class);
        intent.putExtra(EXTRA_MEDIA_OPTIONS, options);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Start {@link MediaPickerActivity} in {@link Fragment} with default media
     * option: {@link MediaOptions#createDefault()}
     *
     * @param fragment
     * @param requestCode
     */
    public static void open(Fragment fragment, int requestCode) {
        open(fragment, requestCode, MediaOptions.createDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: not support change orientation right now (because out of
        // memory when crop image and change orientation, must check third party
        // to crop image again).
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_mediapicker);
        if (savedInstanceState != null) {
            mMediaOptions = savedInstanceState
                    .getParcelable(EXTRA_MEDIA_OPTIONS);
            mPhotoFileCapture = (File) savedInstanceState
                    .getSerializable(KEY_PHOTOFILE_CAPTURE);
        } else {
            mMediaOptions = getIntent().getParcelableExtra(EXTRA_MEDIA_OPTIONS);
            if (mMediaOptions == null) {
                throw new IllegalArgumentException(
                        "MediaOptions must be not null, you should use MediaPickerActivity.open(Activity activity, int requestCode,MediaOptions options) method instead.");
            }
        }
        if (getActivePage() == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container,
                            MediaPickerFragment.newInstance(mMediaOptions))
                    .commit();
        }
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        mBackOff = (ImageView) findViewById(R.id.media_picker_back);
        mSelectAll = (Button) findViewById(R.id.choose_all);
        mSelectVideo = (Button) findViewById(R.id.choose_video);
        mSelectPhoto = (Button) findViewById(R.id.choose_photo);
        mSelectDone = (Button) findViewById(R.id.media_picker_done);

        mButtonObserver = new ButtonObserver();
        mBackOff.setOnClickListener(mButtonObserver);
        mSelectAll.setOnClickListener(mButtonObserver);
        mSelectVideo.setOnClickListener(mButtonObserver);
        mSelectPhoto.setOnClickListener(mButtonObserver);
        mSelectDone.setOnClickListener(mButtonObserver);

        mSelectAll.setSelected(true);
        mSelectVideo.setSelected(false);
        mSelectPhoto.setSelected(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        cancelFileObserverTask();
        stopWatchingFile();
        mFilesCreatedWhileCapturePhoto = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_MEDIA_OPTIONS, mMediaOptions);
        outState.putSerializable(KEY_PHOTOFILE_CAPTURE, mPhotoFileCapture);
    }

    @Override
    public MediaImageLoader getImageLoader() {
        return new MediaImageLoaderImpl(getApplicationContext());
    }

    @Override
    public void onHasNoSelected() {
    }

    @Override
    public void onHasSelected(List<MediaItem> mediaSelectedList) {
    }

    private void returnBackData(List<MediaItem> mediaSelectedList) {
        Intent data = new Intent();
        data.putParcelableArrayListExtra(EXTRA_MEDIA_SELECTED,
                (ArrayList<MediaItem>) mediaSelectedList);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private RecursiveFileObserver.OnFileCreatedListener mOnFileCreatedListener = new RecursiveFileObserver.OnFileCreatedListener() {

        @Override
        public void onFileCreate(File file) {
            if (mFilesCreatedWhileCapturePhoto == null)
                mFilesCreatedWhileCapturePhoto = new ArrayList<File>();
            mFilesCreatedWhileCapturePhoto.add(file);
        }
    };

    /**
     * In some HTC devices (maybe others), duplicate image when captured with
     * extra_output. This method will try delete duplicate image. It's prefer
     * default image by camera than extra output.
     */
    private void tryCorrectPhotoFileCaptured() {
        if (mPhotoFileCapture == null || mFilesCreatedWhileCapturePhoto == null
                || mFilesCreatedWhileCapturePhoto.size() <= 0)
            return;
        long captureSize = mPhotoFileCapture.length();
        for (File file : mFilesCreatedWhileCapturePhoto) {
            if (MediaUtils
                    .isImageExtension(MediaUtils.getFileExtension(file))
                    && file.length() >= captureSize
                    && !file.equals(mPhotoFileCapture)) {
                boolean value = mPhotoFileCapture.delete();
                mPhotoFileCapture = file;
                Log.i(TAG,
                        String.format(
                                "Try correct photo file: Delete duplicate photos in [%s] [%s]",
                                mPhotoFileCapture, value));
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cancelFileObserverTask();
        stopWatchingFile();
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PHOTO_CAPTURE:
                    tryCorrectPhotoFileCaptured();
                    if (mPhotoFileCapture != null) {
                        MediaUtils.galleryAddPic(getApplicationContext(),
                                mPhotoFileCapture);
                        if (mMediaOptions.isCropped()) {
                            MediaItem item = new MediaItem(MediaItem.PHOTO,
                                    Uri.fromFile(mPhotoFileCapture));
                            showCropFragment(item, mMediaOptions);
                        } else {
                            MediaItem item = new MediaItem(MediaItem.PHOTO,
                                    Uri.fromFile(mPhotoFileCapture));
                            ArrayList<MediaItem> list = new ArrayList<MediaItem>();
                            list.add(item);
                            returnBackData(list);
                        }
                    }
                    break;
                case REQUEST_VIDEO_CAPTURE:
                    returnVideo(data.getData());
                    break;
                default:
                    break;
            }
        }
    }

    private void showCropFragment(MediaItem mediaItem, MediaOptions options) {
        Fragment fragment = PhotoCropFragment.newInstance(mediaItem, options);
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onSuccess(MediaItem mediaItem) {
        List<MediaItem> list = new ArrayList<MediaItem>();
        list.add(mediaItem);
        returnBackData(list);
    }

    @Override
    public void onBackStackChanged() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private Fragment getActivePage() {
        return getSupportFragmentManager().findFragmentById(R.id.container);
    }

    /**
     * Check video duration valid or not with options.
     *
     * @param videoUri
     * @return 1 if valid, otherwise is invalid. -2: not found, 0 larger than
     * accepted, -1 smaller than accepted.
     */
    private int checkValidVideo(Uri videoUri) {
        if (videoUri == null)
            return -2;
        // try get duration using MediaPlayer. (Should get duration using
        // MediaPlayer before use Uri because some devices can get duration by
        // Uri or not exactly. Ex: Asus Memo Pad8)
        long duration = MediaUtils.getDuration(getApplicationContext(),
                MediaUtils.getRealVideoPathFromURI(getContentResolver(), videoUri));
        if (duration == 0) {
            // try get duration one more, by uri of video. Note: Some time can
            // not get duration by Uri after record video.(It's usually happen
            // in HTC
            // devices 2.3, maybe others)
            duration = MediaUtils
                    .getDuration(getApplicationContext(), videoUri);
        }
        // accept delta about < 1000 milliseconds. (ex: 10769 is still accepted
        // if limit is 10000)
        if (mMediaOptions.getMaxVideoDuration() != Integer.MAX_VALUE
                && duration >= mMediaOptions.getMaxVideoDuration() + 1000) {
            return 0;
        } else if (duration == 0
                || duration < mMediaOptions.getMinVideoDuration()) {
            return -1;
        }
        return 1;
    }

    private void returnVideo(Uri videoUri) {
        final int code = checkValidVideo(videoUri);
        switch (code) {
            // not found. should never happen. Do nothing when happen.
            case -2:

                break;
            // smaller than min
            case -1:
                // in seconds
                int duration = mMediaOptions.getMinVideoDuration() / 1000;
                String msg = MessageUtils.getInvalidMessageMinVideoDuration(
                        getApplicationContext(), duration);
                showVideoInvalid(msg);
                break;

            // larger than max
            case 0:
                // in seconds.
                duration = mMediaOptions.getMaxVideoDuration() / 1000;
                msg = MessageUtils.getInvalidMessageMaxVideoDuration(
                        getApplicationContext(), duration);
                showVideoInvalid(msg);
                break;
            // ok
            case 1:
                MediaItem item = new MediaItem(MediaItem.VIDEO, videoUri);
                ArrayList<MediaItem> list = new ArrayList<MediaItem>();
                list.add(item);
                returnBackData(list);
                break;

            default:
                break;
        }
    }

    private void showVideoInvalid(String msg) {
        MediaPickerErrorDialog errorDialog = MediaPickerErrorDialog
                .newInstance(msg);
        errorDialog.show(getSupportFragmentManager(), null);
    }

    /**
     * Get media item list selected from intent extra included in
     * {@link Activity#onActivityResult(int, int, Intent)} of activity or fragment
     * that open media picker.
     *
     * @param intent In {@link Activity#onActivityResult(int, int, Intent)} method of
     *               activity or fragment that open media picker.
     * @return Always return {@link ArrayList} of {@link MediaItem}. You must
     * always check null and size of this list before handle your logic.
     */
    public static ArrayList<MediaItem> getMediaItemSelected(Intent intent) {
        if (intent == null)
            return null;
        ArrayList<MediaItem> mediaItemList = intent
                .getParcelableArrayListExtra(MediaPickerActivity.EXTRA_MEDIA_SELECTED);
        return mediaItemList;
    }

    private class FileObserverTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (isCancelled()) return null;
            if (mFileObserver == null) {
                mFileObserver = new RecursiveFileObserver(Environment
                        .getExternalStorageDirectory().getAbsolutePath(),
                        FileObserver.CREATE);
                mFileObserver
                        .setFileCreatedListener(mOnFileCreatedListener);
            }
            mFileObserver.startWatching();
            return null;
        }
    }

    private void cancelFileObserverTask() {
        if (mFileObserverTask != null) {
            mFileObserverTask.cancel(true);
            mFileObserver = null;
        }
    }

    private void stopWatchingFile() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }

    class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.media_picker_back:
                    finish();
                    break;
                case R.id.choose_all:
                    mSelectAll.setSelected(true);
                    mSelectVideo.setSelected(false);
                    mSelectPhoto.setSelected(false);
                    mSelectAll.setTextColor(getResources().getColor(R.color.white));
                    mSelectVideo.setTextColor(getResources().getColor(R.color.grey));
                    mSelectPhoto.setTextColor(getResources().getColor(R.color.grey));
                    requestPhotoAndVideo();
                    break;
                case R.id.choose_video:
                    mSelectAll.setSelected(false);
                    mSelectVideo.setSelected(true);
                    mSelectPhoto.setSelected(false);
                    mSelectAll.setTextColor(getResources().getColor(R.color.grey));
                    mSelectVideo.setTextColor(getResources().getColor(R.color.white));
                    mSelectPhoto.setTextColor(getResources().getColor(R.color.grey));
                    requestVideo();
                    break;
                case R.id.choose_photo:
                    mSelectAll.setSelected(false);
                    mSelectVideo.setSelected(false);
                    mSelectPhoto.setSelected(true);
                    mSelectAll.setTextColor(getResources().getColor(R.color.grey));
                    mSelectVideo.setTextColor(getResources().getColor(R.color.grey));
                    mSelectPhoto.setTextColor(getResources().getColor(R.color.white));
                    requestPhoto();
                    break;
                case R.id.media_picker_done:
                    onDone();
                    break;
                default:
                    break;
            }
        }
    }

    private void requestPhotoAndVideo() {
        requestMedia(MediaPickerFragment.PHOTO_AND_VIDEO);
    }

    private void requestPhoto() {
        requestMedia(MediaPickerFragment.PHOTO);
    }

    private void requestVideo() {
        requestMedia(MediaPickerFragment.VIDEO);
    }

    private void requestMedia(int mediaType) {
        Fragment activePage = getActivePage();
        if (mMediaOptions.canSelectPhotoAndVideo()
                && activePage instanceof MediaPickerFragment) {
            MediaPickerFragment mediaPickerFragment = ((MediaPickerFragment) activePage);
            mediaPickerFragment.clearSelectedList();
            mediaPickerFragment.requestMedia(mediaType);
        }
    }

    private void onDone() {
        Fragment activePage;
        activePage = getActivePage();
        boolean isPhoto = ((MediaPickerFragment) activePage)
                .getMediaType() == MediaItem.PHOTO;
        if (isPhoto) {
            if (mMediaOptions.isCropped()
                    && !mMediaOptions.canSelectMultiPhoto()) {
                // get first item in list (pos=0) because can only crop 1 image at same time.
                MediaItem mediaItem = new MediaItem(MediaItem.PHOTO,
                        ((MediaPickerFragment) activePage)
                                .getMediaSelectedList().get(0)
                                .getUriOrigin());
                showCropFragment(mediaItem, mMediaOptions);
            } else {
                returnBackData(((MediaPickerFragment) activePage)
                        .getMediaSelectedList());
            }
        } else {
            if (mMediaOptions.canSelectMultiVideo()) {
                returnBackData(((MediaPickerFragment) activePage)
                        .getMediaSelectedList());
            } else {
                // only get 1st item regardless of have many.
                returnVideo(((MediaPickerFragment) activePage)
                        .getMediaSelectedList().get(0).getUriOrigin());
            }
        }
    }
}