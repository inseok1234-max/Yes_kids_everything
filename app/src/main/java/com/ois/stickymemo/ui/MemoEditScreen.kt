package com.ois.stickymemo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ois.stickymemo.data.ChecklistItem
import com.ois.stickymemo.data.Memo
import com.ois.stickymemo.data.MemoType
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoEditScreen(
    memo: Memo?,
    draftMemo: Memo? = null,
    memoType: MemoType,
    isDarkTheme: Boolean = false,
    existingTags: List<String> = emptyList(),
    onSave: (Memo) -> Unit,
    onBack: () -> Unit,
    onDraftChange: (Memo) -> Unit = {},
    onPickContact: () -> Unit,
    onPickLocation: () -> Unit,
    selectedContactName: String? = null,
    selectedContactPhone: String? = null,
    selectedLat: Double? = null,
    selectedLng: Double? = null,
    selectedLocationName: String? = null
) {
    val initialMemo = draftMemo ?: memo
    var title by remember { mutableStateOf(initialMemo?.title.orEmpty()) }
    var content by remember { mutableStateOf(initialMemo?.content.orEmpty()) }
    var contactName by remember { mutableStateOf(initialMemo?.contactName ?: selectedContactName.orEmpty()) }
    var contactPhone by remember { mutableStateOf(initialMemo?.contactPhone ?: selectedContactPhone.orEmpty()) }
    var locationName by remember { mutableStateOf(initialMemo?.locationName ?: selectedLocationName.orEmpty()) }
    var lat by remember { mutableStateOf(initialMemo?.latitude ?: selectedLat) }
    var lng by remember { mutableStateOf(initialMemo?.longitude ?: selectedLng) }
    var showLocationOptions by remember { mutableStateOf(initialMemo?.latitude != null || memoType == MemoType.LOCATION) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf(initialMemo?.colorHex ?: "#FFF176") }
    var tagInput by remember { mutableStateOf("") }
    var showTagSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(initialMemo?.imageUri?.let { Uri.parse(it) }) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var checklistItems by remember {
        mutableStateOf(if (initialMemo?.checklistJson != null) parseChecklist(initialMemo.checklistJson) else emptyList())
    }
    var newItemText by remember { mutableStateOf("") }
    var tags by remember {
        mutableStateOf(
            initialMemo?.tags
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    val context = LocalContext.current

    LaunchedEffect(selectedContactName, selectedContactPhone) {
        if (selectedContactName != null) contactName = selectedContactName
        if (selectedContactPhone != null) contactPhone = selectedContactPhone
    }
    LaunchedEffect(selectedLat, selectedLng, selectedLocationName) {
        if (selectedLat != null) lat = selectedLat
        if (selectedLng != null) lng = selectedLng
        if (selectedLocationName != null) locationName = selectedLocationName
    }

    fun createImageFile(): Uri {
        val file = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "memo_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) imageUri = cameraImageUri
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageFile()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun addChecklistItem() {
        if (newItemText.isBlank()) return
        checklistItems = checklistItems + ChecklistItem(
            id = (checklistItems.maxOfOrNull { it.id } ?: 0) + 1,
            text = newItemText.trim()
        )
        newItemText = ""
    }

    fun buildSavedMemo(): Memo {
        val now = System.currentTimeMillis()
        val checklistJson = if (memoType == MemoType.CHECKLIST) checklistToJson(checklistItems) else null
        val isContentChanged = memo?.let {
            it.title != title || it.content != content || it.checklistJson != checklistJson
        } ?: true

        val sourceMemo = memo ?: draftMemo
        return sourceMemo?.copy(
            title = title,
            content = content,
            contactName = contactName.ifEmpty { null },
            contactPhone = contactPhone.ifEmpty { null },
            locationName = locationName.ifEmpty { null },
            latitude = lat,
            longitude = lng,
            checklistJson = checklistJson,
            colorHex = selectedColorHex,
            updatedAt = if (isContentChanged) now else sourceMemo.updatedAt,
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
            checklistJson = checklistJson,
            colorHex = selectedColorHex,
            updatedAt = now,
            tags = tags.joinToString(","),
            imageUri = imageUri?.toString()
        )
    }

    fun memoTextForExport(): String = buildString {
        if (title.isNotBlank()) appendLine(title).appendLine()
        if (memoType == MemoType.CHECKLIST) {
            checklistItems.forEach { item ->
                append(if (item.isChecked) "[x] " else "[ ] ")
                appendLine(item.text)
            }
        } else {
            append(content)
        }
    }.trim()

    LaunchedEffect(
        title,
        content,
        contactName,
        contactPhone,
        locationName,
        lat,
        lng,
        checklistItems,
        selectedColorHex,
        tags,
        imageUri
    ) {
        onDraftChange(buildSavedMemo())
    }

    fun saveAsTxt() {
        val fileName = (title.ifBlank { "메모" }) + ".txt"
        val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), fileName)
        file.writeText(memoTextForExport())
        android.widget.Toast.makeText(context, "TXT 저장 완료: $fileName", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun saveAsPdf() {
        val fileName = (title.ifBlank { "메모" }) + ".pdf"
        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val paint = android.graphics.Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        var y = 40f
        memoTextForExport().lines().forEach { line ->
            page.canvas.drawText(line, 40f, y, paint)
            y += 24f
        }
        document.finishPage(page)
        val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), fileName)
        document.writeTo(file.outputStream())
        document.close()
        android.widget.Toast.makeText(context, "PDF 저장 완료: $fileName", android.widget.Toast.LENGTH_SHORT).show()
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColorHex = selectedColorHex,
            onColorSelected = {
                selectedColorHex = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("파일로 저장") },
            text = { Text("저장 형식을 선택하세요.") },
            confirmButton = {
                TextButton(onClick = { saveAsPdf(); showExportDialog = false }) { Text("PDF") }
            },
            dismissButton = {
                TextButton(onClick = { saveAsTxt(); showExportDialog = false }) { Text("TXT") }
            }
        )
    }

    if (showTagSheet) {
        TagBottomSheet(
            tagInput = tagInput,
            onTagInputChange = { tagInput = it.replace(" ", "") },
            tags = tags,
            existingTags = existingTags,
            onAddTag = {
                if (tagInput.isNotBlank() && tagInput !in tags) {
                    tags = tags + tagInput.trim()
                    tagInput = ""
                }
            },
            onRemoveTag = { tag -> tags = tags.filterNot { it == tag } },
            onPickRecommendedTag = { tag -> if (tag !in tags) tags = tags + tag },
            onDismiss = { showTagSheet = false }
        )
    }

    if (showAddressSearch) {
        AddressSearchDialog(
            onAddressSelected = { result ->
                locationName = result.name + " " + result.address
                lat = result.lat
                lng = result.lng
                showAddressSearch = false
                showLocationOptions = true
            },
            onDismiss = { showAddressSearch = false }
        )
    }

    Scaffold(
        topBar = {
            StickyTopBar(
                title = memoType.editorTitle(),
                subtitle = "짧게 쓰고 바로 저장하세요",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(buildSavedMemo()) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("저장")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = StickyLayout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(StickySpacing.md)
        ) {
            item {
                MemoColorStrip(
                    colorHex = selectedColorHex,
                    onClick = { showColorPicker = true }
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("제목") },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(StickyRadius.largeCard),
                    colors = editorTextFieldColors()
                )
            }

            if (memoType == MemoType.CHECKLIST) {
                item {
                    ChecklistProgress(items = checklistItems)
                }
                itemsIndexed(checklistItems, key = { _, item -> item.id }) { index, item ->
                    ChecklistRow(
                        item = item,
                        onCheckedChange = { checked ->
                            checklistItems = checklistItems.toMutableList().also {
                                it[index] = item.copy(isChecked = checked)
                            }
                        },
                        onRemove = {
                            checklistItems = checklistItems.toMutableList().also { it.removeAt(index) }
                        }
                    )
                }
                item {
                    ChecklistAddRow(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        onAdd = { addChecklistItem() }
                    )
                }
            } else {
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        placeholder = { Text("내용을 입력하세요") },
                        shape = RoundedCornerShape(StickyRadius.largeCard),
                        colors = editorTextFieldColors(),
                        maxLines = 14
                    )
                }
            }

            item {
                CompactOptions(
                    memoType = memoType,
                    selectedColorHex = selectedColorHex,
                    tags = tags,
                    imageUri = imageUri,
                    locationName = locationName,
                    contactName = contactName,
                    showLocationOptions = showLocationOptions,
                    onToggleLocation = {
                        showLocationOptions = !showLocationOptions
                        if (!showLocationOptions) {
                            locationName = ""
                            lat = null
                            lng = null
                        }
                    },
                    onPickCurrentLocation = onPickLocation,
                    onSearchAddress = { showAddressSearch = true },
                    onPickContact = onPickContact,
                    onOpenTags = { showTagSheet = true },
                    onPickGallery = { galleryLauncher.launch("image/*") },
                    onOpenCamera = {
                        val permission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            val uri = createImageFile()
                            cameraImageUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    onPickColor = { showColorPicker = true },
                    onExport = { showExportDialog = true },
                    onRemoveImage = { imageUri = null }
                )
            }

            if (memoType == MemoType.CALL) {
                item {
                    StickySoftCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("통화 메모 권한 안내", fontWeight = FontWeight.Bold)
                                Text(
                                    "통화 중 메모를 띄우려면 전화 상태, 연락처, 오버레이 권한이 필요합니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(StickySpacing.xl)) }
        }
    }
}

@Composable
private fun MemoColorStrip(colorHex: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(hexToColor(colorHex))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Text(
            "메모 색상",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChecklistProgress(items: List<ChecklistItem>) {
    if (items.isEmpty()) return
    val checked = items.count { it.isChecked }
    val progress = checked.toFloat() / items.size.toFloat()
    StickySoftCard(contentPadding = StickySpacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("진행률", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("$checked / ${items.size}", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(StickySpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(StickyRadius.card),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = StickyElevation.card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StickySpacing.sm, vertical = StickySpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Text(
                item.text,
                modifier = Modifier.weight(1f),
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "항목 삭제")
            }
        }
    }
}

@Composable
private fun ChecklistAddRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("항목 추가") },
            shape = RoundedCornerShape(StickyRadius.card),
            colors = editorTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        )
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(StickyRadius.button))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun CompactOptions(
    memoType: MemoType,
    selectedColorHex: String,
    tags: List<String>,
    imageUri: Uri?,
    locationName: String,
    contactName: String,
    showLocationOptions: Boolean,
    onToggleLocation: () -> Unit,
    onPickCurrentLocation: () -> Unit,
    onSearchAddress: () -> Unit,
    onPickContact: () -> Unit,
    onOpenTags: () -> Unit,
    onPickGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onPickColor: () -> Unit,
    onExport: () -> Unit,
    onRemoveImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(StickySpacing.sm)) {
        if (memoType != MemoType.CALL) {
            StickyOptionRow(
                icon = Icons.Default.LocationOn,
                title = if (showLocationOptions) "위치 알림 사용 중" else "위치 알림 추가",
                value = locationName.ifBlank { null },
                onClick = onToggleLocation
            )
            if (showLocationOptions) {
                Row(horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm)) {
                    Button(onClick = onPickCurrentLocation, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Text("현재 위치")
                    }
                    Button(onClick = onSearchAddress, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text("주소 검색")
                    }
                }
            }
        }
        if (memoType == MemoType.CALL) {
            StickyOptionRow(
                icon = Icons.Default.Person,
                title = "연락처 연결",
                value = contactName.ifBlank { null },
                onClick = onPickContact
            )
        }
        StickyOptionRow(
            icon = Icons.Default.Tag,
            title = "태그 추가",
            value = if (tags.isNotEmpty()) tags.joinToString("  ") { "#$it" } else null,
            onClick = onOpenTags
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm)) {
            Button(onClick = onPickGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, contentDescription = null)
                Text("사진")
            }
            Button(onClick = onOpenCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Text("카메라")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(StickySpacing.sm)) {
            Button(onClick = onPickColor, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(hexToColor(selectedColorHex))
                )
                Text("색상")
            }
            Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Description, contentDescription = null)
                Text("내보내기")
            }
        }
        if (imageUri != null) {
            StickySoftCard(contentPadding = StickySpacing.md) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "첨부 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(StickyRadius.card)),
                    contentScale = ContentScale.Crop
                )
                TextButton(onClick = onRemoveImage) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("이미지 제거")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagBottomSheet(
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    tags: List<String>,
    existingTags: List<String>,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onPickRecommendedTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StickyLayout.screenPadding)
                .padding(bottom = StickySpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(StickySpacing.md)
        ) {
            Text("태그", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs)) {
                    tags.forEach { tag ->
                        RecordTypeChip(label = "#$tag", selected = true, onClick = { onRemoveTag(tag) })
                    }
                }
            }
            ChecklistAddRow(value = tagInput, onValueChange = onTagInputChange, onAdd = onAddTag)
            val recommended = existingTags.filter { it !in tags }.take(12)
            if (recommended.isNotEmpty()) {
                Text("추천 태그", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(StickySpacing.xs)) {
                    recommended.forEach { tag ->
                        RecordTypeChip(label = "+ #$tag", onClick = { onPickRecommendedTag(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun editorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun MemoType.editorTitle(): String = when (this) {
    MemoType.NORMAL -> "메모"
    MemoType.CHECKLIST -> "체크리스트"
    MemoType.LOCATION -> "위치 메모"
    MemoType.CALL -> "통화 메모"
}

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
