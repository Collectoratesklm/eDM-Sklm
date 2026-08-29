package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColumnType
import com.example.data.model.SpreadsheetSheet
import java.text.DecimalFormat

@Composable
fun ColumnStatsBar(
    sheet: SpreadsheetSheet,
    selectedColIndex: Int?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculate stats for selected column or find the first numeric/currency column
    val targetCol = selectedColIndex ?: sheet.columnTypes.indexOfFirst {
        it == ColumnType.NUMBER || it == ColumnType.CURRENCY || it == ColumnType.PERCENTAGE
    }.takeIf { it >= 0 } ?: 0

    if (targetCol < 0 || targetCol >= sheet.headers.size) return

    val colName = sheet.headers.getOrElse(targetCol) { "Col ${targetCol + 1}" }
    val colType = sheet.columnTypes.getOrElse(targetCol) { ColumnType.TEXT }
    val cellValues = sheet.rows.mapNotNull { it.getOrNull(targetCol)?.trim() }.filter { it.isNotBlank() }

    val numericValues = cellValues.mapNotNull { raw ->
        val clean = raw.replace("$", "").replace("€", "").replace("£", "").replace(",", "").replace("%", "")
        clean.toDoubleOrNull()
    }

    val isNumeric = numericValues.isNotEmpty() && (colType == ColumnType.NUMBER || colType == ColumnType.CURRENCY || colType == ColumnType.PERCENTAGE || numericValues.size >= cellValues.size / 2)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("column_stats_bar"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "Column Stats",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = colName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            StatPill(label = "Count", value = "${cellValues.size} rows")

            if (isNumeric && numericValues.isNotEmpty()) {
                val sum = numericValues.sum()
                val avg = numericValues.average()
                val min = numericValues.minOrNull() ?: 0.0
                val max = numericValues.maxOrNull() ?: 0.0
                val isCurrency = colType == ColumnType.CURRENCY || cellValues.any { it.contains("$") }

                val df = DecimalFormat(if (isCurrency) "$#,##0.00" else "#,##0.##")

                StatPill(label = "SUM", value = df.format(sum), isPrimary = true)
                StatPill(label = "AVG", value = df.format(avg))
                StatPill(label = "MIN", value = df.format(min))
                StatPill(label = "MAX", value = df.format(max))
            } else {
                StatPill(label = "Type", value = colType.label)
                StatPill(label = "Distinct", value = "${cellValues.distinct().size}")
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    isPrimary: Boolean = false
) {
    Surface(
        color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
