package com.ksyun.media.kmcshortvideo.demo;

import android.graphics.Bitmap;

import com.ksyun.media.kmcshortvideo.KMCMaterial;

/**
 * Created by sensetime on 16-7-6.
 */
public class MaterialInfoItem {
    public Bitmap thumbnail;
    public KMCMaterial material;
    public boolean hasDownload = false;

    public MaterialInfoItem(KMCMaterial material, Bitmap thumbnail) {
        this.material = material;
        this.thumbnail = thumbnail;
    }

    public void setHasDownload(boolean isDownloaded){
        this.hasDownload = isDownloaded;
    }
}
