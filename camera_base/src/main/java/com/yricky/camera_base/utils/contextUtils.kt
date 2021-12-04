package com.yricky.camera_base.utils

import android.content.Context
import android.hardware.camera2.CameraManager

/**
 * @author Yricky
 * @date 2021/12/4
 */

fun Context.getCameraManager():CameraManager{
    return getSystemService(Context.CAMERA_SERVICE) as CameraManager
}