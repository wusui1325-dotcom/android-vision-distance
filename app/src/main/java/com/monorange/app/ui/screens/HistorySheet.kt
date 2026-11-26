package com.monorange.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.monorange.app.engine.DistanceCalculator
import com.monorange.app.model.DistanceUnit
import com.monorange.app.model.MeasurementMode
import com.monorange.app.model.MeasurementRecord
import com.monorange.app.ui.components.StreamlinedCircleIconButton
import com.monorange.app.ui.components.StreamlinedItemRow
import com.monorange.app.ui.components.StreamlinedPillButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    records: List<MeasurementRecord>,
    unit: DistanceUnit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val dateFmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "測距紀錄",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "測距紀錄 (${records.size})",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                if (records.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StreamlinedPillButton(
                            icon = Icons.Rounded.Share,
                            label = "匯出",
                            height = 36.dp,
                            onClick = {
                                val text = records.joinToString("\n") { rec ->
                                    val (dVal, dUnit) = DistanceCalculator.formatDistance(rec.distanceMeters, unit)
                                    val hPart = rec.heightMeters?.let { h ->
                                        val (hVal, hUnit) = DistanceCalculator.formatDistance(h, unit)
                                        " | 高度 $hVal $hUnit"
                                    }.orEmpty()
                                    "[${dateFmt.format(Date(rec.timestampMs))}] ${rec.mode.label}: $dVal $dUnit$hPart (${rec.note})"
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "匯出 MonoRange 測距紀錄"))
                            }
                        )
                        StreamlinedCircleIconButton(
                            icon = Icons.Rounded.DeleteSweep,
                            contentDescription = "清除全部",
                            size = 36.dp,
                            iconSize = 18.dp,
                            onClick = onClearAll
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (records.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "尚無儲存的測距紀錄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(records, key = { it.id }) { rec ->
                        val (dVal, dUnit) = DistanceCalculator.formatDistance(rec.distanceMeters, unit)
                        val heightSuffix = rec.heightMeters?.let { h ->
                            val (hVal, hUnit) = DistanceCalculator.formatDistance(h, unit)
                            "  •  高 $hVal $hUnit"
                        }.orEmpty()

                        val modeIcon = when (rec.mode) {
                            MeasurementMode.GROUND_PITCH -> Icons.Rounded.Terrain
                            MeasurementMode.OBJECT_REF -> Icons.Rounded.AspectRatio
                            MeasurementMode.HEIGHT_MEASURE -> Icons.Rounded.Height
                        }

                        StreamlinedItemRow(
                            icon = modeIcon,
                            title = "$dVal $dUnit$heightSuffix",
                            subtitle = "${rec.mode.label} · ${rec.note} · ${dateFmt.format(Date(rec.timestampMs))}",
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StreamlinedCircleIconButton(
                                        icon = Icons.Rounded.ContentCopy,
                                        contentDescription = "複製數值",
                                        size = 34.dp,
                                        iconSize = 16.dp,
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("MonoRange", "$dVal $dUnit$heightSuffix"))
                                        }
                                    )
                                    StreamlinedCircleIconButton(
                                        icon = Icons.Rounded.DeleteOutline,
                                        contentDescription = "刪除紀錄",
                                        size = 34.dp,
                                        iconSize = 16.dp,
                                        onClick = { onDelete(rec.id) }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
