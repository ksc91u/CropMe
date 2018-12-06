package com.takusemba.cropme;

/**
 * ScaleAnimator
 *
 * @author takusemba
 * @since 05/09/2017
 **/
interface ScaleAnimator {

    /**
     * scale image
     *
     * @param scale how much image scales
     **/
    ScaleXY scale(float scale);

    /**
     * rescale image when image is too much big or small
     **/
    ScaleXY reScaleIfNeeded();
}
