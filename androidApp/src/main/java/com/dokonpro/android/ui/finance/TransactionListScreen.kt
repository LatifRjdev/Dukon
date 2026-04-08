package com.dokonpro.android.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Transaction
import com.dokonpro.shared.domain.entity.TransactionType

private enum class TransactionFilter { ALL, INCOME, EXPENSE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf(TransactionFilter.ALL) }

    val filteredTransactions = when (filter) {
        TransactionFilter.ALL -> transactions
        TransactionFilter.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
        TransactionFilter.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.finance_transactions)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == TransactionFilter.ALL,
                    onClick = { filter = TransactionFilter.ALL },
                    label = { Text("Все") }
                )
                FilterChip(
                    selected = filter == TransactionFilter.INCOME,
                    onClick = { filter = TransactionFilter.INCOME },
                    label = { Text(stringResource(R.string.finance_income)) }
                )
                FilterChip(
                    selected = filter == TransactionFilter.EXPENSE,
                    onClick = { filter = TransactionFilter.EXPENSE },
                    label = { Text(stringResource(R.string.finance_expense)) }
                )
            }

            if (filteredTransactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.finance_no_transactions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionRow(transaction)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
