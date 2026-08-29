package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SpreadsheetStudioScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleekNavBackgroundLight
import com.example.ui.theme.SleekOutlineLight
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val extractionProgress by viewModel.extractionProgress.collectAsState()
    val activeDoc by viewModel.activeDocument.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "App Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AI PDF Extract",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PDF to Excel Studio",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sleek Profile / Mode Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.CONVERT,
                        onClick = { viewModel.setTab(AppTab.CONVERT) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.CONVERT) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Convert"
                            )
                        },
                        label = { Text("Convert", fontWeight = if (currentTab == AppTab.CONVERT) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_convert")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.STUDIO,
                        onClick = { viewModel.setTab(AppTab.STUDIO) },
                        colors = navItemColors,
                        icon = {
                            if (activeDoc != null) {
                                BadgedBox(badge = { Badge { Text("${activeDoc!!.totalRows}") } }) {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.STUDIO) Icons.Filled.TableChart else Icons.Outlined.TableChart,
                                        contentDescription = "Spreadsheet"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (currentTab == AppTab.STUDIO) Icons.Filled.TableChart else Icons.Outlined.TableChart,
                                    contentDescription = "Spreadsheet"
                                )
                            }
                        },
                        label = { Text("Sheet", fontWeight = if (currentTab == AppTab.STUDIO) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_studio")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.HISTORY,
                        onClick = { viewModel.setTab(AppTab.HISTORY) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "History"
                            )
                        },
                        label = { Text("History", fontWeight = if (currentTab == AppTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_history")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.TEMPLATES,
                        onClick = { viewModel.setTab(AppTab.TEMPLATES) },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.TEMPLATES) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Schemas"
                            )
                        },
                        label = { Text("Schemas", fontWeight = if (currentTab == AppTab.TEMPLATES) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_templates")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.CONVERT -> ConvertScreen(viewModel = viewModel)
                AppTab.STUDIO -> SpreadsheetStudioScreen(viewModel = viewModel)
                AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                AppTab.TEMPLATES -> TemplatesScreen(viewModel = viewModel)
            }
        }
    }

    // Active extraction modal dialog
    if (isProcessing && extractionProgress != null) {
        ProcessingProgressDialog(
            progress = extractionProgress!!,
            onCancel = { viewModel.cancelExtraction() }
        )
    }
}


