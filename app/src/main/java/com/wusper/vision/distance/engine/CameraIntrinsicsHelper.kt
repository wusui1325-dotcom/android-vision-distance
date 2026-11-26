package com.wusper.vision.distance.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.wusper.vision.distance.model.CameraIntrinsics
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min

object CameraIntrinsicsHelper {

    fun readBackCameraIntrinsics(context: Context): CameraIntrinsics {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (cameraId in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

                if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null) {
                    val focal = focalLengths[0]
                    val longEdge = max(sensorSize.width, sensorSize.height)
                    val shortEdge = min(sensorSize.width, sensorSize.height)

                    val vFov = (2.0 * Math.toDegrees(atan((longEdge / (2.0 * focal))))).toFloat()
                    val hFov = (2.0 * Math.toDegrees(atan((shortEdge / (2.0 * focal))))).toFloat()

                    return CameraIntrinsics(
                        focalLengthMm = focal,
                        sensorHeightMm = longEdge,
                        sensorWidthMm = shortEdge,
                        verticalFovDeg = vFov.coerceIn(30f, 120f),
                        horizontalFovDeg = hFov.coerceIn(24f, 110f)
                    )
                }
            }
            CameraIntrinsics()
        } catch (_: Exception) {
            CameraIntrinsics()
        }
    }
}
