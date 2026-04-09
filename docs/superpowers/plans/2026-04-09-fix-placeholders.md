# v1.1.0 Fix Placeholders + Bluetooth Printing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all placeholder stubs found in the MVP audit — replace hardcoded storeId with real auth session, implement product/customer edit flows, add Bluetooth ESC/POS receipt printing, and replace empty notification icon with sync status.

**Architecture:** Backend adds storeId to verify-otp response. KMP shared extends TokenStorage with storeId persistence and adds SessionProvider + ReceiptFormatter. Android adds BluetoothPrinterService for ESC/POS thermal printing, reuses existing Add screens for Edit with pre-population, and wires sync status to MainScreen header.

**Tech Stack:** NestJS 11, Prisma 6 | Kotlin 2.1.0, Koin 4.0.2 | Jetpack Compose, Android Bluetooth Classic API, ESC/POS protocol

---

## Task 1: Backend — Add storeId to verify-otp response

**Files:**
- Modify: `api/src/modules/auth/auth.service.ts`

- [ ] **Step 1: Update verifyOtp to include storeId**

In `api/src/modules/auth/auth.service.ts`, modify the `verifyOtp` method. After finding the user, query their first UserStore to get storeId. Update the return type to include `storeId`.

Replace the section after `await this.prisma.otpCode.update(...)` with:

```typescript
    const user = await this.prisma.user.findUnique({
      where: { phone },
      include: { stores: { take: 1 } },
    });
    const isNewUser = !user;
    const storeId = user?.stores?.[0]?.storeId ?? null;

    const tokenPayload = { phone, sub: user?.id ?? 'pending' };
    const accessToken = this.generateAccessToken(tokenPayload);
    const refreshToken = this.generateRefreshToken(tokenPayload);

    return { accessToken, refreshToken, isNewUser, storeId };
```

- [ ] **Step 2: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && npm run build
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/src/modules/auth/auth.service.ts
git commit -m "fix(auth): add storeId to verify-otp response"
```

---

## Task 2: KMP — Extend TokenStorage + SessionProvider + Update Auth DTOs

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.kt`
- Modify: `shared/src/androidMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.android.kt`
- Modify: `shared/src/iosMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.ios.kt`
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/AuthDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/AuthRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/SessionProvider.kt`
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/AuthModule.kt`

- [ ] **Step 1: Extend TokenStorage expect with storeId methods**

Add to `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.kt`:
```kotlin
    fun saveStoreId(storeId: String)
    fun getStoreId(): String?
    fun clearStoreId()
```

- [ ] **Step 2: Implement Android actual storeId methods**

Add to `shared/src/androidMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.android.kt`:
```kotlin
    actual fun saveStoreId(storeId: String) {
        prefs.edit().putString("store_id", storeId).apply()
    }

    actual fun getStoreId(): String? = prefs.getString("store_id", null)

    actual fun clearStoreId() {
        prefs.edit().remove("store_id").apply()
    }
```

Update `clearTokens()` to also clear store_id:
```kotlin
    actual fun clearTokens() {
        prefs.edit().clear().apply()
    }
```

- [ ] **Step 3: Implement iOS actual storeId methods**

Add to `shared/src/iosMain/kotlin/com/dokonpro/shared/data/local/TokenStorage.ios.kt`:
```kotlin
    actual fun saveStoreId(storeId: String) {
        defaults.setObject(storeId, "store_id")
    }

    actual fun getStoreId(): String? = defaults.stringForKey("store_id")

    actual fun clearStoreId() {
        defaults.removeObjectForKey("store_id")
    }
```

- [ ] **Step 4: Update VerifyOtpResponse DTO**

In `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/AuthDtos.kt`, update:
```kotlin
@Serializable
data class VerifyOtpResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
    val storeId: String? = null
)
```

- [ ] **Step 5: Update AuthRepositoryImpl to save storeId**

In `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/AuthRepositoryImpl.kt`:

