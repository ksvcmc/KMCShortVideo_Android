package com.ksyun.media.kmcshortvideo.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaItem;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.imageloader.MediaImageLoader;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.imageloader.MediaImageLoaderImpl;

import java.util.List;

/**
 * Created by sujia on 2017/7/1.
 */

public class MediaListViewAdapter extends RecyclerView.Adapter {
    private static final String TAG = MediaListViewAdapter.class.getSimpleName();
    private List<MediaItem> mMediaItemList;
    private Context mContext;
    private MediaImageLoader mMediaImageLoader;
    private LayoutInflater mInflater;
    Bitmap iconBitmap;
    private int selectIndex = -1;
    private RecyclerView mRecyclerView;

    public static int STATE_INIT = 0;
    public static int STATE_DOWNLOADING = 1;
    public static int STATE_DOWNLOADED = 2;
    public static int STATE_COOLDOWNING = 3;
    public static int STATE_STICKER_READY = 4;
    public static int STATE_DOWNLOAD_THUMBNAIL = 5;

    public interface OnRecyclerViewListener {
        void onItemClick(int position);
        boolean onItemLongClick(int position);
        void onDeleteClick(int position);
    }
    private OnRecyclerViewListener onRecyclerViewListener;
    public void setOnItemClickListener(OnRecyclerViewListener onRecyclerViewListener) {
        this.onRecyclerViewListener = onRecyclerViewListener;
    }

    public MediaListViewAdapter(List<MediaItem> mediaList, Context context) {
        this.mContext = context;
        this.mMediaItemList = mediaList;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int count = getItemCount();
        mMediaImageLoader = new MediaImageLoaderImpl(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_edit_medialist_item, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(lp);
        return new MediaListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        final MediaListViewHolder holder = (MediaListViewHolder)viewHolder;
        holder.position = position;

        if (mMediaItemList.size() == 0 &&
                position == 0) {
            holder.mAddButton.setVisibility(View.VISIBLE);
            holder.mThumb.setVisibility(View.GONE);
            holder.mDeleteButton.setVisibility(View.GONE);
            holder.mEditButton.setVisibility(View.GONE);

            return;
        }

        if ((position % 2) == 0) {//偶数
            holder.mAddButton.setVisibility(View.VISIBLE);
            holder.mThumb.setVisibility(View.GONE);
            holder.mDeleteButton.setVisibility(View.GONE);
            holder.mEditButton.setVisibility(View.GONE);
        } else {
            holder.mAddButton.setVisibility(View.GONE);
            holder.mThumb.setVisibility(View.VISIBLE);
            holder.mDeleteButton.setVisibility(View.VISIBLE);
            holder.mEditButton.setVisibility(View.VISIBLE);
        }

        int mediaIndex = (position - 1 )/2;
        MediaItem item = mMediaItemList.get(mediaIndex);
        if (item.isPhoto()) {
            holder.mEditButton.setImageResource(R.drawable.edit_photo);
        } else if (item.isVideo()) {
            holder.mEditButton.setImageResource(R.drawable.edit_video);
        }

        mMediaImageLoader.displayImage(item.getUriOrigin(), holder.mThumb);
    }

    @Override
    public int getItemCount() {
        if(mMediaItemList == null){
            return 0;
        }
        return (mMediaItemList.size() * 2) + 1;
    }

    public void setRecyclerView(RecyclerView view) {
        this.mRecyclerView = view;
    }

    class MediaListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        private ImageView mAddButton;
        private ImageView mThumb;
        private ImageView mDeleteButton;
        private ImageView mEditButton;
        private int position;

        public MediaListViewHolder(View itemView) {
            super(itemView);
            this.mAddButton = (ImageView) itemView.findViewById(R.id.edit_add_button);
            this.mThumb = (ImageView) itemView.findViewById(R.id.edit_thumb);
            this.mDeleteButton = (ImageView) itemView.findViewById(R.id.edit_delete_button);
            this.mEditButton = (ImageView) itemView.findViewById(R.id.edit_button);

            mAddButton.setOnClickListener(this);
            mDeleteButton.setOnClickListener(this);
            mEditButton.setOnClickListener(this);
            mAddButton.setOnLongClickListener(this);
            mDeleteButton.setOnLongClickListener(this);
            mEditButton.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.edit_delete_button) {
                if (null != onRecyclerViewListener) {
                    onRecyclerViewListener.onDeleteClick(position);
                }
            } else {
                if (null != onRecyclerViewListener) {
                    onRecyclerViewListener.onItemClick(position);
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if(v.getId() != R.id.edit_delete_button &&
                    null != onRecyclerViewListener){
                return onRecyclerViewListener.onItemLongClick(position);
            }
            return false;
        }
    }

    public void setSelectIndex(int i){
        selectIndex = i;
        notifyDataSetChanged();
    }
}
