package com.dokonpro.android.ui.zakat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.ZakatCalculation

private val TealColor = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(
    calculation: ZakatCalculation?,
    isLoading: Boolean,
    isSaved: Boolean,
    error: String?,
    onCalculate: () -> Unit,
    onSave: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.zakat_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealColor,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = stringResource(R.string.zakat_history),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.zakat_settings),
                            tint = Color.White
                        )
                    }
                }
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
            // Calculate button
            Button(
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.zakat_calculate))
            }

            if (calculation != null) {
                // Asset breakdown
                Text(
                    stringResource(R.string.zakat_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                AssetRow(
                    label = stringResource(R.string.zakat_inventory_value),
                    value = calculation.inventoryValue
                )
                AssetRow(
                    label = stringResource(R.string.zakat_cash_balance),
                    value = calculation.cashBalance
                )
                AssetRow(
                    label = stringResource(R.string.zakat_receivables),
                    value = calculation.receivables
                )
                AssetRow(
                    label = stringResource(R.string.zakat_liabilities),
                    value = calculation.liabilities,
                    isDeduction = true
                )

                HorizontalDivider()

                // Nisab threshold
                AssetRow(
                    label = stringResource(R.string.zakat_nisab),
                    value = calculation.nisabThreshold
                )

                // Zakatable amount
                AssetRow(
                    label = stringResource(R.string.zakat_zakatable),
                    value = calculation.zakatableAmount
                )

                // Zakat due — large teal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealColor.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.zakat_due),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TealColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "%.2f TJS".format(calculation.zakatDue),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TealColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.zakat_rate_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${stringResource(R.string.zakat_gold_rate)}: %.2f".format(calculation.goldRate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Save button
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && !isSaved
                ) {
                    Text(
                        if (isSaved) stringResource(R.string.zakat_saved)
                        else stringResource(R.string.zakat_save)
                    )
                }
            }

            // Error
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    label: String,
    value: Double,
    isDeduction: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${if (isDeduction) "-" else ""}%.2f TJS".format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDeduction) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
        )
    }
}
