package com.ksyun.media.kmcshortvideo.demo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by sujia on 2017/7/4.
 */

public class VideoConfig implements Parcelable {
    int index;
    String filePath;

    long trimIn;
    long trimOut;
    double speed;
    double bright;
    double saturation;
    double contrast;
    double rotate;
    double scale;

    protected VideoConfig(Parcel in) {
        index = in.readInt();
        filePath = in.readString();
        trimIn = in.readLong();
        trimOut = in.readLong();
        speed = in.readDouble();
        bright = in.readDouble();
        saturation = in.readDouble();
        contrast = in.readDouble();
        rotate = in.readDouble();
        scale = in.readDouble();
    }

    public static final Creator<VideoConfig> CREATOR = new Creator<VideoConfig>() {
        @Override
        public VideoConfig createFromParcel(Parcel in) {
            return new VideoConfig(in);
        }

        @Override
        public VideoConfig[] newArray(int size) {
            return new VideoConfig[size];
        }
    };

    public VideoConfig() {
        trimIn = 0;
        trimOut = 0;

        speed = 1.0;

        bright = 1.0;
        contrast = 1.0;
        saturation = 1.0;

        scale = 1.0;
        rotate = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);

        if (filePath == null) {
            dest.writeString(null);
        } else {
            dest.writeString(filePath);
        }

        dest.writeLong(trimIn);
        dest.writeLong(trimOut);

        dest.writeDouble(speed);

        dest.writeDouble(bright);
        dest.writeDouble(saturation);
        dest.writeDouble(contrast);

        dest.writeDouble(rotate);
        dest.writeDouble(scale);
    }
}
