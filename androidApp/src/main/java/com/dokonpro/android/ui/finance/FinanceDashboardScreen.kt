package com.dokonpro.android.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
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
import com.dokonpro.shared.domain.entity.DailyRevenue
import com.dokonpro.shared.domain.entity.FinanceSummary
import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.entity.TransactionType

private val TealColor = Color(0xFF009688)
private val GreenColor = Color(0xFF4CAF50)
private val RedColor = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    summary: FinanceSummary?,
    transactions: List<Transaction>,
    report: List<DailyRevenue>,
    selectedPeriod: String,
    isLoading: Boolean,
    onPeriodChange: (String) -> Unit,
    onTransactionsClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_finance)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading && summary == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period selector
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("day" to R.string.finance_today, "week" to R.string.finance_week, "month" to R.string.finance_month).forEach { (period, labelRes) ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { onPeriodChange(period) },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                }

                // KPI cards: Revenue and Expenses side by side
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard(
                            label = stringResource(R.string.finance_revenue),
                            value = summary?.revenue ?: 0.0,
                            icon = Icons.Filled.ArrowDownward,
                            tintColor = GreenColor,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = stringResource(R.string.finance_expenses),
                            value = summary?.expenses ?: 0.0,
                            icon = Icons.Filled.ArrowUpward,
                            tintColor = RedColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Profit card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.finance_profit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "%.2f TJS".format(summary?.profit ?: 0.0),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                summary?.period ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Simple bar chart
                if (report.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.finance_reports),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleBarChart(data = report)
                    }
                }

                // Recent transactions header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Последние операции",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onTransactionsClick) {
                            Text(stringResource(R.string.finance_transactions))
                        }
                    }
                }

                // Recent transactions (up to 4)
                val recentTransactions = transactions.take(4)
                if (recentTransactions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.finance_no_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(recentTransactions) { transaction ->
                        TransactionRow(transaction)
                    }
                }

                // Navigation buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTransactionsClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.finance_transactions))
                        }
                        OutlinedButton(
                            onClick = onAddExpenseClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.finance_add_expense))
                        }
                        OutlinedButton(
                            onClick = onReportClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.finance_reports))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tintColor.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%.2f".format(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TransactionRow(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val icon = if (isIncome) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward
    val color = if (isIncome) GreenColor else RedColor
    val prefix = if (isIncome) "+" else "-"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    transaction.description ?: if (isIncome) "Доход" else "Расход",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    transaction.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "$prefix%.2f".format(transaction.amount),
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
internal fun SimpleBarChart(data: List<DailyRevenue>) {
    val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
    val maxBarHeight = 120.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(maxBarHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { daily ->
                    val fraction = (daily.amount / maxAmount).coerceIn(0.0, 1.0)
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(fraction.toFloat().coerceAtLeast(0.02f))
                            .background(TealColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { daily ->
                    Text(
                        daily.date.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
