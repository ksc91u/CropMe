package com.takusemba.cropme

import android.graphics.PointF
import android.net.Uri

interface OnCropChangeListener{
    fun onCropChange(uri: Uri, scaleXY: ScaleXY?, point: PointF?)
}