In `verifyOtp()`, after saving tokens add:
```kotlin
        response.storeId?.let { tokenStorage.saveStoreId(it) }
```

In `register()`, after saving tokens add:
```kotlin
        tokenStorage.saveStoreId(response.store.id)
```

In `logout()`, add:
```kotlin
        tokenStorage.clearStoreId()
```

- [ ] **Step 6: Create SessionProvider**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/SessionProvider.kt`:
```kotlin
package com.dokonpro.shared.data.local

class SessionProvider(private val tokenStorage: TokenStorage) {
    val storeId: String
        get() = tokenStorage.getStoreId() ?: "no-store"

    val isLoggedIn: Boolean
        get() = tokenStorage.hasTokens() && tokenStorage.getStoreId() != null
}
```

- [ ] **Step 7: Register SessionProvider in Koin**

Add to `shared/src/commonMain/kotlin/com/dokonpro/shared/di/AuthModule.kt`:
```kotlin
    single { SessionProvider(get()) }
```

- [ ] **Step 8: Build, test, commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
git add shared/src/
git commit -m "fix(auth): extend TokenStorage with storeId, add SessionProvider"
```

---

## Task 3: Android — Replace hardcoded "default-store" with SessionProvider

**Files:**
- Modify: `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`

- [ ] **Step 1: Replace all "default-store" with SessionProvider**

In `DokonProApp.kt`, replace all ViewModel registrations that use `"default-store"`:

```kotlin
viewModel { ProductViewModel(get(), get(), get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { POSViewModel(get(), get(), get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { CustomerViewModel(get(), get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { FinanceViewModel(get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { StaffViewModel(get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { ZakatViewModel(get(), get(), get(), get(), get<SessionProvider>().storeId) }
viewModel { SettingsViewModel(get(), get(), get(), get<SessionProvider>().storeId) }
```

Add import: `import com.dokonpro.shared.data.local.SessionProvider`

