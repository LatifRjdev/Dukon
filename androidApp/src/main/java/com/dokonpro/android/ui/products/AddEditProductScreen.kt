package com.dokonpro.android.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    product: Product? = null,
    onSave: (name: String, barcode: String?, price: Double, costPrice: Double, quantity: Int, unit: String, categoryId: String?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var price by remember { mutableStateOf(product?.price?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.let { if (it > 0) it.toString() else "" } ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "\u0448\u0442") }
    val isEditing = product != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEditing) R.string.product_edit else R.string.product_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text(stringResource(R.string.product_barcode)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text(stringResource(R.string.product_price)) },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text(stringResource(R.string.product_cost_price)) },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text(stringResource(R.string.product_quantity)) },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(stringResource(R.string.product_unit)) },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSave(name, barcode.ifBlank { null }, price.toDoubleOrNull() ?: 0.0,
                    costPrice.toDoubleOrNull() ?: 0.0, quantity.toIntOrNull() ?: 0, unit, null) },
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.save), fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
