package com.ksyun.media.kmcshortvideo.demo.mediapicker.activities;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.ksyun.media.kmcshortvideo.demo.R;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaAdapter;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaItem;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaOptions;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaSelectedListener;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.MediaUtils;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.Utils;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.widget.HeaderGridView;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.widget.PickerImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TUNGDX
 */

/**
 * Display list of videos, photos from {@link MediaStore} and select one or many
 * item from list depends on {@link MediaOptions} that passed when open media
 * picker.
 */
public class MediaPickerFragment extends BaseFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
    private static final String LOADER_EXTRA_URI = "loader_extra_uri";
    private static final String LOADER_EXTRA_PROJECT = "loader_extra_project";
    private static final String LOADER_EXTRA_SELECTION = "loader_extra_selection";
    private static final String KEY_MEDIA_TYPE = "media_type";
    private static final String KEY_GRID_STATE = "grid_state";
    private static final String KEY_MEDIA_SELECTED_LIST = "media_selected_list";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;

    public static final int PHOTO = 1;
    public static final int VIDEO = 2;
    public static final int PHOTO_AND_VIDEO = 3;

    private HeaderGridView mGridView;
    private TextView mNoItemView;
    private MediaAdapter mMediaAdapter;
    private MediaOptions mMediaOptions;
    private MediaSelectedListener mMediaSelectedListener;
    private Bundle mSavedInstanceState;
    private List<MediaItem> mMediaSelectedList;

    private int mMediaType;
    private int mPhotoSize, mPhotoSpacing;

    public MediaPickerFragment() {
        mSavedInstanceState = new Bundle();
    }

    public static MediaPickerFragment newInstance(MediaOptions options) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(MediaPickerActivity.EXTRA_MEDIA_OPTIONS, options);
        MediaPickerFragment fragment = new MediaPickerFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaSelectedListener = (MediaSelectedListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mMediaOptions = savedInstanceState
                    .getParcelable(MediaPickerActivity.EXTRA_MEDIA_OPTIONS);
            mMediaType = savedInstanceState.getInt(KEY_MEDIA_TYPE);
            mMediaSelectedList = savedInstanceState
                    .getParcelableArrayList(KEY_MEDIA_SELECTED_LIST);
            mSavedInstanceState = savedInstanceState;
        } else {
            mMediaOptions = getArguments().getParcelable(
                    MediaPickerActivity.EXTRA_MEDIA_OPTIONS);
            if (mMediaOptions.canSelectPhotoAndVideo()) {
                mMediaType = PHOTO_AND_VIDEO;
            } else if (mMediaOptions.canSelectPhoto()) {
                mMediaType = PHOTO;
            } else {
                mMediaType = VIDEO;
            }
            mMediaSelectedList = mMediaOptions.getMediaListSelected();
            // Override mediaType by 1st item media if has media selected.
            if (mMediaSelectedList != null && mMediaSelectedList.size() > 0) {
                mMediaType = mMediaSelectedList.get(0).getType();
            }
        }
        // get the photo size and spacing
        mPhotoSize = getResources().getDimensionPixelSize(
                R.dimen.picker_photo_size);
        mPhotoSpacing = getResources().getDimensionPixelSize(
                R.dimen.picker_photo_spacing);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.mediapicker_fragment, container,
                false);
        initView(root);
        return root;
    }

    private void requestPhotos(boolean isRestart) {
        requestMedia(Images.Media.EXTERNAL_CONTENT_URI,
                MediaUtils.PROJECT_PHOTO, null, isRestart);
    }

    private void requestVideos(boolean isRestart) {
        requestMedia(Video.Media.EXTERNAL_CONTENT_URI,
                MediaUtils.PROJECT_VIDEO, null, isRestart);
    }

    private void requestPhotoAndVideos(boolean isRestart) {
        // Get relevant columns for use later.
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE
        };

        // Return only video and image metadata.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");
        requestMedia(queryUri, projection, selection, isRestart);
    }

    private void requestMedia(Uri uri, String[] projects, String selection,
                              boolean isRestart) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(LOADER_EXTRA_PROJECT, projects);
        bundle.putString(LOADER_EXTRA_URI, uri.toString());
        bundle.putString(LOADER_EXTRA_SELECTION, selection);
        if (isRestart)
            getLoaderManager().restartLoader(0, bundle, this);
        else
            getLoaderManager().initLoader(0, bundle, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mGridView != null) {
            mSavedInstanceState.putParcelable(KEY_GRID_STATE,
                    mGridView.onSaveInstanceState());
        }
        mSavedInstanceState.putParcelable(
                MediaPickerActivity.EXTRA_MEDIA_OPTIONS, mMediaOptions);
        mSavedInstanceState.putInt(KEY_MEDIA_TYPE, mMediaType);
        mSavedInstanceState.putParcelableArrayList(KEY_MEDIA_SELECTED_LIST,
                (ArrayList<MediaItem>) mMediaSelectedList);
        outState.putAll(mSavedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri uri = Uri.parse(bundle.getString(LOADER_EXTRA_URI));
        String[] projects = bundle.getStringArray(LOADER_EXTRA_PROJECT);
        String selection = bundle.getString(LOADER_EXTRA_SELECTION);
        String order = MediaColumns.DATE_ADDED + " DESC";
        return new CursorLoader(mContext, uri, projects, selection, null, order);
    }

    private void bindData(Cursor cursor) {
        if (cursor == null || cursor.getCount() <= 0) {
            switchToError();
            return;
        }
        switchToData();
        if (mMediaAdapter == null) {
            mMediaAdapter = new MediaAdapter(mContext, cursor, 0,
                    mMediaImageLoader, mMediaType, mMediaOptions);
        } else {
            mMediaAdapter.setMediaType(mMediaType);
            mMediaAdapter.swapCursor(cursor);
        }
        if (mGridView.getAdapter() == null) {
            mGridView.setAdapter(mMediaAdapter);
            mGridView.setRecyclerListener(mMediaAdapter);
        }
        Parcelable state = mSavedInstanceState.getParcelable(KEY_GRID_STATE);
        if (state != null) {
            mGridView.onRestoreInstanceState(state);
        }
        if (mMediaSelectedList != null) {
            mMediaAdapter.setMediaSelectedList(mMediaSelectedList);
        }
        mMediaAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        bindData(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Preference:http://developer.android.com/guide/components/loaders.html#callback
        if (mMediaAdapter != null)
            mMediaAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Object object = parent.getAdapter().getItem(position);
        if (object instanceof Cursor) {
            int mediaType = getType((Cursor) object, mMediaType);
            Uri uri = MediaUtils.getMediaUri((Cursor ) object, mediaType);
            PickerImageView pickerImageView = (PickerImageView) view
                    .findViewById(R.id.thumbnail);

            TextView textView = (TextView) view.findViewById(R.id.selected_num);
            textView.setTextColor(getResources().getColor(R.color.white));
            textView.setBackgroundColor(getResources().getColor(R.color.purple));

            MediaItem mediaItem = new MediaItem(mediaType, uri);
            mMediaAdapter.updateMediaSelected(mediaItem, pickerImageView, textView);
            mMediaSelectedList = mMediaAdapter.getMediaSelectedList();

            if (mMediaAdapter.hasSelected()) {
                mMediaSelectedListener.onHasSelected(mMediaAdapter
                        .getMediaSelectedList());
            } else {
                mMediaSelectedListener.onHasNoSelected();
            }
        }
    }

    private int getType(Cursor cursor, int mediaType) {
        if (mediaType == PHOTO_AND_VIDEO) {
            int type = cursor.getInt(cursor.getColumnIndex(
                    MediaStore.Files.FileColumns.MEDIA_TYPE));
            if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                return PHOTO;
            } else { //MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                return VIDEO;
            }
        } else {
            return mediaType;
        }
    }

    public void switchMediaSelector() {
        if (!mMediaOptions.canSelectPhotoAndVideo())
            return;
        if (mMediaType == PHOTO) {
            mMediaType = VIDEO;
        } else {
            mMediaType = PHOTO;
        }
        switch (mMediaType) {
            case PHOTO:
                requestPhotos(true);
                break;
            case VIDEO:
                requestVideos(true);
                break;
            default:
                break;
        }
    }

    public void clearSelectedList() {
        mMediaSelectedList.clear();
        if (mMediaAdapter != null) {
            mMediaAdapter.clearSelectedList();
        }
    }

    public void requestMedia(int mediaType) {
        switch (mediaType) {
            case PHOTO:
                requestPhotos(true);
                mMediaType = PHOTO;
                break;
            case VIDEO:
                requestVideos(true);
                mMediaType = VIDEO;
                break;
            case PHOTO_AND_VIDEO:
                requestPhotoAndVideos(true);
                mMediaType = PHOTO_AND_VIDEO;
            default:
                break;
        }
    }

    public List<MediaItem> getMediaSelectedList() {
        return mMediaSelectedList;
    }

    public boolean hasMediaSelected() {
        return mMediaSelectedList != null && mMediaSelectedList.size() > 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mGridView != null) {
            mSavedInstanceState.putParcelable(KEY_GRID_STATE,
                    mGridView.onSaveInstanceState());
            mGridView = null;
        }
        if (mMediaAdapter != null) {
            mMediaAdapter.onDestroyView();
        }
    }

    public int getMediaType() {
        return mMediaType;
    }

    private void switchToData() {
        mNoItemView.setVisibility(View.GONE);
        mNoItemView.setText(null);
        mGridView.setVisibility(View.VISIBLE);
    }

    private void switchToError() {
        mNoItemView.setVisibility(View.VISIBLE);
        mNoItemView.setText(R.string.picker_no_items);
        mGridView.setVisibility(View.GONE);
    }

    private void initView(View view) {
        mGridView = (HeaderGridView) view.findViewById(R.id.grid);
        View header = new View(getActivity());
        ViewGroup.LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.getActionbarHeight(getActivity()));
        header.setLayoutParams(params);
        mGridView.addHeaderView(header);

        mGridView.setOnItemClickListener(this);
        mNoItemView = (TextView) view.findViewById(R.id.no_data);

        // get the view tree observer of the grid and set the height and numcols
        // dynamically
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mMediaAdapter != null
                                && mMediaAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(mGridView
                                    .getWidth() / (mPhotoSize + mPhotoSpacing));
                            if (numColumns > 0) {
                                final int columnWidth = (mGridView.getWidth() / numColumns)
                                        - mPhotoSpacing;
                                mMediaAdapter.setNumColumns(numColumns);
                                mMediaAdapter.setItemHeight(columnWidth);
                            }
                        }
                    }
                });
    }

    private void requestMedia() {
        if (mMediaType == PHOTO) {
            requestPhotos(false);
        } else if (mMediaType == VIDEO){
            requestVideos(false);
        } else if(mMediaType == PHOTO_AND_VIDEO) {
            requestPhotoAndVideos(false);
        }
    }

    private void performRequestMedia() {
        if (hasPermission()) {
            requestMedia();
        } else {
            requestReadingExternalStoragePermission();
        }
    }

    private void requestReadingExternalStoragePermission() {
        requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"},
                REQUEST_READ_EXTERNAL_STORAGE);
    }

    private boolean hasPermission() {
        int permission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestMedia();
                }
                return;
        }
        //handle permissions that passed from the host activity.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestMedia();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        performRequestMedia();
    }
}