- [ ] **Step 2: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "fix(auth): replace hardcoded default-store with SessionProvider.storeId"
```

---

## Task 4: Android — Product Edit Flow

**Files:**
- Modify: `androidApp/src/main/java/com/dokonpro/android/ui/products/AddEditProductScreen.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/viewmodel/ProductViewModel.kt`

- [ ] **Step 1: Add product parameter to AddEditProductScreen**

Modify `AddEditProductScreen` signature to accept an optional product for editing:

```kotlin
@Composable
fun AddEditProductScreen(
    product: Product? = null,
    onSave: (name: String, barcode: String?, price: Double, costPrice: Double, quantity: Int, unit: String, categoryId: String?) -> Unit,
    onBack: () -> Unit
)
```

Initialize state with product values if editing:
```kotlin
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "шт") }
```

Change title based on mode:
```kotlin
title = { Text(if (product != null) stringResource(R.string.product_edit) else stringResource(R.string.product_add)) }
```

- [ ] **Step 2: Add updateExistingProduct method to ProductViewModel**

Add to `ProductViewModel`:
```kotlin
    fun updateExistingProduct(product: Product) {
        viewModelScope.launch {
            updateProduct(product)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }
```

- [ ] **Step 3: Add EDIT_PRODUCT route and wire navigation**

Add to Routes:
```kotlin
const val EDIT_PRODUCT = "products/{productId}/edit"
```

Add composable in NavHost:
```kotlin
composable(Routes.EDIT_PRODUCT) { backStackEntry ->
    val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
    val viewModel: ProductViewModel = koinViewModel()
    val product = viewModel.state.value.products.find { it.id == productId }
    AddEditProductScreen(
        product = product,
        onSave = { name, barcode, price, costPrice, quantity, unit, categoryId ->
            if (product != null) {
                viewModel.updateExistingProduct(product.copy(
                    name = name, barcode = barcode, price = price, costPrice = costPrice,
                    quantity = quantity, unit = unit, categoryId = categoryId,
                    updatedAt = java.time.Instant.now().toString()
                ))
            }
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() }
    )
}
```

Wire ProductDetailScreen onEdit:
```kotlin
onEdit = { navController.navigate("products/$productId/edit") },
```

- [ ] **Step 4: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "feat(products): implement product edit flow with pre-populated form"
```

---

## Task 5: Android — Customer Edit Flow

**Files:**
- Modify: `androidApp/src/main/java/com/dokonpro/android/ui/customers/AddEditCustomerScreen.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/viewmodel/CustomerViewModel.kt`

- [ ] **Step 1: Add customer parameter to AddEditCustomerScreen**

Modify signature:
```kotlin
@Composable
fun AddEditCustomerScreen(
    customer: Customer? = null,
    onSave: (name: String, phone: String?, email: String?, notes: String?) -> Unit,
    onBack: () -> Unit
)
```

Initialize state:
```kotlin
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
```

Change title:
```kotlin
title = { Text(if (customer != null) stringResource(R.string.customer_detail) else stringResource(R.string.customer_add)) }
```

- [ ] **Step 2: Add updateExistingCustomer to CustomerViewModel**

```kotlin
    fun updateExistingCustomer(customer: Customer) {
        viewModelScope.launch {
            updateCustomer(customer)
                .onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }
```

- [ ] **Step 3: Add EDIT_CUSTOMER route and wire navigation**

Add to Routes:
```kotlin
const val EDIT_CUSTOMER = "customers/{customerId}/edit"
```

Add composable:
```kotlin
composable(Routes.EDIT_CUSTOMER) { backStackEntry ->
    val customerId = backStackEntry.arguments?.getString("customerId") ?: return@composable
    val viewModel: CustomerViewModel = koinViewModel()
    val customer = viewModel.listState.value.customers.find { it.id == customerId }
    AddEditCustomerScreen(
        customer = customer,
        onSave = { name, phone, email, notes ->
            if (customer != null) {
                viewModel.updateExistingCustomer(customer.copy(
                    name = name, phone = phone, email = email, notes = notes,
                    updatedAt = java.time.Instant.now().toString()
                ))
            }
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() }
    )
}
```

Wire CustomerDetailScreen onEdit:
```kotlin
onEdit = { navController.navigate("customers/$customerId/edit") },
```

- [ ] **Step 4: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "feat(customers): implement customer edit flow with pre-populated form"
```

---

## Task 6: KMP — ReceiptFormatter

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/ReceiptFormatter.kt`
- Create: `shared/src/commonTest/kotlin/com/dokonpro/shared/data/local/ReceiptFormatterTest.kt`

- [ ] **Step 1: Create ReceiptFormatter**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/ReceiptFormatter.kt`:
```kotlin
package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings

object ReceiptFormatter {
    private const val LINE_WIDTH = 32

    fun format(sale: Sale, settings: StoreSettings?): String {
        val sb = StringBuilder()
        val divider = "=".repeat(LINE_WIDTH)
        val thinDivider = "-".repeat(LINE_WIDTH)

        // Header
        sb.appendLine(divider)
        sb.appendLine(center(settings?.name ?: "DokonPro"))
        settings?.address?.let { sb.appendLine(center(it)) }
        settings?.phone?.let { sb.appendLine(center(it)) }
        sb.appendLine(divider)

        // Receipt info
        sb.appendLine("Чек № ${sale.id.takeLast(8).uppercase()}")
        sb.appendLine("Дата: ${sale.createdAt.take(19).replace("T", " ")}")
        sb.appendLine(thinDivider)

        // Items
        for (item in sale.items) {
            val total = item.price * item.quantity
            val namePart = item.name.take(18)
            val qtyPart = "x${item.quantity}"
            val pricePart = "%.2f".format(total)
            val spacesNeeded = LINE_WIDTH - namePart.length - qtyPart.length - pricePart.length - 2
            val spaces = if (spacesNeeded > 0) " ".repeat(spacesNeeded) else " "
            sb.appendLine("$namePart $qtyPart$spaces$pricePart")
        }

        sb.appendLine(thinDivider)

        // Totals
        if (sale.discount > 0) {
            sb.appendLine(rightAlign("Скидка:", "-%.2f".format(sale.discount)))
        }
        sb.appendLine(rightAlign("ИТОГО:", "%.2f".format(sale.totalAmount)))
        sb.appendLine(rightAlign("Оплата:", sale.paymentMethod.name))
        sb.appendLine(thinDivider)

        // QR content
        sb.appendLine(center("sale:${sale.id}"))

        // Footer
        val footer = settings?.receiptFooter ?: "Спасибо за покупку!"
        sb.appendLine(center(footer))
        sb.appendLine(divider)

        return sb.toString()
    }

    fun qrContent(sale: Sale): String = "sale:${sale.id}"

    private fun center(text: String): String {
        val trimmed = text.take(LINE_WIDTH)
        val padding = (LINE_WIDTH - trimmed.length) / 2
        return if (padding > 0) " ".repeat(padding) + trimmed else trimmed
    }

    private fun rightAlign(label: String, value: String): String {
        val spaces = LINE_WIDTH - label.length - value.length
        return if (spaces > 0) "$label${" ".repeat(spaces)}$value" else "$label $value"
    }
}
```

- [ ] **Step 2: Create tests**

Create `shared/src/commonTest/kotlin/com/dokonpro/shared/data/local/ReceiptFormatterTest.kt`:
```kotlin
package com.dokonpro.shared.data.local

import com.dokonpro.shared.domain.entity.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ReceiptFormatterTest {
    private val sale = Sale(
        id = "sale-12345678-abcd",
        totalAmount = 32.0,
        discount = 5.0,
        paymentMethod = PaymentMethod.CASH,
        customerId = null,
        storeId = "store-1",
        isRefunded = false,
        createdAt = "2026-04-08T14:32:00",
        items = listOf(
            SaleItem("si1", "sale-12345678-abcd", "p1", "Coca-Cola 1.5л", 2, 12.5, 0.0),
            SaleItem("si2", "sale-12345678-abcd", "p2", "Нон", 3, 3.0, 0.0)
        )
    )

    private val settings = StoreSettings(
        id = "s1", name = "Магазин Барака", address = "ул. Рудаки 45",
        phone = "+992 90 123 4567", currency = "TJS", logoUrl = null,
        receiptHeader = null, receiptFooter = "Рахмат!", storeId = "store-1"
    )

    @Test
    fun `should contain store name`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertTrue(receipt.contains("Магазин Барака"))
    }

    @Test
    fun `should contain sale items`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertTrue(receipt.contains("Coca-Cola"))
        assertTrue(receipt.contains("Нон"))
    }

    @Test
    fun `should contain total`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertTrue(receipt.contains("32.00"))
    }

    @Test
    fun `should contain discount`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertTrue(receipt.contains("-5.00"))
    }

    @Test
    fun `should contain custom footer`() {
        val receipt = ReceiptFormatter.format(sale, settings)
        assertTrue(receipt.contains("Рахмат!"))
    }

    @Test
    fun `should generate QR content`() {
        assertEquals("sale:sale-12345678-abcd", ReceiptFormatter.qrContent(sale))
    }
}
```

- [ ] **Step 3: Run tests and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
git add shared/src/
git commit -m "feat(print): add ReceiptFormatter with ESC/POS text layout and tests"
```

