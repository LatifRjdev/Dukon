# DokonPro v1.1.0 — Fix Placeholders + Bluetooth Printing

**Date:** 2026-04-09
**Tag:** v1.1.0
**Scope:** Fix critical stubs found in MVP audit + add Bluetooth receipt printing

---

## Decisions

- **StoreId from session** — no more hardcoded "default-store"
- **Edit flows** — reuse existing Add screens with pre-populated fields
- **Bluetooth printing** — ESC/POS protocol, 58mm thermal printers, QR code on receipt
- **Notification icon** → replaced with sync status indicator
- **Backend changes** — minimal: add storeId to verify-otp response

---

## 1. StoreId from Session

### Problem
All 7 ViewModels receive hardcoded `"default-store"` via Koin. No data persists to the real store.

### Solution

**Backend:**
- Modify `POST /auth/verify-otp` response — when user exists (isNewUser=false), include `storeId` from the first UserStore record
- `POST /auth/register` already returns `store.id` — no change needed

**KMP Shared:**
- Extend `TokenStorage` — add `saveStoreId(storeId: String)`, `getStoreId(): String?`, `clearStoreId()` (alongside existing token methods)
- Android actual: store in same EncryptedSharedPreferences
- iOS actual: store in same NSUserDefaults
- `AuthRepositoryImpl.verifyOtp()` — save storeId from response
- `AuthRepositoryImpl.register()` — save storeId from response
- `AuthRepositoryImpl.logout()` — clear storeId

**Android:**
- Create `SessionProvider` class — reads storeId from TokenStorage, provides it to ViewModels
- Register as Koin singleton
- All 7 ViewModel registrations in DokonProApp: replace `"default-store"` with `get<SessionProvider>().storeId`

---

## 2. Product Edit Flow

### Problem
`ProductDetailScreen` onEdit callback is `{ /* edit flow later */ }`.

### Solution
- Modify `AddEditProductScreen` — add optional `product: Product?` parameter
- When product is not null: pre-populate all fields, change title to "Редактировать товар"
- On save: call `UpdateProductUseCase` (already exists) instead of `CreateProductUseCase`
- Add `EDIT_PRODUCT = "products/{productId}/edit"` route
- Wire `ProductDetailScreen` onEdit → navigate to EDIT_PRODUCT
- `ProductViewModel` already has update logic — just needs wiring

---

## 3. Customer Edit Flow

### Problem
`CustomerDetailScreen` onEdit callback is `{ /* edit flow later */ }`.

### Solution
- Modify `AddEditCustomerScreen` — add optional `customer: Customer?` parameter
- When customer is not null: pre-populate name, phone, email, notes, change title to "Редактировать клиента"
- On save: call `UpdateCustomerUseCase` (already exists)
- Add `EDIT_CUSTOMER = "customers/{customerId}/edit"` route
- Wire `CustomerDetailScreen` onEdit → navigate to EDIT_CUSTOMER

---

## 4. Bluetooth Receipt Printing

### Architecture

**KMP Shared (pure Kotlin, testable):**
- `ReceiptFormatter` — takes `Sale` + `StoreSettings`, returns formatted receipt text (32 chars per line for 58mm)

**Android (platform-specific):**
- `EscPosCommands` — byte sequences for ESC/POS protocol (init, bold, center, cut, QR)
- `BluetoothPrinterService` — scan paired devices, connect via Bluetooth Classic RFCOMM, send bytes
- `PrinterViewModel` — manages connection state, printer selection, print jobs
- `PrinterSettingsScreen` — UI for scanning/selecting/testing printer

### ESC/POS Receipt Template (58mm = 32 chars)
```
================================
      {storeName}
      {storeAddress}
      {storePhone}
================================
Чек № {saleId last 8 chars}
Дата: {date} {time}
--------------------------------
{productName}    x{qty}  {total}
...
--------------------------------
Подытог:              {subtotal}
Скидка:               -{discount}
ИТОГО:                {total}
Оплата: {paymentMethod}
--------------------------------
        [QR: sale:{saleId}]
    {receiptFooter}
================================
```

### Receipt Header/Footer
- Uses `StoreSettings.receiptHeader` / `StoreSettings.receiptFooter` from Settings
- If empty, defaults: header = store name, footer = "Спасибо за покупку!"

### Print Triggers
- `ReceiptScreen` — "Печать чека" button after sale completion
- `SalesHistoryScreen` — print icon on each sale item for reprinting

### Bluetooth Flow
1. User goes to Settings → Bluetooth Printer
2. App shows list of paired Bluetooth devices (filtered by printer-like names or SPP UUID)
3. User selects printer → MAC address saved to SharedPreferences
4. On print: connect to saved MAC → send ESC/POS bytes → disconnect
5. Error handling: show toast if printer not found/disconnected

### Permissions
- `BLUETOOTH_CONNECT` (Android 12+)
- `BLUETOOTH_SCAN` (Android 12+)
- Runtime permission request before scanning

### Files
- `shared/.../domain/entity/ReceiptData.kt` — data class for receipt content
- `shared/.../data/local/ReceiptFormatter.kt` — formats receipt text from Sale + StoreSettings
- `shared/src/commonTest/.../ReceiptFormatterTest.kt` — unit tests
- `androidApp/.../service/EscPosCommands.kt` — ESC/POS byte constants and builders
- `androidApp/.../service/BluetoothPrinterService.kt` — Bluetooth Classic connection + print
- `androidApp/.../viewmodel/PrinterViewModel.kt` — state management for printer
- `androidApp/.../ui/settings/PrinterSettingsScreen.kt` — printer scan/select UI
- Modify: `ReceiptScreen.kt` — add print button
- Modify: `SalesHistoryScreen.kt` — add print icon per sale
- Modify: `AndroidManifest.xml` — add Bluetooth permissions
- Strings: ru + tg for printer UI

---

## 5. Sync Status Icon (replaces Notification)

### Problem
Notification bell icon in MainScreen has empty onClick — no notification system exists.

### Solution
- Replace notification IconButton with sync status indicator
- Read `SyncStatus` from Koin (already exists as singleton)
- If `pendingCount > 0` → show sync icon with amber badge showing count
- If `state == SYNCING` → show animated sync icon
- If `state == IDLE` → show nothing (clean header)
- Pass SyncStatus to MainScreen via Koin injection

---

## Summary of Changes

| Area | What | Files Changed |
|------|------|---------------|
| Backend | Add storeId to verify-otp response | 1 (auth.service.ts) |
| KMP TokenStorage | Add storeId persistence | 3 (common + android + ios) |
| KMP Session | SessionProvider | 1 new |
| KMP DI | Wire SessionProvider | 1 (SharedModule or new) |
| Android DI | Replace "default-store" | 1 (DokonProApp.kt) |
| Product Edit | Modify screen + navigation | 3 |
| Customer Edit | Modify screen + navigation | 3 |
| Receipt Formatter | KMP shared | 2 (formatter + test) |
| Bluetooth Printer | Android service | 4 new |
| Printer UI | Settings screen | 1 new + 1 modified |
| Print buttons | Receipt + History screens | 2 modified |
| Sync Status | MainScreen | 1 modified |
| Strings | ru + tg | 2 modified |
| Manifest | Bluetooth permissions | 1 modified |
| **Total** | | **~25 files** |
