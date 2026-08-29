package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CellEditDialog(
    initialValue: String,
    columnName: String,
    rowIndex: Int,
    columnIndex: Int,
    onDismiss: () -> Unit,
    onSave: (newValue: String) -> Unit,
    onDeleteRow: (() -> Unit)? = null
) {
    var cellText by remember { mutableStateOf(initialValue) }
    val columnLetter = ('A'.code + (columnIndex % 26)).toChar().toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Cell [$columnLetter${rowIndex + 1}]",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Column: $columnName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDeleteRow != null) {
                    IconButton(
                        onClick = {
                            onDeleteRow()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_row_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Row",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = cellText,
                    onValueChange = { cellText = it },
                    label = { Text("Cell Value") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cell_edit_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Format Helper Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickFormatterChip(label = "$ USD") {
                        val num = cellText.replace("$", "").trim()
                        cellText = "$$num"
                    }
                    QuickFormatterChip(label = "0.00") {
                        val clean = cellText.replace("$", "").replace(",", "").trim()
                        clean.toDoubleOrNull()?.let {
                            cellText = String.format(java.util.Locale.US, "%.2f", it)
                        }
                    }
                    QuickFormatterChip(label = "%") {
                        val clean = cellText.replace("%", "").trim()
                        cellText = "$clean%"
                    }
                    QuickFormatterChip(label = "Clear") {
                        cellText = ""
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(cellText)
                    onDismiss()
                },
                modifier = Modifier.testTag("cell_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QuickFormatterChip(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
