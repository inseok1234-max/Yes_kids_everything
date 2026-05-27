package com.ois.stickymemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onOpenPermissionSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            StickyTopBar(
                title = "설정",
                subtitle = "민감한 권한은 필요할 때만 켭니다"
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(StickyLayout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(StickySpacing.md)
        ) {
            item { StickySectionHeader("권한 관리") }
            item {
                PermissionCard(
                    title = "권한 설정",
                    reason = "알림, 위치, 연락처, 통화 메모 권한은 기기 설정에서 한 번에 확인하고 바꿀 수 있습니다.",
                    timing = "필요한 기능을 사용할 때 권한을 켜면 됩니다.",
                    actionLabel = "권한 설정 열기",
                    onAction = onOpenPermissionSettings
                )
            }

            item { StickySectionHeader("화면") }
            item {
                StickySoftCard {
                    ListItem(
                        headlineContent = { Text("다크 모드") },
                        supportingContent = { Text("현재 세션에만 적용됩니다.") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { onToggleDarkTheme() }
                            )
                        }
                    )
                }
            }
            item {
                StickySoftCard {
                    ListItem(
                        headlineContent = { Text("StickyMemo") },
                        supportingContent = { Text("빠르게 남기고 나중에 쉽게 다시 찾는 개인 기록 앱") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    reason: String,
    timing: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    StickySoftCard(
        modifier = Modifier.clickable(onClick = onAction)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)) {
            ListItem(
                headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(reason) },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
            )
            Text(
                timing,
                modifier = Modifier.padding(horizontal = StickySpacing.lg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StickySpacing.lg)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Text(actionLabel)
            }
        }
    }
}