---

## Task 7: Android — EscPosCommands + BluetoothPrinterService

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/service/EscPosCommands.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/service/BluetoothPrinterService.kt`
- Modify: `androidApp/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create EscPosCommands**

Create `androidApp/src/main/java/com/dokonpro/android/service/EscPosCommands.kt`:
```kotlin
package com.dokonpro.android.service

object EscPosCommands {
    val INIT = byteArrayOf(0x1B, 0x40)                    // ESC @ — initialize printer
    val LINE_FEED = byteArrayOf(0x0A)                      // LF
    val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x00)         // GS V 0 — full cut
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)            // ESC E 1
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)           // ESC E 0
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)       // ESC a 1
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)         // ESC a 0
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)        // ESC a 2
    val FONT_NORMAL = byteArrayOf(0x1B, 0x21, 0x00)        // ESC ! 0
    val FONT_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10) // ESC ! 16
    val FEED_LINES_3 = byteArrayOf(0x1B, 0x64, 0x03)       // ESC d 3

    fun printQrCode(content: String): ByteArray {
        val data = content.toByteArray(Charsets.UTF_8)
        val storeLen = data.size + 3
        val pL = (storeLen % 256).toByte()
        val pH = (storeLen / 256).toByte()
        return byteArrayOf(
            // QR model
            0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00,
            // QR size (6 dots)
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06,
            // QR error correction L
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30,
            // Store data
            0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30
        ) + data + byteArrayOf(
            // Print QR
            0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30
        )
    }

    fun textToBytes(text: String): ByteArray = text.toByteArray(charset("CP866"))
}
```

