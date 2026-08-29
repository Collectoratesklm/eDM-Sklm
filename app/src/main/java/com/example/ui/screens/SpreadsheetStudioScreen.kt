package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpreadsheetDocument
import com.example.service.SpreadsheetExporter
import com.example.ui.components.PdfViewerModal
import com.example.ui.components.SpreadsheetGrid
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SpreadsheetStudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeDoc by viewModel.activeDocument.collectAsState()
    val activeSheetIndex by viewModel.activeSheetIndex.collectAsState()
    val renderedBitmaps by viewModel.renderedBitmaps.collectAsState()

    var showPdfViewerModal by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showAddSheetDialog by remember { mutableStateOf(false) }

    if (activeDoc == null) {
        // Empty Studio State
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "No Workbook",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "No Active Spreadsheet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select a PDF and run AI conversion to start editing and exporting your Excel workbook.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = { viewModel.setTab(AppTab.CONVERT) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to Converter")
                }
            }
        }
        return
    }

    val doc = activeDoc!!
    val currentSheet = doc.sheets.getOrNull(activeSheetIndex) ?: doc.sheets.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        // Top Workbook Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = doc.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // AI Confidence Pill
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "${(doc.confidenceScore * 100).toInt()}% AI",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${doc.totalRows} rows • ${doc.totalSheets} sheet(s) • ${doc.presetUsed.displayName}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(doc) },
                            modifier = Modifier.testTag("toggle_fav_btn")
                        ) {
                            Icon(
                                imageVector = if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (doc.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (renderedBitmaps.isNotEmpty()) {
                            IconButton(
                                onClick = { showPdfViewerModal = true },
                                modifier = Modifier.testTag("view_source_pdf_btn")
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "View Source PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showExportMenu = true },
                                modifier = Modifier.testTag("export_menu_btn")
                            ) {
                                Icon(
                                    Icons.Default.IosShare,
                                    contentDescription = "Export Options",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export to Excel (.xls)") },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.exportActiveDocument(SpreadsheetExporter.ExportFormat.EXCEL_XML)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to CSV (.csv)") },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.exportActiveDocument(SpreadsheetExporter.ExportFormat.CSV)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy TSV for Google Sheets") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.copyCurrentSheetToClipboard(asTsv = true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Interactive HTML (.html)") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.exportActiveDocument(SpreadsheetExporter.ExportFormat.HTML)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Export Toolbar Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.exportActiveDocument(SpreadsheetExporter.ExportFormat.EXCEL_XML) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_export_excel_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel (.xls)", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportActiveDocument(SpreadsheetExporter.ExportFormat.CSV) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_export_csv_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("CSV", fontSize = 11.5.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.copyCurrentSheetToClipboard(asTsv = true) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_copy_tsv_btn"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Table", fontSize = 11.5.sp)
                    }
                }
            }
        }

        // Sheet Tabs Bar
        if (doc.sheets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeSheetIndex.coerceIn(0, doc.sheets.lastIndex),
                    edgePadding = 12.dp,
                    modifier = Modifier.weight(1f),
                    divider = {}
                ) {
                    doc.sheets.forEachIndexed { index, sheet ->
                        Tab(
                            selected = activeSheetIndex == index,
                            onClick = { viewModel.setSelectedSheetIndex(index) },
                            text = {
                                Text(
                                    text = "${sheet.name} (${sheet.rowCount})",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeSheetIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("sheet_tab_$index")
                        )
                    }
                }

                IconButton(
                    onClick = { showAddSheetDialog = true },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("add_sheet_btn")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Sheet",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Core Spreadsheet Grid Component
        if (currentSheet != null) {
            SpreadsheetGrid(
                sheet = currentSheet,
                onUpdateSheet = { updated ->
                    viewModel.updateActiveSheet(updated)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }

    // PDF Source Modal
    if (showPdfViewerModal) {
        PdfViewerModal(
            pageBitmaps = renderedBitmaps,
            documentName = doc.fileName,
            onDismiss = { showPdfViewerModal = false }
        )
    }

    // Add Sheet Dialog
    if (showAddSheetDialog) {
        var newSheetName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSheetDialog = false },
            title = { Text("Add Worksheet Tab", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSheetName,
                    onValueChange = { newSheetName = it },
                    label = { Text("Sheet Name (e.g. Taxes, Notes, Summary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newSheetName.ifBlank { "Sheet ${doc.sheets.size + 1}" }
                        viewModel.addNewSheet(name)
                        showAddSheetDialog = false
                    }
                ) {
                    Text("Create Sheet")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSheetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
