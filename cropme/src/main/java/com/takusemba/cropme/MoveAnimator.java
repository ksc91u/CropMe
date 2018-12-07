package com.takusemba.cropme;

import android.support.animation.SpringForce;

/**
 * MoveAnimator
 *
 * @author takusemba
 * @since 05/09/2017
 **/
interface MoveAnimator {

    /**
     * stiffness when flinging or bouncing
     **/
    float STIFFNESS = SpringForce.STIFFNESS_VERY_LOW;

    /**
     * dumping ratio when flinging or bouncing
     **/
    float DAMPING_RATIO = SpringForce.DAMPING_RATIO_NO_BOUNCY;

    /**
     * friction when flinging
     **/
    float FRICTION = 3f;

    /**
     * move image
     *
     * @param delta distance of how much image moves
     **/
    Float move(float delta);

    /**
     * bounce image when image is off of {@link CropOverlayView#resultRect}
     *
     * @param velocity velocity when starting to move
     **/
    Float reMoveIfNeeded(float velocity);

    /**
     * fling image
     *
     * @param velocity velocity when starting to fling
     **/
    void fling(float velocity);

    /**
     * true if image is flinging, false otherwise
     **/
    boolean isNotFlinging();

    void moveTo(float value);
}
