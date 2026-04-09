package com.ois.stickymemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.*

val presetColors = listOf(
    "#FFF176" to "노란색",
    "#CCFF90" to "초록색",
    "#B3E5FC" to "파란색",
    "#FFCDD2" to "분홍색",
    "#FFD180" to "주황색",
    "#E1BEE7" to "보라색",
    "#FFFFFF" to "흰색"
)

fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFFFFF176)
    }
}

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
fun ColorPickerDialog(
    currentColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHex by remember { mutableStateOf(currentColorHex) }
    var showColorWheel by remember { mutableStateOf(false) }
    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("색상 선택", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // 기본 7가지 색상
                Text(
                    "기본 색상",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presetColors.forEach { (hex, _) ->
                        val color = hexToColor(hex)
                        val isSelected = selectedHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 직접 선택 토글
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorWheel = !showColorWheel }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        "직접 선택",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showColorWheel) "▲" else "▼",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 현재 선택 색상 미리보기
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(hexToColor(selectedHex))
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                }

                if (showColorWheel) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 색상환
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        controller = controller,
                        initialColor = hexToColor(selectedHex),
                        onColorChanged = { colorEnvelope ->
                            selectedHex = "#${colorEnvelope.hexCode.substring(2)}"
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 밝기 슬라이더
                    BrightnessSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(35.dp),
                        controller = controller
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 선택된 색상 HEX 표시
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = hexToColor(selectedHex),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                selectedHex.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedHex) }) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}