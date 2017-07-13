package com.ksyun.media.kmcshortvideo.demo.mediapicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView.RecyclerListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ksyun.media.kmcshortvideo.demo.R;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.activities.MediaPickerFragment;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.imageloader.MediaImageLoader;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.utils.MediaUtils;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.widget.PickerImageView;

import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.CENTER;

/**
 * @author TUNGDX
 */

/**
 * Adapter for display media item list.
 */
public class MediaAdapter extends CursorAdapter implements RecyclerListener {
    private int mMediaType;
    private MediaImageLoader mMediaImageLoader;
    private List<MediaItem> mMediaListSelected = new ArrayList<MediaItem>();
    private MediaOptions mMediaOptions;
    private int mItemHeight = 0;
    private int mNumColumns = 0;
    private FrameLayout.LayoutParams mImageViewLayoutParams;
    private List<PickerImageView> mPickerImageViewSelected = new ArrayList<PickerImageView>();
    private List<TextView> mTextviewSelected = new ArrayList<TextView>();

    public MediaAdapter(Context context, Cursor c, int flags,
                        MediaImageLoader mediaImageLoader, int mediaType, MediaOptions mediaOptions) {
        this(context, c, flags, null, mediaImageLoader, mediaType, mediaOptions);
    }

    public MediaAdapter(Context context, Cursor c, int flags,
                        List<MediaItem> mediaListSelected, MediaImageLoader mediaImageLoader,
                        int mediaType, MediaOptions mediaOptions) {
        super(context, c, flags);
        if (mediaListSelected != null)
            mMediaListSelected = mediaListSelected;
        mMediaImageLoader = mediaImageLoader;
        mMediaType = mediaType;
        mMediaOptions = mediaOptions;
        mImageViewLayoutParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        int mediaType = getType(cursor, mMediaType);
        Uri uri = MediaUtils.getMediaUri(cursor, mediaType);
        if (mediaType == MediaItem.PHOTO) {
            holder.thumbnail.setVisibility(View.GONE);
            holder.duration.setVisibility(View.GONE);
        } else {
            holder.thumbnail.setVisibility(View.VISIBLE);

            long duration = MediaUtils.getDuration(context, uri);
            duration /= 1000;
            long hours = duration / 3600;
            long minutes = (duration - hours * 3600) / 60;
            long seconds = duration - (hours * 3600 + minutes * 60);
            String length = String.format("%02d:%02d", hours*60 + minutes, seconds);
            holder.duration.setVisibility(View.VISIBLE);
            holder.duration.setText(length);
        }
        boolean isSelected = isSelected(uri);
        holder.imageView.setSelected(isSelected);
        if (isSelected) {
            mPickerImageViewSelected.add(holder.imageView);

            MediaItem item = new MediaItem(mediaType, uri);
            for (int i = 0; i < mMediaListSelected.size(); i++) {
                if (item.getUriOrigin().equals(mMediaListSelected.get(i).getUriOrigin())) {
                    holder.mask.setText(String.valueOf(i + 1));
                    holder.mask.setTextSize(48);
                    holder.mask.setGravity(CENTER);
                }
            }
            holder.mask.setVisibility(View.VISIBLE);
        } else {
            holder.mask.setVisibility(View.INVISIBLE);
        }
        mMediaImageLoader.displayImage(uri, holder.imageView);
    }

    private int getType(Cursor cursor, int mediaType) {
        if (mediaType == MediaPickerFragment.PHOTO_AND_VIDEO) {
            int type = cursor.getInt(cursor.getColumnIndex(
                    MediaStore.Files.FileColumns.MEDIA_TYPE));
            if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                return MediaItem.PHOTO;
            } else { //MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                return MediaItem.VIDEO;
            }
        } else {
            return mediaType;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        ViewHolder holder = new ViewHolder();
        View root = View
                .inflate(mContext, R.layout.mediapicker_list_item, null);
        holder.imageView = (PickerImageView) root.findViewById(R.id.thumbnail);
        holder.thumbnail = root.findViewById(R.id.overlay);

        holder.mask = (TextView) root.findViewById(R.id.selected_num);
        holder.mask.setVisibility(View.INVISIBLE);
        holder.mask.setLayoutParams(mImageViewLayoutParams);
        // Check the height matches our calculated column width
        if (holder.mask.getLayoutParams().height != mItemHeight) {
            holder.mask.setLayoutParams(mImageViewLayoutParams);
        }

        holder.duration = (TextView) root.findViewById(R.id.video_duration);

        holder.imageView.setLayoutParams(mImageViewLayoutParams);
        // Check the height matches our calculated column width
        if (holder.imageView.getLayoutParams().height != mItemHeight) {
            holder.imageView.setLayoutParams(mImageViewLayoutParams);
        }
        root.setTag(holder);
        return root;
    }

    private class ViewHolder {
        PickerImageView imageView;
        View thumbnail;
        TextView mask;
        TextView duration;
    }

    public boolean hasSelected() {
        return mMediaListSelected.size() > 0;
    }

