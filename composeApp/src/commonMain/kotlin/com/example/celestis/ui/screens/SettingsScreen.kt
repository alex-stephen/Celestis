package com.example.celestis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.celestis.legal.LegalDocument
import com.example.celestis.ui.navigation.ApodTopAppBar
import com.example.celestis.ui.navigation.TopBarState
import com.example.celestis.ui.navigation.apodNavigationOverlayWidth
import com.example.celestis.ui.navigation.apodTopAppBarContentHeight
import com.example.celestis.ui.viewModels.SettingsUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

private enum class SettingsDialog {
    ClearCache
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    windowSizeClass: WindowSizeClass,
    hazeState: HazeState,
    topBarState: TopBarState,
    onPrivacyPolicyClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onTermsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onReportBugClick: () -> Unit,
    onLeaveReviewClick: () -> Unit,
    onShareAppClick: () -> Unit
) {
    var visibleDialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var selectedDocument by remember { mutableStateOf<LegalDocument?>(null) }
    val navigationOverlayWidth = apodNavigationOverlayWidth(windowSizeClass)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarContentHeight = apodTopAppBarContentHeight(windowSizeClass)
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    selectedDocument?.let { document ->
        LegalDocumentScreen(
            document = document,
            windowSizeClass = windowSizeClass,
            hazeState = hazeState,
            navigationOverlayWidth = navigationOverlayWidth,
            onNavigateBack = { selectedDocument = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(start = navigationOverlayWidth)
                .nestedScroll(topBarState.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = statusBarTop + appBarContentHeight + 20.dp,
                bottom = 90.dp + navigationBarBottom,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SettingsHeader()
            }

            item {
                LowDataModeRow(isLowDataMode = state.isLowDataMode)
            }

            item {
                SectionTitle("General")
            }

            items(
                listOf(
                    SettingsRowData(Icons.Default.PrivacyTip, "Privacy policy") {
                        onPrivacyPolicyClick()
                        selectedDocument = LegalDocument.PrivacyPolicy
                    },
                    SettingsRowData(Icons.Default.DeleteSweep, "Clear cache") {
                        visibleDialog = SettingsDialog.ClearCache
                    },
                    SettingsRowData(Icons.AutoMirrored.Filled.Article, "Terms of use") {
                        onTermsClick()
                        selectedDocument = LegalDocument.TermsOfUse
                    },
                    SettingsRowData(Icons.Default.Notifications, "Notifications", onNotificationsClick)
                )
            ) { row ->
                SettingsRow(row, trailing = {
                    if (row.title == "Clear cache" && state.isClearingCache) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                })
            }

            state.cacheMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                    )
                }
            }

            item {
                SectionTitle("Feedback")
            }

            items(
                listOf(
                    SettingsRowData(Icons.Default.BugReport, "Report a bug", onReportBugClick),
                    SettingsRowData(Icons.Default.RateReview, "Leave review", onLeaveReviewClick),
                    SettingsRowData(Icons.Default.Share, "Share this app", onShareAppClick)
                )
            ) { row ->
                SettingsRow(row) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        AnimatedVisibility(
            visible = topBarState.isVisible,
            enter = androidx.compose.animation.slideInVertically { -it },
            exit = androidx.compose.animation.slideOutVertically { -it }
        ) {
            ApodTopAppBar(
                modifier = Modifier.padding(start = navigationOverlayWidth),
                titleContent = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SETTINGS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                },
                hazeState = hazeState,
                windowSizeClass = windowSizeClass
            )
        }
    }

    visibleDialog?.let { dialog ->
        when (dialog) {
            SettingsDialog.ClearCache -> ClearCacheDialog(
                onDismiss = { visibleDialog = null },
                onConfirm = {
                    visibleDialog = null
                    onClearCacheClick()
                }
            )
        }
    }
}

@Composable
private fun LegalDocumentScreen(
    document: LegalDocument,
    windowSizeClass: WindowSizeClass,
    hazeState: HazeState,
    navigationOverlayWidth: androidx.compose.ui.unit.Dp,
    onNavigateBack: () -> Unit
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarContentHeight = apodTopAppBarContentHeight(windowSizeClass)
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(start = navigationOverlayWidth),
            contentPadding = PaddingValues(
                top = statusBarTop + appBarContentHeight + 20.dp,
                bottom = 40.dp + navigationBarBottom,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = document.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
                )
            }
        }

        ApodTopAppBar(
            modifier = Modifier.padding(start = navigationOverlayWidth),
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            titleContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = document.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            },
            hazeState = hazeState,
            windowSizeClass = windowSizeClass
        )
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = "Celestis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Astronomy Picture of the Day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun LowDataModeRow(isLowDataMode: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Low data mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = isLowDataMode,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

private data class SettingsRowData(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsRow(
    row: SettingsRowData,
    trailing: @Composable () -> Unit
) {
    Surface(
        onClick = row.onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = row.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
                )
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier.padding(start = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear")
            }
        },
        title = { Text("Clear cache?") },
        text = { Text("This removes cached APOD items that are not in favourites.") }
    )
}
