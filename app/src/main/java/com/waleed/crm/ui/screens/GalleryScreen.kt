package com.waleed.crm.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.waleed.crm.data.GalleryFile
import com.waleed.crm.ui.viewmodel.CrmViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: CrmViewModel, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val galleryFiles by viewModel.galleryFiles.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: الصور, 1: الملفات

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                saveFileToGallery(context, uri, viewModel)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المعرض المخصص للتطبيق", fontWeight = FontWeight.Bold) },
                actions = {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "رفع ملف")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفع صورة أو ملف")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("الصور", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.Image, contentDescription = "الصور") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("الملفات (PDF وغيرها)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = "الملفات") }
                )
            }

            val displayedFiles = galleryFiles.filter {
                if (selectedTabIndex == 0) it.type == "image" else it.type == "pdf"
            }

            if (displayedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTabIndex == 0) "لا توجد صور مرفوعة في المعرض حتى الآن." else "لا توجد ملفات مرفوعة في المعرض حتى الآن.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(displayedFiles, key = { it.id }) { file ->
                        GalleryItemCard(file = file, onDelete = { viewModel.deleteGalleryFile(file.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryItemCard(file: GalleryFile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.type == "image") {
                AsyncImage(
                    model = File(file.filePath),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = file.type,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = if (file.type == "image") "ملف صورة" else "ملف مستند / PDF", color = Color.Gray, fontSize = 13.sp)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف الملف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun saveFileToGallery(context: Context, uri: Uri, viewModel: CrmViewModel) {
    val contentResolver = context.contentResolver
    var fileName = "attachment_${System.currentTimeMillis()}"
    val mimeType = contentResolver.getType(uri) ?: ""
    val type = if (mimeType.startsWith("image")) "image" else "pdf"

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex) ?: fileName
            }
        }
    }

    val galleryDir = File(context.filesDir, "media_gallery")
    if (!galleryDir.exists()) {
        galleryDir.mkdirs()
    }

    val destinationFile = File(galleryDir, fileName)
    try {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        viewModel.addGalleryFile(fileName, destinationFile.absolutePath, type)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
