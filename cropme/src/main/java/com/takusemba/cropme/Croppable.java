package com.takusemba.cropme;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Croppable
 *
 * @author takusemba
 * @since 05/09/2017
 **/
interface Croppable {

    /**
     * setUri to {@link CropImageView}
     **/
    void setUri(Uri uri);

    void setUri(Uri uri, ScaleXY scale);

    /**
     * setBitmap to {@link CropImageView}
     **/
    void setBitmap(Bitmap bitmap);

    /**
     * crop image. fails if image is outside of {@link CropOverlayView#resultRect}
     **/
    void crop(OnCropListener listener);
}
