package com.waleed.crm.security

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PREF = "waleed_security"
private const val PIN = "pin_code"

fun savedPin(context: Context): String = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(PIN, "") ?: ""
fun savePin(context: Context, pin: String) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(PIN, pin).apply() }
fun clearPin(context: Context) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(PIN).apply() }

@Composable
fun AppLockScreen(context: Context, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val realPin = remember { savedPin(context) }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Text("قفل Waleed CRM", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("رمز الدخول") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Button(onClick = { if (pin == realPin) onUnlocked() else error = "رمز غير صحيح" }, modifier = Modifier.fillMaxWidth()) { Text("فتح التطبيق") }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
