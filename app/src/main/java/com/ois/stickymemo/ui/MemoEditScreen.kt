package com.ois.stickymemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import android.net.Uri
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ois.stickymemo.data.ChecklistItem
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoEditScreen(
    memo: Memo?,
    memoType: MemoType,
    isDarkTheme: Boolean = false,
    existingTags: List<String> = emptyList(),
    onSave: (Memo) -> Unit,
    onBack: () -> Unit,
    onPickContact: () -> Unit,
    onPickLocation: () -> Unit,
    selectedContactName: String? = null,
    selectedContactPhone: String? = null,
    selectedLat: Double? = null,
    selectedLng: Double? = null,
    selectedLocationName: String? = null
) {
    var title by remember { mutableStateOf(memo?.title ?: "") }
    var content by remember { mutableStateOf(memo?.content ?: "") }
    var contactName by remember { mutableStateOf(memo?.contactName ?: selectedContactName ?: "") }
    var contactPhone by remember {
        mutableStateOf(memo?.contactPhone ?: selectedContactPhone ?: "")
    }
    var locationName by remember {
        mutableStateOf(memo?.locationName ?: selectedLocationName ?: "")
    }
    var lat by remember { mutableStateOf(memo?.latitude ?: selectedLat) }
    var lng by remember { mutableStateOf(memo?.longitude ?: selectedLng) }
    var showLocationOptions by remember { mutableStateOf(memo?.latitude != null) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf(memo?.colorHex ?: "#FFF176") }
    var tagInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showPdfTxtDialog by remember { mutableStateOf(false) }
    var tags by remember {
        mutableStateOf(
            if (memo?.tags?.isNotBlank() == true)
                memo.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else listOf()
        )
    }
    // 다크모드 색상 분기
    val bgColor = if (isDarkTheme) Color(0xFF121212) else hexToColor(selectedColorHex)
    val topBarColor = if (isDarkTheme) Color(0xFF1E1E1E) else WarmOrange
    val topBarContentColor = if (isDarkTheme) Color(0xFFFF8C42) else Color.White
    val surfaceColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) Color(0xFFE0E0E0) else WarmBrown
    val textSecondary = if (isDarkTheme) Color(0xFFBDBDBD) else WarmBrownLight
    val fieldBg = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White.copy(alpha = 0.8f)
    val fieldBgUnfocused = if (isDarkTheme) Color(0xFF242424) else Color.White.copy(alpha = 0.6f)

    var imageUri by remember { mutableStateOf<Uri?>(memo?.imageUri?.let { Uri.parse(it) }) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
// 앨범에서 선택
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    // 카메라로 촬영
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) imageUri = cameraImageUri
    }

    fun createImageFile(): Uri {
        val file = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "memo_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // 카메라 권한 요청
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageFile()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    var checklistItems by remember {
        mutableStateOf(
            if (memo?.checklistJson != null) parseChecklist(memo.checklistJson)
            else listOf()
        )
    }
    var newItemText by remember { mutableStateOf("") }

    LaunchedEffect(selectedContactName, selectedContactPhone) {
        if (selectedContactName != null) contactName = selectedContactName
        if (selectedContactPhone != null) contactPhone = selectedContactPhone
    }
    LaunchedEffect(selectedLat, selectedLng, selectedLocationName) {
        if (selectedLat != null) lat = selectedLat
        if (selectedLng != null) lng = selectedLng
        if (selectedLocationName != null) locationName = selectedLocationName
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColorHex = selectedColorHex,
            onColorSelected = { hex ->
                selectedColorHex = hex
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
    fun saveAsTxt() {
        val fileName = (if (title.isNotBlank()) title else "메모") + ".txt"
        val contentText = buildString {
            if (title.isNotBlank()) { append(title); append("\n\n") }
            if (memoType == MemoType.CHECKLIST) {
                checklistItems.forEach { item ->
                    append(if (item.isChecked) "✅ " else "☐ ")
                    append(item.text); append("\n")
                }
            } else {
                append(content)
            }
        }
        val file = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
            fileName
        )
        file.writeText(contentText.trim())
        android.widget.Toast.makeText(context, "TXT 저장 완료: $fileName", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun saveAsPdf() {
        val fileName = (if (title.isNotBlank()) title else "메모") + ".pdf"
        val contentText = buildString {
            if (title.isNotBlank()) { append(title); append("\n\n") }
            if (memoType == MemoType.CHECKLIST) {
                checklistItems.forEach { item ->
                    append(if (item.isChecked) "✅ " else "☐ ")
                    append(item.text); append("\n")
                }
            } else {
                append(content)
            }
        }
        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        var y = 40f
        contentText.trim().lines().forEach { line ->
            canvas.drawText(line, 40f, y, paint)
            y += 24f
        }
        document.finishPage(page)
        val file = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
            fileName
        )
        document.writeTo(file.outputStream())
        document.close()
        android.widget.Toast.makeText(context, "PDF 저장 완료: $fileName", android.widget.Toast.LENGTH_SHORT).show()
    }
    // PDF/TXT 선택 다이얼로그
    if (showPdfTxtDialog) {
        AlertDialog(
            onDismissRequest = { showPdfTxtDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White,
            title = { Text("파일로 저장", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("저장 형식을 선택하세요", color = textSecondary) },
            confirmButton = {
                TextButton(onClick = { saveAsPdf(); showPdfTxtDialog = false }) {
                    Text("PDF", color = WarmOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saveAsTxt(); showPdfTxtDialog = false }) {
                    Text("TXT", color = WarmOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    // 위치 알림 다이얼로그
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White,
            title = { Text("📍 위치 알림 설정", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("위치 알림 사용", color = textPrimary)
                        Switch(
                            checked = showLocationOptions,
                            onCheckedChange = {
                                showLocationOptions = it
                                if (!it) { locationName = ""; lat = null; lng = null }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WarmOrange
                            )
                        )
                    }
                    if (showLocationOptions) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (locationName.isNotEmpty()) {
                            Text("📍 $locationName", fontSize = 13.sp, color = Color(0xFF64B5F6))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onPickLocation()
                                    showLocationDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("현재 위치", fontSize = 13.sp)
                            }
                            Button(
                                onClick = { showAddressSearch = true; showLocationDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmBrownMid),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("주소 검색", fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("확인", color = WarmOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 태그 바텀시트
    if (showTagSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTagSheet = false },
            containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("🏷️ 태그", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // 현재 태그 목록
                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = WarmOrange.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text("#$tag", fontSize = 13.sp, color = if (isDarkTheme) Color(0xFFFFCC80) else WarmBrownMid)
                                    IconButton(
                                        onClick = { tags = tags.filter { it != tag } },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = textSecondary)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 태그 입력
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it.replace(" ", "") },
                        label = { Text("새 태그 추가", color = textSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBgUnfocused,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = WarmOrange,
                            unfocusedBorderColor = WarmOrange.copy(alpha = 0.4f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (tagInput.isNotBlank() && !tags.contains(tagInput)) {
                                tags = tags + tagInput.trim()
                                tagInput = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank() && !tags.contains(tagInput)) {
                                tags = tags + tagInput.trim()
                                tagInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmOrange)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                }

                // 추천 태그
                val recommendedTags = existingTags.filter { it !in tags }
                if (recommendedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("추천 태그", fontSize = 13.sp, color = textSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recommendedTags.take(10).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkTheme) Color(0xFF3A3A3A) else WarmOrange.copy(alpha = 0.08f),
                                modifier = Modifier.clickable {
                                    if (!tags.contains(tag)) tags = tags + tag
                                }
                            ) {
                                Text(
                                    "+ #$tag",
                                    fontSize = 13.sp,
                                    color = WarmOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddressSearch) {
        AddressSearchDialog(
            onAddressSelected = { result ->
                locationName = result.name + " " + result.address
                lat = result.lat
                lng = result.lng
                showAddressSearch = false
                showLocationOptions = false
            },
            onDismiss = { showAddressSearch = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (memoType) {
                            MemoType.NORMAL -> "📝 일반 메모"
                            MemoType.CHECKLIST -> "📋 체크리스트"
                            MemoType.LOCATION -> "📍 위치 메모"
                            MemoType.CALL -> "📞 통화 메모"
                        },
                        fontWeight = FontWeight.Bold,
                        color = topBarContentColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = topBarContentColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(hexToColor(selectedColorHex))
                                .border(2.dp, topBarContentColor, CircleShape)
                        )
                    }
                    // 점 3개 메뉴
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "더보기", tint = topBarContentColor)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(
                                if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
                            )
                        ) {
                            DropdownMenuItem(
                                text = { Text("📷 카메라", color = textPrimary) },
                                onClick = {
                                    showMenu = false
                                    val permission = android.Manifest.permission.CAMERA
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, permission
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val uri = createImageFile()
                                        cameraImageUri = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(permission)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🖼️ 이미지", color = textPrimary) },
                                onClick = {
                                    showMenu = false
                                    galleryLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🏷️ 태그", color = textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showTagSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📍 위치 알림", color = textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showLocationDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("📄 파일로 저장", color = textPrimary) },
                                onClick = {
                                    showMenu = false
                                    showPdfTxtDialog = true
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        val now = System.currentTimeMillis()
                        val isContentChanged = memo?.let {
                            it.title != title ||
                                    it.content != content ||
                                    it.checklistJson != checklistToJson(checklistItems)
                        } ?: true

                        val savedMemo = memo?.copy(
                            title = title,
                            content = content,
                            contactName = contactName.ifEmpty { null },
                            contactPhone = contactPhone.ifEmpty { null },
                            locationName = locationName.ifEmpty { null },
                            latitude = lat,
                            longitude = lng,
                            checklistJson = if (memoType == MemoType.CHECKLIST)
                                checklistToJson(checklistItems) else null,
                            colorHex = selectedColorHex,
                            updatedAt = if (isContentChanged) now else memo.updatedAt,
                            tags = tags.joinToString(","),
                            imageUri = imageUri?.toString()
                        ) ?: Memo(
                            type = memoType,
                            title = title,
                            content = content,
                            contactName = contactName.ifEmpty { null },
                            contactPhone = contactPhone.ifEmpty { null },
                            locationName = locationName.ifEmpty { null },
                            latitude = lat,
                            longitude = lng,
                            checklistJson = if (memoType == MemoType.CHECKLIST)
                                checklistToJson(checklistItems) else null,
                            colorHex = selectedColorHex,
                            updatedAt = now,
                            tags = tags.joinToString(","),
                            imageUri = imageUri?.toString()
                        )
                        onSave(savedMemo)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "저장", tint = topBarContentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 제목
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목", color = textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBgUnfocused,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedLabelColor = textSecondary,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = WarmOrange,
                        unfocusedBorderColor = WarmOrange.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )
            }

            // 내용 (체크리스트 제외)
            if (memoType != MemoType.CHECKLIST) {
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("내용", color = textSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBg,
                            unfocusedContainerColor = fieldBgUnfocused,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedLabelColor = textSecondary,
                            unfocusedLabelColor = textSecondary,
                            focusedBorderColor = WarmOrange,
                            unfocusedBorderColor = WarmOrange.copy(alpha = 0.4f)
                        ),
                        maxLines = 10
                    )
                }
            }

            // 체크리스트
            if (memoType == MemoType.CHECKLIST) {

                // 진행률 바
                if (checklistItems.isNotEmpty()) {
                    item {
                        val total = checklistItems.size
                        val checked = checklistItems.count { it.isChecked }
                        val progress = checked.toFloat() / total.toFloat()

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = surfaceColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "진행률",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        "$checked / $total",
                                        fontSize = 13.sp,
                                        color = if (progress >= 1f) Color(0xFF66BB6A) else textSecondary,
                                        fontWeight = if (progress >= 1f) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (progress >= 1f) Color(0xFF66BB6A) else WarmOrange,
                                    trackColor = WarmOrange.copy(alpha = 0.2f)
                                )
                                if (progress >= 1f) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "🎉 모든 항목 완료!",
                                        fontSize = 12.sp,
                                        color = Color(0xFF66BB6A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 체크리스트 항목 목록
                itemsIndexed(checklistItems) { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { checked ->
                                checklistItems = checklistItems.toMutableList().also {
                                    it[index] = item.copy(isChecked = checked)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = WarmOrange,
                                uncheckedColor = textSecondary
                            )
                        )
                        Text(
                            text = item.text,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            color = if (item.isChecked) textSecondary else textPrimary
                        )
                        IconButton(onClick = {
                            checklistItems = checklistItems.toMutableList().also {
                                it.removeAt(index)
                            }
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "항목 삭제",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = WarmOrange.copy(alpha = 0.2f))
                }

                // 항목 추가 입력창
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            label = { Text("항목 추가", color = textSecondary) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBgUnfocused,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedLabelColor = textSecondary,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = WarmOrange,
                                unfocusedBorderColor = WarmOrange.copy(alpha = 0.4f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newItemText.isNotBlank()) {
                                    checklistItems = checklistItems + ChecklistItem(
                                        id = checklistItems.size,
                                        text = newItemText.trim()
                                    )
                                    newItemText = ""
                                }
                            })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newItemText.isNotBlank()) {
                                    checklistItems = checklistItems + ChecklistItem(
                                        id = checklistItems.size,
                                        text = newItemText.trim()
                                    )
                                    newItemText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmOrange)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "추가", tint = Color.White)
                        }
                    }
                }
            }
            // 이미지 미리보기 (첨부된 경우만 표시)
            if (imageUri != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🖼️ 첨부 이미지", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "첨부 이미지",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { imageUri = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE57373))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("이미지 제거", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 위치 옵션 (통화 메모 제외)
            if (memoType != MemoType.CALL) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "📍 위치 알림 설정",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary
                                )
                                Switch(
                                    checked = showLocationOptions,
                                    onCheckedChange = {
                                        showLocationOptions = it
                                        if (!it) {
                                            locationName = ""
                                            lat = null
                                            lng = null
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = WarmOrange
                                    )
                                )
                            }
                            if (showLocationOptions) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (locationName.isNotEmpty()) {
                                    Text(
                                        "📍 $locationName",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64B5F6)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onPickLocation,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WarmOrange
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("현재 위치", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { showAddressSearch = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WarmBrownMid
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("주소 검색", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 통화 메모
            if (memoType == MemoType.CALL) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "📞 연락처 태그",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (contactName.isNotEmpty()) {
                                Text(
                                    "연락처: $contactName",
                                    fontSize = 14.sp,
                                    color = Color(0xFFCE93D8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Button(
                                onClick = onPickContact,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF880E4F)
                                )
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("연락처에서 선택")
                            }
                        }
                    }
                }
            }
        } // LazyColumn 끝
    } // Scaffold 끝
} // MemoEditScreen 끝

fun parseChecklist(json: String): List<ChecklistItem> {
    return try {
        val array = JSONArray(json)
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            ChecklistItem(
                id = obj.getInt("id"),
                text = obj.getString("text"),
                isChecked = obj.getBoolean("isChecked")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun checklistToJson(items: List<ChecklistItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("text", item.text)
            put("isChecked", item.isChecked)
        }
        array.put(obj)
    }
    return array.toString()
}