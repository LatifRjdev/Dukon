package com.dokonpro.android.ui.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Role
import com.dokonpro.shared.domain.entity.Staff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staff: Staff?,
    onChangeRole: (Role) -> Unit,
    onDeactivate: () -> Unit,
    onBack: () -> Unit
) {
    var selectedRole by remember(staff) { mutableStateOf(staff?.role ?: Role.CASHIER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(staff?.name ?: stringResource(R.string.staff_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (staff == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(stringResource(R.string.staff_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Profile card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(staff.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(staff.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    RoleBadge(staff.role)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Change role section
            Text(
                text = stringResource(R.string.staff_change_role),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleFilterChip(
                    label = stringResource(R.string.staff_owner),
                    selected = selectedRole == Role.OWNER,
                    onClick = { selectedRole = Role.OWNER }
                )
                RoleFilterChip(
                    label = stringResource(R.string.staff_manager),
                    selected = selectedRole == Role.MANAGER,
                    onClick = { selectedRole = Role.MANAGER }
                )
                RoleFilterChip(
                    label = stringResource(R.string.staff_cashier),
                    selected = selectedRole == Role.CASHIER,
                    onClick = { selectedRole = Role.CASHIER }
                )
            }

            if (selectedRole != staff.role) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onChangeRole(selectedRole) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.save), fontSize = 16.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Deactivate button
            OutlinedButton(
                onClick = onDeactivate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.staff_deactivate), fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