- [ ] **Step 2: Create BluetoothPrinterService**

Create `androidApp/src/main/java/com/dokonpro/android/service/BluetoothPrinterService.kt`:
```kotlin
package com.dokonpro.android.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.SharedPreferences
import com.dokonpro.shared.data.local.ReceiptFormatter
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothPrinterService(private val context: Context) {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val prefs: SharedPreferences = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun getPairedDevices(): List<BluetoothDevice> =
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

    fun getSavedPrinterAddress(): String? = prefs.getString("printer_mac", null)

    fun savePrinterAddress(mac: String) {
        prefs.edit().putString("printer_mac", mac).apply()
    }

    fun clearSavedPrinter() {
        prefs.edit().remove("printer_mac").apply()
    }

    suspend fun printReceipt(sale: Sale, settings: StoreSettings?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val mac = getSavedPrinterAddress() ?: throw IllegalStateException("No printer saved")
            val device = bluetoothAdapter?.getRemoteDevice(mac)
                ?: throw IllegalStateException("Bluetooth device not found")

            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                socket.connect()
                val output: OutputStream = socket.outputStream

                // Init printer
                output.write(EscPosCommands.INIT)

                // Header — centered, bold
                output.write(EscPosCommands.ALIGN_CENTER)
                output.write(EscPosCommands.BOLD_ON)
                output.write(EscPosCommands.FONT_DOUBLE_HEIGHT)
                output.write(EscPosCommands.textToBytes(settings?.name ?: "DokonPro"))
                output.write(EscPosCommands.LINE_FEED)
                output.write(EscPosCommands.FONT_NORMAL)
                output.write(EscPosCommands.BOLD_OFF)

                settings?.address?.let {
                    output.write(EscPosCommands.textToBytes(it))
                    output.write(EscPosCommands.LINE_FEED)
                }
                settings?.phone?.let {
                    output.write(EscPosCommands.textToBytes(it))
                    output.write(EscPosCommands.LINE_FEED)
                }

                // Receipt body — left aligned
                output.write(EscPosCommands.ALIGN_LEFT)
                val receiptText = ReceiptFormatter.format(sale, settings)
                // Skip header lines (already printed with formatting), print from receipt number
                val bodyLines = receiptText.lines().dropWhile { !it.startsWith("Чек") }
                for (line in bodyLines) {
                    if (line.contains("sale:")) {
                        // Print QR code instead of text
                        output.write(EscPosCommands.ALIGN_CENTER)
                        output.write(EscPosCommands.printQrCode(ReceiptFormatter.qrContent(sale)))
                        output.write(EscPosCommands.LINE_FEED)
                    } else {
                        output.write(EscPosCommands.textToBytes(line))
                        output.write(EscPosCommands.LINE_FEED)
                    }
                }

                // Feed and cut
                output.write(EscPosCommands.FEED_LINES_3)
                output.write(EscPosCommands.CUT_PAPER)
                output.flush()
            } finally {
                socket.close()
            }
        }
    }
}
```