    /**
     * Check media uri is selected or not.
     *
     * @param uri Uri of media item (photo, video)
     * @return true if selected, false otherwise.
     */
    public boolean isSelected(Uri uri) {
        if (uri == null)
            return false;
        for (MediaItem item : mMediaListSelected) {
            if (item.getUriOrigin().equals(uri))
                return true;
        }
        return false;
    }

    /**
     * Check {@link MediaItem} is selected or not.
     *
     * @param item {@link MediaItem} to check.
     * @return true if selected, false otherwise.
     */
    public boolean isSelected(MediaItem item) {
        return mMediaListSelected.contains(item);
    }

    /**
     * Set {@link MediaItem} selected.
     *
     * @param item {@link MediaItem} to selected.
     */
    public void setMediaSelected(MediaItem item) {
        syncMediaSelectedAsOptions();
        if (!mMediaListSelected.contains(item))
            mMediaListSelected.add(item);
    }

    public void clearSelectedList() {
        if (mMediaListSelected != null) {
            mMediaListSelected.clear();
        }
        if (mPickerImageViewSelected != null) {
            for (PickerImageView picker : this.mPickerImageViewSelected) {
                picker.setSelected(false);
            }
            mPickerImageViewSelected.clear();
        }
        if (mTextviewSelected != null) {
            for (TextView textView: mTextviewSelected) {
                textView.setVisibility(View.INVISIBLE);
            }
            mTextviewSelected.clear();
        }
    }

    /**
     * If item selected then change to unselected and unselected to selected.
     *
     * @param item Item to update.
     */
    public void updateMediaSelected(MediaItem item,
                                    PickerImageView pickerImageView) {
        if (mMediaListSelected.contains(item)) {
            mMediaListSelected.remove(item);

            pickerImageView.setSelected(false);
            this.mPickerImageViewSelected.remove(pickerImageView);
        } else {
            mMediaListSelected.add(item);

            boolean value = syncMediaSelectedAsOptions();
            if (value) {
                for (PickerImageView picker : this.mPickerImageViewSelected) {
                    picker.setSelected(false);
                }
                this.mPickerImageViewSelected.clear();
            }

            pickerImageView.setSelected(true);
            this.mPickerImageViewSelected.add(pickerImageView);
        }
    }

    public void updateMediaSelected(MediaItem item,
                                    PickerImageView pickerImageView, TextView textView) {
        if (mMediaListSelected.contains(item)) {
            mMediaListSelected.remove(item);

            textView.setVisibility(View.INVISIBLE);
            this.mTextviewSelected.remove(textView);

            pickerImageView.setSelected(false);
            this.mPickerImageViewSelected.remove(pickerImageView);
        } else {
            mMediaListSelected.add(item);

            textView.setVisibility(View.VISIBLE);
            this.mTextviewSelected.add(textView);

            boolean value = syncMediaSelectedAsOptions();
            if (value) {
                for (PickerImageView picker : this.mPickerImageViewSelected) {
                    picker.setSelected(false);
                }
                this.mPickerImageViewSelected.clear();
            }

            pickerImageView.setSelected(true);
            this.mPickerImageViewSelected.add(pickerImageView);
        }

        updateTextMask();
    }

    private void updateTextMask() {
        for (int i = 0; i < mTextviewSelected.size(); i++) {
            showTextMask(mTextviewSelected.get(i), i + 1);
        }
    }

    public void showTextMask(TextView textView, int index) {
        textView.setText(String.valueOf(index));
        textView.setTextSize(48);
        textView.setGravity(CENTER);
    }

    /**
     * @return List of {@link MediaItem} selected.
     */
    public List<MediaItem> getMediaSelectedList() {
        return mMediaListSelected;
    }

    /**
     * Set list of {@link MediaItem} selected.
     *
     * @param list
     */
    public void setMediaSelectedList(List<MediaItem> list) {
        mMediaListSelected = list;
    }

    /**
     * Whether clear or not media selected as options.
     *
     * @return true if clear, false otherwise.
     */
    private boolean syncMediaSelectedAsOptions() {
        switch (mMediaType) {
            case MediaItem.PHOTO:
                if (!mMediaOptions.canSelectMultiPhoto()) {
                    mMediaListSelected.clear();
                    return true;
                }
                break;
            case MediaItem.VIDEO:
                if (!mMediaOptions.canSelectMultiVideo()) {
                    mMediaListSelected.clear();
                    return true;
                }

                break;
            default:
                break;
        }
        return false;
    }

    /**
     * {@link MediaItem#VIDEO} or {@link MediaItem#PHOTO}
     *
     * @param mediaType
     */
    public void setMediaType(int mediaType) {
        mMediaType = mediaType;
    }

    // set numcols
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    // set photo item height
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams.height = height;
        mImageViewLayoutParams.width = height;
        notifyDataSetChanged();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        PickerImageView imageView = (PickerImageView) view
                .findViewById(R.id.thumbnail);
        mPickerImageViewSelected.remove(imageView);
    }

    public void onDestroyView() {
        mPickerImageViewSelected.clear();
    }
}