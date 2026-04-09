package com.dokonpro.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.dokonpro.android.ui.MainScreen
import com.dokonpro.android.ui.auth.OtpScreen
import com.dokonpro.android.ui.auth.PhoneInputScreen
import com.dokonpro.android.ui.auth.RegisterScreen
import com.dokonpro.android.ui.customers.AddEditCustomerScreen
import com.dokonpro.android.ui.customers.CustomerDetailScreen
import com.dokonpro.android.ui.customers.CustomerListScreen
import com.dokonpro.android.ui.finance.AddExpenseScreen
import com.dokonpro.android.ui.finance.FinanceDashboardScreen
import com.dokonpro.android.ui.finance.ReportScreen
import com.dokonpro.android.ui.finance.TransactionListScreen
import com.dokonpro.android.ui.staff.AddStaffScreen
import com.dokonpro.android.ui.staff.StaffDetailScreen
import com.dokonpro.android.ui.staff.StaffListScreen
import com.dokonpro.android.ui.settings.EditStoreScreen
import com.dokonpro.android.ui.settings.PrinterSettingsScreen
import com.dokonpro.android.ui.settings.SettingsScreen
import com.dokonpro.android.ui.zakat.ZakatHistoryScreen
import com.dokonpro.android.ui.zakat.ZakatScreen
import com.dokonpro.android.ui.zakat.ZakatSettingsScreen
import com.dokonpro.android.service.BluetoothPrinterService
import com.dokonpro.android.ui.pos.CheckoutScreen
import com.dokonpro.android.ui.pos.POSScreen
import com.dokonpro.android.ui.pos.ReceiptScreen
import com.dokonpro.android.ui.products.AddEditProductScreen
import com.dokonpro.android.ui.products.ProductDetailScreen
import com.dokonpro.android.ui.products.ProductListScreen
import com.dokonpro.android.ui.sales.SalesHistoryScreen
import com.dokonpro.android.viewmodel.AuthStep
import com.dokonpro.android.viewmodel.AuthViewModel
import com.dokonpro.android.viewmodel.CustomerViewModel
import com.dokonpro.android.viewmodel.FinanceViewModel
import com.dokonpro.android.viewmodel.POSViewModel
import com.dokonpro.android.viewmodel.ProductViewModel
import com.dokonpro.android.viewmodel.PrinterViewModel
import com.dokonpro.android.viewmodel.StaffViewModel
import com.dokonpro.android.viewmodel.SettingsViewModel
import com.dokonpro.android.viewmodel.ZakatViewModel
import com.dokonpro.shared.data.sync.SyncStatus
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val PRODUCTS = "products"
    const val PRODUCT_DETAIL = "products/{productId}"
    const val ADD_PRODUCT = "products/add"
    const val EDIT_PRODUCT = "products/{productId}/edit"
    const val POS = "pos"
    const val CHECKOUT = "pos/checkout"
    const val RECEIPT = "pos/receipt"
    const val SALES_HISTORY = "sales/history"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_DETAIL = "customers/{customerId}"
    const val ADD_CUSTOMER = "customers/add"
    const val EDIT_CUSTOMER = "customers/{customerId}/edit"
    const val FINANCE_DASHBOARD = "finance"
    const val FINANCE_TRANSACTIONS = "finance/transactions"
    const val FINANCE_ADD_EXPENSE = "finance/add-expense"
    const val FINANCE_REPORT = "finance/report"
    const val STAFF_LIST = "staff"
    const val STAFF_DETAIL = "staff/{staffId}"
    const val ADD_STAFF = "staff/add"
    const val ZAKAT = "zakat"
    const val ZAKAT_HISTORY = "zakat/history"
    const val ZAKAT_SETTINGS = "zakat/settings"
    const val SETTINGS = "settings"
    const val EDIT_STORE = "settings/edit-store"
    const val PRINTER_SETTINGS = "settings/printer"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            val viewModel: AuthViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            when (state.step) {
                AuthStep.PHONE_INPUT -> PhoneInputScreen(
                    phone = state.phone,
                    isLoading = state.isLoading,
                    error = state.error,
                    onPhoneChange = viewModel::updatePhone,
                    onSendOtp = viewModel::sendOtpCode
                )
                AuthStep.OTP_VERIFY -> OtpScreen(
                    phone = state.phone,
                    otp = state.otp,
                    isLoading = state.isLoading,
                    error = state.error,
                    onOtpChange = viewModel::updateOtp,
                    onVerify = viewModel::verifyOtpCode,
                    onBack = { viewModel.updateOtp("") }
                )
                AuthStep.REGISTER -> RegisterScreen(
                    name = state.name,
                    storeName = state.storeName,
                    isLoading = state.isLoading,
                    error = state.error,
                    onNameChange = viewModel::updateName,
                    onStoreNameChange = viewModel::updateStoreName,
                    onRegister = viewModel::registerUser
                )
                AuthStep.COMPLETE -> {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.MAIN) {
            val syncStatus = org.koin.java.KoinJavaComponent.get<SyncStatus>(SyncStatus::class.java)
            val syncData by syncStatus.status.collectAsStateWithLifecycle()
            MainScreen(
                syncState = syncData.state,
                pendingCount = syncData.pendingCount,
                onNavigateToPOS = { navController.navigate(Routes.POS) },
                onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                onNavigateToCustomers = { navController.navigate(Routes.CUSTOMERS) },
                onNavigateToFinance = { navController.navigate(Routes.FINANCE_DASHBOARD) },
                onNavigateToSalesHistory = { navController.navigate(Routes.SALES_HISTORY) },
                onNavigateToStaff = { navController.navigate(Routes.STAFF_LIST) },
                onNavigateToZakat = { navController.navigate(Routes.ZAKAT) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PRODUCTS) {
            val viewModel: ProductViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
            ProductListScreen(
                products = state.products, searchQuery = state.searchQuery, syncStatus = syncStatus,
                isLoading = state.isLoading, onSearchChange = viewModel::onSearchQueryChange,
                onProductClick = { id -> navController.navigate("products/$id") },
                onAddClick = { navController.navigate(Routes.ADD_PRODUCT) }
            )
        }
        composable(Routes.ADD_PRODUCT) {
            val viewModel: ProductViewModel = koinViewModel()
            AddEditProductScreen(
                onSave = { name, barcode, price, costPrice, quantity, unit, categoryId ->
                    viewModel.addProduct(name, barcode, price, costPrice, quantity, unit, categoryId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PRODUCT_DETAIL) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val viewModel: ProductViewModel = koinViewModel()
            val product = viewModel.state.value.products.find { it.id == productId }
            ProductDetailScreen(
                product = product, onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("products/$productId/edit") },
                onDelete = { viewModel.removeProduct(productId); navController.popBackStack() }
            )
        }
        composable(Routes.EDIT_PRODUCT) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val viewModel: ProductViewModel = koinViewModel()
            val product = viewModel.state.value.products.find { it.id == productId }
            AddEditProductScreen(
                product = product,
                onSave = { name, barcode, price, costPrice, quantity, unit, categoryId ->
                    product?.let {
                        viewModel.updateExistingProduct(
                            it.copy(
                                name = name, barcode = barcode, price = price,
                                costPrice = costPrice, quantity = quantity, unit = unit,
                                categoryId = categoryId,
                                updatedAt = java.time.Instant.now().toString()
                            )
                        )
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.POS) {
            val viewModel: POSViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            POSScreen(
                products = state.products,
                cart = state.cart,
                searchQuery = state.searchQuery,
                onSearchChange = viewModel::onSearchChange,
                onProductTap = viewModel::addProduct,
                onUpdateQuantity = viewModel::updateQuantity,
                onRemoveItem = viewModel::removeProduct,
                onCheckout = {
                    if (state.cart.items.isNotEmpty()) {
                        navController.navigate(Routes.CHECKOUT)
                    }
                }
            )
        }
        composable(Routes.CHECKOUT) {
            val viewModel: POSViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            if (state.completedSale != null) {
                navController.navigate(Routes.RECEIPT) {
                    popUpTo(Routes.POS)
                }
            } else {
                CheckoutScreen(
                    cart = state.cart,
                    saleDiscount = state.saleDiscount,
                    paymentMethod = state.paymentMethod,
                    isLoading = state.isLoading,
                    error = state.error,
                    onDiscountChange = viewModel::setSaleDiscount,
                    onPaymentMethodChange = viewModel::setPaymentMethod,
                    onPay = viewModel::checkout,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Routes.RECEIPT) {
            val viewModel: POSViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val printerViewModel: PrinterViewModel = koinViewModel()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
            val sale = state.completedSale

            if (sale != null) {
                ReceiptScreen(
                    sale = sale,
                    onNewSale = {
                        viewModel.clearCompletedSale()
                        navController.navigate(Routes.POS) {
                            popUpTo(Routes.POS) { inclusive = true }
                        }
                    },
                    onPrint = {
                        settingsState.settings?.let { settings ->
                            printerViewModel.printReceipt(sale, settings)
                        }
                    },
                    onBack = {
                        viewModel.clearCompletedSale()
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable(Routes.SALES_HISTORY) {
            val viewModel: POSViewModel = koinViewModel()
            val sales by viewModel.salesHistory.collectAsStateWithLifecycle()
            val printerViewModel: PrinterViewModel = koinViewModel()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
            SalesHistoryScreen(
                sales = sales,
                onPrintSale = { saleId ->
                    val sale = sales.find { it.id == saleId }
                    if (sale != null && settingsState.settings != null) {
                        printerViewModel.printReceipt(sale, settingsState.settings!!)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CUSTOMERS) {
            val viewModel: CustomerViewModel = koinViewModel()
            val state by viewModel.listState.collectAsStateWithLifecycle()
            CustomerListScreen(
                customers = state.customers,
                searchQuery = state.searchQuery,
                isLoading = state.isLoading,
                onSearchChange = viewModel::onSearchChange,
                onCustomerClick = { id -> navController.navigate("customers/$id") },
                onAddClick = { navController.navigate(Routes.ADD_CUSTOMER) }
            )
        }
        composable(Routes.ADD_CUSTOMER) {
            val viewModel: CustomerViewModel = koinViewModel()
            AddEditCustomerScreen(
                onSave = { name, phone, email, notes ->
                    viewModel.addCustomer(name, phone, email, notes)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CUSTOMER_DETAIL) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: return@composable
            val viewModel: CustomerViewModel = koinViewModel()
            val state by viewModel.detailState.collectAsStateWithLifecycle()
            LaunchedEffect(customerId) { viewModel.loadCustomerDetail(customerId) }
            CustomerDetailScreen(
                customer = state.customer,
                purchases = state.purchases,
                isLoading = state.isLoading,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("customers/$customerId/edit") }
            )
        }
        composable(Routes.EDIT_CUSTOMER) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: return@composable
            val viewModel: CustomerViewModel = koinViewModel()
            val state by viewModel.detailState.collectAsStateWithLifecycle()
            LaunchedEffect(customerId) { viewModel.loadCustomerDetail(customerId) }
            AddEditCustomerScreen(
                customer = state.customer,
                onSave = { name, phone, email, notes ->
                    state.customer?.let {
                        viewModel.updateExistingCustomer(
                            it.copy(
                                name = name, phone = phone, email = email, notes = notes,
                                updatedAt = java.time.Instant.now().toString()
                            )
                        )
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FINANCE_DASHBOARD) {
            val viewModel: FinanceViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            FinanceDashboardScreen(
                summary = state.summary,
                transactions = state.transactions,
                report = state.report,
                selectedPeriod = state.selectedPeriod,
                isLoading = state.isLoading,
                onPeriodChange = viewModel::selectPeriod,
                onTransactionsClick = { navController.navigate(Routes.FINANCE_TRANSACTIONS) },
                onAddExpenseClick = { navController.navigate(Routes.FINANCE_ADD_EXPENSE) },
                onReportClick = { navController.navigate(Routes.FINANCE_REPORT) }
            )
        }
        composable(Routes.FINANCE_TRANSACTIONS) {
            val viewModel: FinanceViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            TransactionListScreen(
                transactions = state.transactions,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FINANCE_ADD_EXPENSE) {
            val viewModel: FinanceViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            AddExpenseScreen(
                isLoading = state.isLoading,
                onSave = { amount, description, categoryId ->
                    viewModel.submitExpense(amount, description, categoryId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FINANCE_REPORT) {
            val viewModel: FinanceViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ReportScreen(
                report = state.report,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STAFF_LIST) {
            val viewModel: StaffViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            StaffListScreen(
                staff = state.staff,
                isLoading = state.isLoading,
                onStaffClick = { id -> navController.navigate("staff/$id") },
                onAddClick = { navController.navigate(Routes.ADD_STAFF) }
            )
        }
        composable(Routes.ADD_STAFF) {
            val viewModel: StaffViewModel = koinViewModel()
            AddStaffScreen(
                onSave = { phone, name, role ->
                    viewModel.addNewStaff(phone, name, role)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STAFF_DETAIL) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: return@composable
            val viewModel: StaffViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val staff = state.staff.find { it.id == staffId }
            StaffDetailScreen(
                staff = staff,
                onChangeRole = { role -> viewModel.changeRole(staffId, role) },
                onDeactivate = { viewModel.removeStaff(staffId); navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ZAKAT) {
            val viewModel: ZakatViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ZakatScreen(
                calculation = state.calculation,
                isLoading = state.isLoading,
                isSaved = state.isSaved,
                error = state.error,
                onCalculate = viewModel::calculate,
                onSave = viewModel::save,
                onHistoryClick = { navController.navigate(Routes.ZAKAT_HISTORY) },
                onSettingsClick = { navController.navigate(Routes.ZAKAT_SETTINGS) }
            )
        }
        composable(Routes.ZAKAT_HISTORY) {
            val viewModel: ZakatViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ZakatHistoryScreen(
                history = state.history,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ZAKAT_SETTINGS) {
            val viewModel: ZakatViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ZakatSettingsScreen(
                config = state.config,
                isLoading = state.isLoading,
                onSave = { goldRate, silverRate ->
                    viewModel.updateConfig(goldRate, silverRate)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = state.settings,
                isLoading = state.isLoading,
                error = state.error,
                onEditStore = { navController.navigate(Routes.EDIT_STORE) },
                onPrinterSettings = { navController.navigate(Routes.PRINTER_SETTINGS) },
                onLogout = {
                    viewModel.performLogout {
                        navController.navigate(Routes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EDIT_STORE) {
            val viewModel: SettingsViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            EditStoreScreen(
                settings = state.settings,
                isLoading = state.isLoading,
                isSaved = state.isSaved,
                onSave = viewModel::saveSettings,
                onSavedAck = viewModel::clearSaved,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PRINTER_SETTINGS) {
            val printerViewModel: PrinterViewModel = koinViewModel()
            val printerState by printerViewModel.state.collectAsStateWithLifecycle()
            val printerService: BluetoothPrinterService = org.koin.compose.koinInject()
            PrinterSettingsScreen(
                state = printerState,
                isBluetoothEnabled = printerService.isBluetoothEnabled(),
                onSelectPrinter = printerViewModel::selectPrinter,
                onClearPrinter = printerViewModel::clearPrinter,
                onTestPrint = {
                    val settingsViewModel: SettingsViewModel = org.koin.java.KoinJavaComponent.get(SettingsViewModel::class.java)
                    settingsViewModel.state.value.settings?.let { settings ->
                        val testSale = com.dokonpro.shared.domain.entity.Sale(
                            id = "test-print-0001",
                            totalAmount = 100.0,
                            discount = 0.0,
                            paymentMethod = com.dokonpro.shared.domain.entity.PaymentMethod.CASH,
                            customerId = null,
                            storeId = settings.storeId,
                            isRefunded = false,
                            createdAt = java.time.LocalDateTime.now().toString(),
                            items = listOf(
                                com.dokonpro.shared.domain.entity.SaleItem(
                                    id = "test-item-1",
                                    saleId = "test-print-0001",
                                    productId = "test-p1",
                                    name = "Тестовый товар",
                                    quantity = 1,
                                    price = 100.0,
                                    discount = 0.0
                                )
                            )
                        )
                        printerViewModel.printReceipt(testSale, settings)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
