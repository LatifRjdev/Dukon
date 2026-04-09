package com.dokonpro.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.StoreSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStoreScreen(
    settings: StoreSettings?,
    isLoading: Boolean,
    isSaved: Boolean,
    onSave: (StoreSettings) -> Unit,
    onSavedAck: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember(settings) { mutableStateOf(settings?.name ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }
    var phone by remember(settings) { mutableStateOf(settings?.phone ?: "") }
    var receiptHeader by remember(settings) { mutableStateOf(settings?.receiptHeader ?: "") }
    var receiptFooter by remember(settings) { mutableStateOf(settings?.receiptFooter ?: "") }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onSavedAck()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_edit_store)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_store_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.settings_store_address)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.settings_store_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = receiptHeader,
                onValueChange = { receiptHeader = it },
                label = { Text(stringResource(R.string.settings_receipt_header)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                label = { Text(stringResource(R.string.settings_receipt_footer)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (settings != null) {
                        onSave(
                            settings.copy(
                                name = name,
                                address = address,
                                phone = phone,
                                receiptHeader = receiptHeader,
                                receiptFooter = receiptFooter
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.save), fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
