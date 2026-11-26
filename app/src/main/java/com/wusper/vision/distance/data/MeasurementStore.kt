package com.wusper.vision.distance.data

import android.content.Context
import com.wusper.vision.distance.model.DistanceUnit
import com.wusper.vision.distance.model.MeasurementMode
import com.wusper.vision.distance.model.MeasurementRecord
import org.json.JSONArray
import org.json.JSONObject

class MeasurementStore(context: Context) {

    private val prefs = context.getSharedPreferences("monorange_prefs", Context.MODE_PRIVATE)

    fun loadCameraHeight(): Float = prefs.getFloat(KEY_CAMERA_HEIGHT, 1.50f)
    fun saveCameraHeight(value: Float) = prefs.edit().putFloat(KEY_CAMERA_HEIGHT, value).apply()

    fun loadFocalFactor(): Float = prefs.getFloat(KEY_FOCAL_FACTOR, 1.00f)
    fun saveFocalFactor(value: Float) = prefs.edit().putFloat(KEY_FOCAL_FACTOR, value).apply()

    fun loadPitchOffset(): Float = prefs.getFloat(KEY_PITCH_OFFSET, 0.0f)
    fun savePitchOffset(value: Float) = prefs.edit().putFloat(KEY_PITCH_OFFSET, value).apply()

    fun loadSmoothing(): Float = prefs.getFloat(KEY_SMOOTHING, 0.18f)
    fun saveSmoothing(value: Float) = prefs.edit().putFloat(KEY_SMOOTHING, value).apply()

    fun loadUnit(): DistanceUnit {
        val raw = prefs.getString(KEY_UNIT, DistanceUnit.METERS.name)
        return DistanceUnit.entries.firstOrNull { it.name == raw } ?: DistanceUnit.METERS
    }

    fun saveUnit(unit: DistanceUnit) = prefs.edit().putString(KEY_UNIT, unit.name).apply()

    fun loadDarkMono(): Boolean = prefs.getBoolean(KEY_DARK_MONO, true)
    fun saveDarkMono(dark: Boolean) = prefs.edit().putBoolean(KEY_DARK_MONO, dark).apply()

    fun loadHaptics(): Boolean = prefs.getBoolean(KEY_HAPTICS, true)
    fun saveHaptics(enabled: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()

    fun loadHistory(): List<MeasurementRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val mode = MeasurementMode.entries.firstOrNull {
                        it.name == obj.optString("mode")
                    } ?: MeasurementMode.GROUND_PITCH
                    add(
                        MeasurementRecord(
                            id = obj.optLong("id"),
                            timestampMs = obj.optLong("ts"),
                            mode = mode,
                            distanceMeters = obj.optDouble("dist", 0.0).toFloat(),
                            heightMeters = if (obj.has("height") && !obj.isNull("height")) {
                                obj.optDouble("height").toFloat()
                            } else null,
                            pitchDeg = obj.optDouble("pitch", 0.0).toFloat(),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHistory(records: List<MeasurementRecord>) {
        val arr = JSONArray()
        records.take(100).forEach { rec ->
            val obj = JSONObject().apply {
                put("id", rec.id)
                put("ts", rec.timestampMs)
                put("mode", rec.mode.name)
                put("dist", rec.distanceMeters.toDouble())
                if (rec.heightMeters != null) {
                    put("height", rec.heightMeters.toDouble())
                }
                put("pitch", rec.pitchDeg.toDouble())
                put("note", rec.note)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val KEY_CAMERA_HEIGHT = "camera_height"
        private const val KEY_FOCAL_FACTOR = "focal_factor"
        private const val KEY_PITCH_OFFSET = "pitch_offset"
        private const val KEY_SMOOTHING = "smoothing_alpha"
        private const val KEY_UNIT = "unit"
        private const val KEY_DARK_MONO = "dark_mono"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_HISTORY = "history_json"
    }
}
