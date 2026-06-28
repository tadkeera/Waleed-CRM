package com.pharmacomm.crm.utils

import android.content.Context
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object ContactImporter {
    fun importContacts(
        context: Context,
        onContactsImported: (List<Pair<String, String>>) -> Unit
    ) {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "يرجى منح صلاحية جهات الاتصال", Toast.LENGTH_SHORT).show()
            return
        }

        val contacts = mutableListOf<Pair<String, String>>()
        
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )) ?: "غير معروف"
                val phone = it.getString(it.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )) ?: ""
                
                if (phone.isNotBlank()) {
                    contacts.add(name to phone)
                }
            }
        }

        if (contacts.isNotEmpty()) {
            onContactsImported(contacts.take(50)) // Limit to 50 for safety
            Toast.makeText(context, "تم استيراد ${contacts.size} جهة اتصال", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لم يتم العثور على جهات اتصال", Toast.LENGTH_SHORT).show()
        }
    }
}