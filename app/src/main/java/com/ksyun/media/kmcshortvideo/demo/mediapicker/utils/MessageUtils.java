package com.ksyun.media.kmcshortvideo.demo.mediapicker.utils;

import android.content.Context;

import com.ksyun.media.kmcshortvideo.demo.R;
import com.ksyun.media.kmcshortvideo.demo.mediapicker.MediaOptions;

/**
 * @author TUNGDX
 */

/**
 * Get warning, error message for media picker module.
 */
public class MessageUtils {
    /**
     * @param context
     * @param maxDuration in seconds.
     * @return message before record video.
     */
    public static String getWarningMessageVideoDuration(Context context,
                                                        int maxDuration) {
        return context.getResources().getQuantityString(
                R.plurals.picker_video_duration_warning, maxDuration, maxDuration);
    }

    /**
     * @param context
     * @param maxDuration
     * @return message when record and select video that has duration larger
     * than max options.
     * {@link MediaOptions.Builder#setMaxVideoDuration(int)}
     */
    public static String getInvalidMessageMaxVideoDuration(Context context,
                                                           int maxDuration) {
        return context.getResources().getQuantityString(
                R.plurals.picker_video_duration_max, maxDuration, maxDuration);
    }

    /**
     * @param context
     * @param minDuration
     * @return message when record and select video that has duration smaller
     * than min options.
     * {@link MediaOptions.Builder#setMinVideoDuration(int)}
     */
    public static String getInvalidMessageMinVideoDuration(Context context,
                                                           int minDuration) {
        return context.getResources().getQuantityString(
                R.plurals.picker_video_duration_min, minDuration, minDuration);
    }
}