package com.takusemba.cropme

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri


data class CropInfo(val scaleXY: ScaleXY?, val point: PointF?, val targetRect:Rect, val restriction: RectF?)

interface OnCropChangeListener{
    fun onCropChange(uri: Uri,
                     cropInfo: CropInfo)
}