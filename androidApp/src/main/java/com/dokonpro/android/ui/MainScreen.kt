package com.dokonpro.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Sync
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
import com.dokonpro.android.ui.theme.*
import com.dokonpro.shared.data.sync.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    syncState: SyncState = SyncState.IDLE,
    pendingCount: Long = 0,
    onNavigateToPOS: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToSalesHistory: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onNavigateToZakat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (pendingCount > 0) {
                        IconButton(onClick = {}) {
                            BadgedBox(
                                badge = {
                                    Badge { Text(pendingCount.toString()) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync pending",
                                    tint = Color(0xFFFFA000)
                                )
                            }
                        }
                    } else if (syncState == SyncState.SYNCING) {
                        IconButton(onClick = {}) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text(stringResource(R.string.nav_home)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPOS,
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    label = { Text(stringResource(R.string.nav_pos)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProducts,
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text(stringResource(R.string.nav_products)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToFinance,
                    icon = { Icon(Icons.Default.AccountBox, null) },
                    label = { Text(stringResource(R.string.nav_finance)) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Quick Actions
            Text(
                stringResource(R.string.nav_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard(
                    title = stringResource(R.string.nav_pos),
                    icon = "💰",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPOS
                )
                QuickActionCard(
                    title = stringResource(R.string.nav_products),
                    icon = "📦",
                    color = Color(0xFFFFA000),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToProducts
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard(
                    title = stringResource(R.string.nav_customers),
                    icon = "👥",
                    color = Color(0xFF5C6BC0),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCustomers
                )
                QuickActionCard(
                    title = stringResource(R.string.nav_sales),
                    icon = "📋",
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSalesHistory
                )
            }

            Spacer(Modifier.height(24.dp))

            // Management section
            Text(
                stringResource(R.string.nav_management),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MenuCard(stringResource(R.string.nav_finance), "📊", onClick = onNavigateToFinance)
            Spacer(Modifier.height(8.dp))
            MenuCard(stringResource(R.string.staff_title), "👤", onClick = onNavigateToStaff)
            Spacer(Modifier.height(8.dp))
            MenuCard(stringResource(R.string.zakat_title), "🕌", onClick = onNavigateToZakat)
            Spacer(Modifier.height(8.dp))
            MenuCard(stringResource(R.string.settings_title), "⚙️", onClick = onNavigateToSettings)
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MenuCard(title: String, icon: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
