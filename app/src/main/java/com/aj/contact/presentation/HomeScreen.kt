package com.aj.contact.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var contactsList by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher to request READ_CONTACTS permission
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        if (isGranted) {
            // Fetch contacts once permission is granted
            contactsList = getContacts(context)
        } else {
            Log.e("HomeScreen", "READ_CONTACTS permission denied")
        }
    }

    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            // Fetch contacts if permission is already granted
            contactsList = getContacts(context)
        }
    }

    // UI Rendering based on permission and contacts list
    when {
        permissionGranted -> {
            if (contactsList.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    items(contactsList) { contact ->
                        ContactItem(contact = contact)
                    }
                }
            } else {
                // Display message if no contacts are found
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No contacts found.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        else -> {
            // Display message if permission is denied
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Text("Permission to read contacts was denied.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show contact image
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(contact.image)
                .placeholder(android.R.drawable.ic_menu_camera) // Placeholder image
                .error(android.R.drawable.ic_menu_camera) // Fallback image if photoUri is null
                .build()
        )

        Image(
            painter = painter,
            contentDescription = "Contact image",
            modifier = Modifier
                .size(50.dp)
                .padding(end = 16.dp)
        )

        // Show contact name and number
        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contact.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("Range")
fun getContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val contentResolver: ContentResolver = context.contentResolver

    // Define projection to include DISPLAY_NAME and NUMBER
    val projection: Array<String> = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
    )

    // Define sort order
    val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

    // Query the Phone table directly to get names and numbers
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val imageIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

        if (nameIndex == -1 || numberIndex == -1) {
            Log.e("getContacts", "Required columns not found")
            return contacts
        }

        while (it.moveToNext()) {
            val name = it.getString(nameIndex)?.trim()
            val number = it.getString(numberIndex)?.trim()
            val image = it.getString(imageIndex)?.trim()

            if (!name.isNullOrEmpty() && !number.isNullOrEmpty()) {
                contacts.add(Contact(name = name, phoneNumber = number, image =  image))
            }
        }
    } ?: run {
        Log.e("getContacts", "Cursor is null")
    }

    Log.d("getContacts", "Fetched ${contacts.size} contacts")
    return contacts
}

data class Contact(
    var name: String = "",
    var phoneNumber: String = "",
    var image: String? = null
)