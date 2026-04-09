package com.dokonpro.android.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.StoreSettings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: StoreSettings?,
    isLoading: Boolean,
    error: String?,
    onEditStore: () -> Unit,
    onPrinterSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dokonpro_prefs", Context.MODE_PRIVATE) }
    val currentLocale = prefs.getString("app_locale", "ru") ?: "ru"
    var selectedLocale by remember { mutableStateOf(currentLocale) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        if (isLoading && settings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Store Profile Section
                SectionHeader(stringResource(R.string.settings_store_profile))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings?.name ?: "---",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!settings?.address.isNullOrBlank()) {
                                Text(
                                    text = settings?.address ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!settings?.phone.isNullOrBlank()) {
                                Text(
                                    text = settings?.phone ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onEditStore) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.settings_edit_store)
                            )
                        }
                    }
                }

                // Language Section
                SectionHeader(stringResource(R.string.settings_language))
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        LanguageRadioOption(
                            label = stringResource(R.string.settings_language_ru),
                            selected = selectedLocale == "ru",
                            onClick = {
                                if (selectedLocale != "ru") {
                                    selectedLocale = "ru"
                                    switchLocale(context, prefs, "ru")
                                }
                            }
                        )
                        LanguageRadioOption(
                            label = stringResource(R.string.settings_language_tg),
                            selected = selectedLocale == "tg",
                            onClick = {
                                if (selectedLocale != "tg") {
                                    selectedLocale = "tg"
                                    switchLocale(context, prefs, "tg")
                                }
                            }
                        )
                    }
                }

                // Currency Section
                SectionHeader(stringResource(R.string.settings_currency))
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_currency),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = settings?.currency ?: "TJS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Receipt Section
                SectionHeader(stringResource(R.string.settings_receipt))
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_receipt_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = settings?.receiptHeader ?: "---",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.settings_receipt_footer),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = settings?.receiptFooter ?: "---",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Bluetooth Printer Section
                SectionHeader(stringResource(R.string.settings_printer))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    onClick = onPrinterSettings
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPrinterSettings,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.settings_scan_printer))
                        }
                    }
                }

                // About Section
                SectionHeader(stringResource(R.string.settings_about))
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.app_name)} v1.0.0",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Logout Button
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        width = 1.5.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LanguageRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun switchLocale(
    context: Context,
    prefs: android.content.SharedPreferences,
    locale: String
) {
    prefs.edit().putString("app_locale", locale).apply()
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
}
