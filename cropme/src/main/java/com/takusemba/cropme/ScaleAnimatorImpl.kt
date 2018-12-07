package com.takusemba.cropme

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator

import android.view.View.SCALE_X
import android.view.View.SCALE_Y

/**
 * ScaleAnimatorImpl
 *
 * @author takusemba
 * @since 05/09/2017
 */
internal class ScaleAnimatorImpl(target: View, private val maxScale: Int) : ScaleAnimator {

    private val animatorX: ObjectAnimator
    private val animatorY: ObjectAnimator

    init {

        this.animatorX = ObjectAnimator()
        animatorX.setProperty(SCALE_X)
        animatorX.target = target

        this.animatorY = ObjectAnimator()
        animatorY.setProperty(SCALE_Y)
        animatorY.target = target
    }

    override fun reset() {
        val targetX = animatorX.target as View
        if (targetX != null) {
            animatorX.cancel()
            animatorX.duration = 0
            animatorX.interpolator = null
            animatorX.setFloatValues(1 / targetX.scaleX)
            animatorX.start()
        }

        val targetY = animatorY.target as View
        if (targetY != null) {
            animatorY.cancel()
            animatorY.duration = 0
            animatorY.interpolator = null
            animatorY.setFloatValues(1 / targetY.scaleY)
            animatorY.start()
        }
        reScaleIfNeeded()
    }

    override fun scale(scale: Float): ScaleXY? {
        val targetX = animatorX.target as View
        if (targetX != null) {
            animatorX.cancel()
            animatorX.duration = 0
            animatorX.interpolator = null
            animatorX.setFloatValues(targetX.scaleX * scale)
            animatorX.start()
        }

        val targetY = animatorY.target as View
        if (targetY != null) {
            animatorY.cancel()
            animatorY.duration = 0
            animatorY.interpolator = null
            animatorY.setFloatValues(targetY.scaleY * scale)
            animatorY.start()
        }

        return ScaleXY(targetX.scaleX * scale, targetY.scaleY * scale)
    }

    override fun reScaleIfNeeded(): ScaleXY? {
        val targetX = animatorX.target as View
        var scaleX:Float = targetX.scaleX
        var scaleY:Float = targetX.scaleY

        if (targetX != null) {
            if (targetX.scaleX < 1) {
                animatorX.cancel()
                animatorX.duration = DURATION.toLong()
                animatorX.setFloatValues(1.0f)
                animatorX.interpolator = DecelerateInterpolator(FACTOR.toFloat())
                animatorX.start()
                scaleX = 1.0f
            } else if (maxScale < targetX.scaleX) {
                animatorX.cancel()
                animatorX.duration = DURATION.toLong()
                animatorX.setFloatValues(maxScale.toFloat())
                animatorX.interpolator = DecelerateInterpolator(FACTOR.toFloat())
                animatorX.start()
                scaleX = maxScale.toFloat()
            }
        }

        val targetY = animatorY.target as View
        if (targetY != null) {
            if (targetY.scaleY < 1) {
                animatorY.cancel()
                animatorY.duration = DURATION.toLong()
                animatorY.setFloatValues(1.0f)
                animatorY.interpolator = DecelerateInterpolator(FACTOR.toFloat())
                animatorY.start()
                scaleY = 1.0f
            } else if (maxScale < targetY.scaleY) {
                animatorY.cancel()
                animatorY.duration = DURATION.toLong()
                animatorY.setFloatValues(maxScale.toFloat())
                animatorY.interpolator = DecelerateInterpolator(FACTOR.toFloat())
                animatorY.start()
                scaleY = maxScale.toFloat()
            }
        }
        return ScaleXY(scaleX,scaleY)
    }

    companion object {

        private val DURATION = 600
        private val FACTOR = 2
    }
}
