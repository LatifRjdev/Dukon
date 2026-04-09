package com.dokonpro.android.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(customer: Customer? = null, onSave: (name: String, phone: String?, email: String?, notes: String?) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    val isEditing = customer != null
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(if (isEditing) R.string.product_edit else R.string.customer_add)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.customer_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.customer_phone)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.customer_email)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.customer_notes)) }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(name, phone.ifBlank { null }, email.ifBlank { null }, notes.ifBlank { null }) },
                enabled = name.length >= 2, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp))
            { Text(stringResource(R.string.save), fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
