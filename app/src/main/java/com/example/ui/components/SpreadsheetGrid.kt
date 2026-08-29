package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColumnType
import com.example.data.model.SpreadsheetSheet
import com.example.ui.theme.GridCellSelected
import com.example.ui.theme.GridHeaderBgLight
import com.example.ui.theme.GridLineColor

@Composable
fun SpreadsheetGrid(
    sheet: SpreadsheetSheet,
    onUpdateSheet: (SpreadsheetSheet) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) } // rowIndex, colIndex
    var selectedColIndex by remember { mutableStateOf<Int?>(null) }
    var editingCell by remember { mutableStateOf<Triple<Int, Int, String>?>(null) } // rowIndex, colIndex, currentValue
    var renamingColIndex by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var isAddingColumn by remember { mutableStateOf(false) }

    // Column widths map or default width
    val defaultColWidth = 140.dp
    val rowNumberWidth = 44.dp

    val hScrollState = rememberScrollState()

    // Filter rows by search query
    val filteredRowIndices = remember(sheet.rows, searchQuery) {
        if (searchQuery.isBlank()) {
            sheet.rows.indices.toList()
        } else {
            sheet.rows.indices.filter { rIndex ->
                sheet.rows[rIndex].any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search and Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter table rows...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("table_search_input")
            )

            // Add Column Button
            OutlinedButton(
                onClick = { isAddingColumn = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_col_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Column", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Column", fontSize = 12.sp)
            }
        }

        // Live Column Statistics Pill Bar
        ColumnStatsBar(
            sheet = sheet,
            selectedColIndex = selectedColIndex,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Spreadsheet Grid Container
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Horizontal Scrollable Grid Body
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(hScrollState)
                    ) {
                        // Left Fixed Column: Row Numbers
                        Column(
                            modifier = Modifier
                                .width(rowNumberWidth)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            // Top-left corner cell (Intersection)
                            Box(
                                modifier = Modifier
                                    .size(rowNumberWidth, 42.dp)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Row index numbers
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(filteredRowIndices) { _, originalRowIndex ->
                                    val isRowSelected = selectedCell?.first == originalRowIndex
                                    Box(
                                        modifier = Modifier
                                            .size(rowNumberWidth, 40.dp)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            .background(
                                                if (isRowSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${originalRowIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Add Row Button at bottom of row numbers
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(rowNumberWidth, 40.dp)
                                            .clickable {
                                                val newRow = List(sheet.headers.size) { "" }
                                                onUpdateSheet(sheet.copy(rows = sheet.rows + listOf(newRow)))
                                            }
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add Row",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Data Grid Columns
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            // Column Headers Row
                            Row(
                                modifier = Modifier
                                    .height(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            ) {
                                sheet.headers.forEachIndexed { colIndex, headerTitle ->
                                    val colLetter = ('A'.code + (colIndex % 26)).toChar().toString()
                                    val colType = sheet.columnTypes.getOrElse(colIndex) { ColumnType.TEXT }
                                    val isColSelected = selectedColIndex == colIndex

                                    Box(
                                        modifier = Modifier
                                            .width(defaultColWidth)
                                            .height(42.dp)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                            .background(
                                                if (isColSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedColIndex = colIndex
                                                renamingColIndex = Pair(colIndex, headerTitle)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = colLetter,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = colType.label,
                                                    fontSize = 8.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.surface,
                                                            RoundedCornerShape(3.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = headerTitle,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Data Rows
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(filteredRowIndices) { _, originalRowIndex ->
                                    val rowData = sheet.rows[originalRowIndex]

                                    Row(modifier = Modifier.height(40.dp)) {
                                        sheet.headers.indices.forEach { colIndex ->
                                            val cellValue = rowData.getOrElse(colIndex) { "" }
                                            val isSelected = selectedCell?.first == originalRowIndex && selectedCell?.second == colIndex
                                            val colType = sheet.columnTypes.getOrElse(colIndex) { ColumnType.TEXT }
                                            val isNumeric = colType == ColumnType.NUMBER || colType == ColumnType.CURRENCY || colType == ColumnType.PERCENTAGE

                                            Box(
                                                modifier = Modifier
                                                    .width(defaultColWidth)
                                                    .height(40.dp)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    )
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                        else if (originalRowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable {
                                                        selectedCell = Pair(originalRowIndex, colIndex)
                                                        selectedColIndex = colIndex
                                                        editingCell = Triple(originalRowIndex, colIndex, cellValue)
                                                    }
                                                    .padding(horizontal = 8.dp),
                                                contentAlignment = if (isNumeric) Alignment.CenterEnd else Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = cellValue,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (colType == ColumnType.CURRENCY && cellValue.contains("$")) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (colType == ColumnType.CURRENCY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Bottom Row with Add Row Full Width Affordance
                                item {
                                    Row(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clickable {
                                                val newRow = List(sheet.headers.size) { "" }
                                                onUpdateSheet(sheet.copy(rows = sheet.rows + listOf(newRow)))
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add New Row",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ Add new row (${sheet.rowCount + 1})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cell Edit Dialog
    editingCell?.let { (rIndex, cIndex, currentVal) ->
        val colName = sheet.headers.getOrElse(cIndex) { "Column ${cIndex + 1}" }
        CellEditDialog(
            initialValue = currentVal,
            columnName = colName,
            rowIndex = rIndex,
            columnIndex = cIndex,
            onDismiss = { editingCell = null },
            onSave = { newVal ->
                val mutableRows = sheet.rows.map { it.toMutableList() }.toMutableList()
                if (rIndex < mutableRows.size) {
                    val row = mutableRows[rIndex]
                    while (row.size <= cIndex) {
                        row.add("")
                    }
                    row[cIndex] = newVal
                    onUpdateSheet(sheet.copy(rows = mutableRows).withDetectedColumnTypes())
                }
            },
            onDeleteRow = {
                val mutableRows = sheet.rows.toMutableList()
                if (rIndex < mutableRows.size) {
                    mutableRows.removeAt(rIndex)
                    onUpdateSheet(sheet.copy(rows = mutableRows))
                }
            }
        )
    }

    // Rename Column Dialog
    renamingColIndex?.let { (colIdx, currentTitle) ->
        var newTitle by remember { mutableStateOf(currentTitle) }
        var selectedType by remember { mutableStateOf(sheet.columnTypes.getOrElse(colIdx) { ColumnType.TEXT }) }

        AlertDialog(
            onDismissRequest = { renamingColIndex = null },
            title = { Text("Edit Column Header", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Column Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Column Data Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ColumnType.values().take(4).forEach { type ->
                            TextButton(
                                onClick = { selectedType = type },
                                modifier = Modifier
                                    .background(
                                        if (selectedType == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(type.label, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mutableHeaders = sheet.headers.toMutableList()
                        val mutableTypes = sheet.columnTypes.toMutableList()
                        if (colIdx < mutableHeaders.size) {
                            mutableHeaders[colIdx] = newTitle
                        }
                        if (colIdx < mutableTypes.size) {
                            mutableTypes[colIdx] = selectedType
                        }
                        onUpdateSheet(sheet.copy(headers = mutableHeaders, columnTypes = mutableTypes))
                        renamingColIndex = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { renamingColIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Column Dialog
    if (isAddingColumn) {
        var newColTitle by remember { mutableStateOf("") }
        var newColType by remember { mutableStateOf(ColumnType.TEXT) }

        AlertDialog(
            onDismissRequest = { isAddingColumn = false },
            title = { Text("Add New Column", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newColTitle,
                        onValueChange = { newColTitle = it },
                        label = { Text("Column Title (e.g. Tax, Discount, Notes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newColTitle.ifBlank { "Column ${sheet.headers.size + 1}" }
                        val newHeaders = sheet.headers + title
                        val newRows = sheet.rows.map { it + "" }
                        val newTypes = sheet.columnTypes + newColType
                        onUpdateSheet(sheet.copy(headers = newHeaders, rows = newRows, columnTypes = newTypes))
                        isAddingColumn = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isAddingColumn = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
