package com.dokonpro.android.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(customer: Customer?, purchases: List<Sale>, isLoading: Boolean, onBack: () -> Unit, onEdit: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(customer?.name ?: stringResource(R.string.customer_detail)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.product_edit)) } })
    }) { padding ->
        if (customer == null) { Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(stringResource(R.string.customer_not_found)) }; return@Scaffold }
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary) } }
                        Spacer(Modifier.height(12.dp))
                        Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        customer.phone?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        customer.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${customer.totalSpent} c", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.customer_total_spent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${customer.visitCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.customer_visits), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                customer.notes?.let {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) { Text(stringResource(R.string.customer_notes), style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(4.dp)); Text(it) } } }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.customer_purchases), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            if (purchases.isEmpty()) { item { Text(stringResource(R.string.customer_no_purchases), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            else items(purchases, key = { it.id }) { sale ->
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("#${sale.id.takeLast(8).uppercase()}", fontWeight = FontWeight.SemiBold)
                            Text("${sale.items.size} ${stringResource(R.string.pos_items)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("${sale.totalAmount} c", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } } }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
