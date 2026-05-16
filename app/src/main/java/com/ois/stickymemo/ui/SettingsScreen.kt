package com.ois.stickymemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onEnableCallMemo: () -> Unit = {},
    onEnableBackgroundLocation: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SettingsSectionTitle("권한 관리") }
            item {
                ListItem(
                    headlineContent = { Text("알림") },
                    supportingContent = { Text("위치 알림이나 리마인더를 처음 사용할 때 요청합니다") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("위치") },
                    supportingContent = { Text("위치 메모를 만들 때만 요청합니다") },
                    leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("통화 메모") },
                    supportingContent = { Text("고급 기능을 직접 켠 뒤 통화/오버레이 권한을 요청합니다") },
                    leadingContent = { Icon(Icons.Default.Call, contentDescription = null) }
                )
            }
            item { HorizontalDivider() }

            item { SettingsSectionTitle("데이터") }
            item {
                ListItem(
                    headlineContent = { Text("백업 / 내보내기") },
                    supportingContent = { Text("빠른 기록 위젯과 함께 확장할 예정입니다") },
                    leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("데이터 관리") },
                    supportingContent = { Text("데이터 이전과 구조화 작업에서 계속 개선합니다") },
                    leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) }
                )
            }
            item { HorizontalDivider() }

            item { SettingsSectionTitle("고급 기능") }
            item {
                ListItem(
                    headlineContent = { Text("민감 기능은 직접 켜기") },
                    supportingContent = { Text("통화 메모, 오버레이, 백그라운드 위치는 설명 후 활성화합니다") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) }
                )
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onEnableCallMemo) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Text("통화 메모 권한 설정")
                    }
                    FilledTonalButton(onClick = onEnableBackgroundLocation) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Text("백그라운드 위치 권한 설정")
                    }
                }
            }
            item { HorizontalDivider() }

            item { SettingsSectionTitle("앱") }
            item {
                ListItem(
                    headlineContent = { Text("다크 모드") },
                    supportingContent = { Text("현재 세션에 적용됩니다") },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("StickyMemo") },
                    supportingContent = { Text("상황 기반 개인 기록 앱") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
