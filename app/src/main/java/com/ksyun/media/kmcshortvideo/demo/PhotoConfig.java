package com.ksyun.media.kmcshortvideo.demo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sujia on 2017/7/5.
 */

class PhotoConfig implements Parcelable {
    int index;
    String filePath;

    long trimOut;
    double bright;
    double saturation;
    double contrast;
    double rotate;

    protected PhotoConfig(Parcel in) {
        index = in.readInt();
        filePath = in.readString();
        trimOut = in.readLong();
        bright = in.readDouble();
        saturation = in.readDouble();
        contrast = in.readDouble();
        rotate = in.readDouble();
    }

    public PhotoConfig() {
        trimOut = 0;

        bright = 1.0;
        contrast = 1.0;
        saturation = 1.0;

        rotate = 0;
    }

    public static final Creator<PhotoConfig> CREATOR = new Creator<PhotoConfig>() {
        @Override
        public PhotoConfig createFromParcel(Parcel in) {
            return new PhotoConfig(in);
        }

        @Override
        public PhotoConfig[] newArray(int size) {
            return new PhotoConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeString(filePath);
        dest.writeLong(trimOut);
        dest.writeDouble(bright);
        dest.writeDouble(saturation);
        dest.writeDouble(contrast);
        dest.writeDouble(rotate);
    }
}