- [ ] **Step 3: Add Bluetooth permissions to AndroidManifest**

Add before `<application>` in `androidApp/src/main/AndroidManifest.xml`:
```xml
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

- [ ] **Step 4: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add androidApp/
git commit -m "feat(print): add ESC/POS commands and BluetoothPrinterService"
```

---

## Task 8: Android — PrinterViewModel + PrinterSettingsScreen + Strings

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/viewmodel/PrinterViewModel.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/settings/PrinterSettingsScreen.kt`
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values-tg/strings.xml`

- [ ] **Step 1: Create PrinterViewModel**

Create `androidApp/src/main/java/com/dokonpro/android/viewmodel/PrinterViewModel.kt`:
```kotlin
package com.dokonpro.android.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.android.service.BluetoothPrinterService
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.StoreSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrinterState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val savedAddress: String? = null,
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val error: String? = null
)

class PrinterViewModel(
    private val printerService: BluetoothPrinterService
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterState())
    val state: StateFlow<PrinterState> = _state.asStateFlow()

    init { loadState() }

    fun loadState() {
        _state.value = _state.value.copy(
            savedAddress = printerService.getSavedPrinterAddress(),
            pairedDevices = if (printerService.isBluetoothEnabled()) printerService.getPairedDevices() else emptyList()
        )
    }

    fun selectPrinter(device: BluetoothDevice) {
        printerService.savePrinterAddress(device.address)
        _state.value = _state.value.copy(savedAddress = device.address)
    }

    fun clearPrinter() {
        printerService.clearSavedPrinter()
        _state.value = _state.value.copy(savedAddress = null)
    }

    fun printReceipt(sale: Sale, settings: StoreSettings?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPrinting = true, printSuccess = false, error = null)
            printerService.printReceipt(sale, settings)
                .onSuccess { _state.value = _state.value.copy(isPrinting = false, printSuccess = true) }
                .onFailure { _state.value = _state.value.copy(isPrinting = false, error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearPrintSuccess() { _state.value = _state.value.copy(printSuccess = false) }
}
```

- [ ] **Step 2: Create PrinterSettingsScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/settings/PrinterSettingsScreen.kt` — shows:
- Bluetooth status (enabled/disabled)
- Currently saved printer (if any) with disconnect button
- List of paired Bluetooth devices to select from
- "Test print" button that prints a test receipt
- Back arrow TopAppBar "Bluetooth принтер"

Follow existing screen patterns (TopAppBar, Surface cards, LazyColumn).

- [ ] **Step 3: Add printer strings**

Russian (add before `</resources>`):
```xml
    <!-- Printer -->
    <string name="printer_title">Bluetooth принтер</string>
    <string name="printer_no_bluetooth">Bluetooth выключен</string>
    <string name="printer_saved">Подключён</string>
    <string name="printer_none">Принтер не выбран</string>
    <string name="printer_select">Выберите принтер</string>
    <string name="printer_disconnect">Отключить</string>
    <string name="printer_test">Тест печати</string>
    <string name="printer_printing">Печать...</string>
    <string name="printer_success">Напечатано!</string>
    <string name="printer_error">Ошибка печати</string>
    <string name="printer_print_receipt">Печать чека</string>
```

Tajik:
```xml
    <!-- Printer -->
    <string name="printer_title">Принтери Bluetooth</string>
    <string name="printer_no_bluetooth">Bluetooth хомӯш аст</string>
    <string name="printer_saved">Пайваст шуд</string>
    <string name="printer_none">Принтер интихоб нашудааст</string>
    <string name="printer_select">Принтерро интихоб кунед</string>
    <string name="printer_disconnect">Ҷудо кардан</string>
    <string name="printer_test">Санҷиши чоп</string>
    <string name="printer_printing">Чоп шуда истодааст...</string>
    <string name="printer_success">Чоп шуд!</string>
    <string name="printer_error">Хатогии чоп</string>
    <string name="printer_print_receipt">Чопи чек</string>
```

- [ ] **Step 4: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add androidApp/
git commit -m "feat(print): add PrinterViewModel, PrinterSettingsScreen, and printer strings"
```

---

## Task 9: Android — Wire Print Buttons + Printer Navigation + DI

**Files:**
- Modify: `androidApp/src/main/java/com/dokonpro/android/ui/pos/ReceiptScreen.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/ui/sales/SalesHistoryScreen.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`

- [ ] **Step 1: Add print button to ReceiptScreen**

Add `onPrint: () -> Unit` parameter to `ReceiptScreen`. Add an OutlinedButton "Печать чека" with printer icon between the "Новая продажа" button and back button.

- [ ] **Step 2: Add print icon to SalesHistoryScreen**

Add `onPrintSale: (String) -> Unit` parameter. On each sale item in the LazyColumn, add an IconButton with print icon that calls `onPrintSale(sale.id)`.

- [ ] **Step 3: Add PRINTER_SETTINGS route and wire everything**

Add to Routes:
```kotlin
const val PRINTER_SETTINGS = "settings/printer"
```

Wire in NavHost: PrinterSettingsScreen composable, ReceiptScreen print callback (using PrinterViewModel), SalesHistoryScreen print callback.

In SettingsScreen, replace the disabled printer button with navigation to `PRINTER_SETTINGS`.

- [ ] **Step 4: Register DI**

In DokonProApp.kt:
```kotlin
single { BluetoothPrinterService(get()) }
viewModel { PrinterViewModel(get()) }
```

Import `com.dokonpro.android.service.BluetoothPrinterService`.

- [ ] **Step 5: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "feat(print): wire print buttons in Receipt and SalesHistory, add printer navigation"
```

---

## Task 10: Android — Sync Status Icon in MainScreen

**Files:**
- Modify: `androidApp/src/main/java/com/dokonpro/android/ui/MainScreen.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`

- [ ] **Step 1: Add SyncStatus to MainScreen**

Add parameters to MainScreen:
```kotlin
    syncState: SyncState = SyncState.IDLE,
    pendingCount: Long = 0,
```

Replace the notification IconButton with:
```kotlin
if (pendingCount > 0) {
    BadgedBox(
        badge = { Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("$pendingCount") } }
    ) {
        Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.onPrimary)
    }
    Spacer(Modifier.width(12.dp))
} else if (syncState == SyncState.SYNCING) {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
    )
    Spacer(Modifier.width(12.dp))
}
```

Add imports for `SyncState`, `Badge`, `BadgedBox`, `Sync` icon.

- [ ] **Step 2: Wire SyncStatus in AppNavigation**

In the MAIN composable, get SyncStatus from Koin and pass to MainScreen:
```kotlin
composable(Routes.MAIN) {
    val syncStatus = org.koin.java.KoinJavaComponent.get<com.dokonpro.shared.data.sync.SyncStatus>(com.dokonpro.shared.data.sync.SyncStatus::class.java)
    val syncData by syncStatus.status.collectAsStateWithLifecycle()
    MainScreen(
        syncState = syncData.state,
        pendingCount = syncData.pendingCount,
        onNavigateToPOS = { navController.navigate(Routes.POS) },
        ...existing callbacks...
    )
}
```

- [ ] **Step 3: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "fix(ui): replace notification icon with sync status indicator"
```

---

## Task 11: Build Verification + Tag + Push

- [ ] **Step 1: Run all tests**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug
cd api && npm run build
```

- [ ] **Step 2: Tag and push**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git tag v1.1.0
git push origin main --tags
```
