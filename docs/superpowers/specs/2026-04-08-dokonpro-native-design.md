# DokonPro Native — Full Project Design

**Date:** 2026-04-08
**Approach:** Vertical Slice (full stack per feature module)
**Platforms:** Android first, iOS later
**Languages:** Russian (default) + Tajik
**Backend:** NestJS + Prisma + PostgreSQL + Redis (Docker Compose)

---

## Decisions

- **Vertical Slice approach** — each module is built end-to-end (Backend → Shared KMP → Android) before moving to the next
- **Android first** — iOS added later using same shared KMP layer
- **Offline-first** — all writes go to local SQLDelight DB, then sync via queue
- **Mock OTP in dev** — code is always `123456` during development
- **Cart in memory** — not persisted to DB, lives only during POS session
- **Last-write-wins** — conflict resolution by server timestamp comparison

---

## Module Order

1. Foundation (project scaffolding)
2. Auth
3. Products (+ Sync Engine)
4. POS / Sales
5. Customers / CRM
6. Finance
7. Staff
8. Zakat
9. Settings

---

## 1. Foundation

### Git
- Initialize repo with `.gitignore` for Kotlin, Swift, Node.js, IDE files

### Gradle
- `settings.gradle.kts` — includes `:shared`, `:androidApp`
- `gradle/libs.versions.toml` — version catalog for all dependencies
- Kotlin 2.0+, Android Gradle Plugin, KMP plugin

### KMP Shared Module (`shared/`)
- `commonMain` packages: `domain/entity`, `domain/repository`, `domain/usecase`, `data/remote`, `data/local`, `data/sync`, `di`
- `androidMain` — platform implementations (initially empty)
- `iosMain` — stubs for later
- Dependencies: Ktor Client, SQLDelight, Koin, kotlinx.serialization, kotlinx.coroutines

### Android App (`androidApp/`)
- Minimal Compose app with Material 3
- Compose Navigation
- Koin DI connected to shared module
- Empty MainScreen as entry point

### Backend (`api/`)
- NestJS project
- Prisma + PostgreSQL
- Docker Compose: Postgres 16 + Redis 7
- Base modules: `auth`, `stores`, `health`
- Health check: `GET /health`

### Design System
- Material 3 theme with retail/fintech colors
- Cyrillic typography support (ru, tg)
- String resources: `ru` (default) + `tg`

---

## 2. Auth Flow

### Backend (`api/src/modules/auth/`)
- `POST /auth/send-otp` — send OTP to phone (+992...). Dev mode: always `123456`
- `POST /auth/verify-otp` — verify code, return JWT (access 15min + refresh 7d)
- `POST /auth/refresh` — refresh tokens
- `POST /auth/register` — create user + store after OTP verification
- `JwtAuthGuard` for protected endpoints
- Prisma models: `User`, `Store`, `UserStore` (many-to-many)

### Shared KMP
- **Entities:** `User`, `Store`, `AuthTokens`
- **Use Cases:** `SendOtpUseCase`, `VerifyOtpUseCase`, `RefreshTokenUseCase`, `RegisterUseCase`, `LogoutUseCase`
- **Repository:** `AuthRepository` (interface in domain, impl in data)
- **Remote:** Ktor `AuthApiClient`
- **Local:** `TokenStorage` via `expect/actual` — secure per-platform storage
- **DI:** `authModule` for Koin

### Android
- **PhoneInputScreen** — phone input with +992 mask, "Get code" button
- **OtpScreen** — 6-digit input, 60s timer, "Confirm" button
- **RegisterScreen** — username + store name (first login only)
- **AuthViewModel** — manages auth flow state and navigation
- Token storage: `EncryptedSharedPreferences`
- Success → navigate to MainScreen

---

## 3. Products

### Backend (`api/src/modules/products/`)
- `GET /stores/:storeId/products` — paginated list, search by name/barcode
- `POST /stores/:storeId/products` — create
- `PATCH /stores/:storeId/products/:id` — update
- `DELETE /stores/:storeId/products/:id` — soft delete
- `POST /stores/:storeId/products/import` — Excel import
- Prisma models: `Product` (name, barcode, sku, price, costPrice, quantity, unit, categoryId, imageUrl, storeId, isDeleted, createdAt, updatedAt), `Category` (name, storeId)

### Shared KMP
- **Entities:** `Product`, `Category`
- **Use Cases:** `GetProductsUseCase`, `CreateProductUseCase`, `UpdateProductUseCase`, `DeleteProductUseCase`, `SearchProductUseCase`, `ScanBarcodeUseCase`
- **Repository:** `ProductRepository` — offline-first: read from SQLDelight, write to local + SyncQueue
- **SQLDelight:** `product.sq`, `category.sq` — indexes on `store_id`, `barcode`

### Sync Engine (first real implementation)
- SQLDelight table `sync_queue`: entity_type, entity_id, operation (CREATE/UPDATE/DELETE), payload, retry_count, status, created_at
- `SyncManager` — listens to connectivity, processes queue FIFO
- Exponential backoff, max 5 retries
- Last-write-wins conflict resolution by server timestamp

### Android
- **ProductListScreen** — product list with search, pull-to-refresh, FAB
- **ProductDetailScreen** — view/edit product
- **AddProductScreen** — form: name, barcode (manual or scanner), price, cost price, quantity, unit, category
- **BarcodeScannerScreen** — camera scanning (ML Kit or ZXing)
- **ProductViewModel**
- Offline indicator: sync icon in toolbar

---

## 4. POS / Sales

### Backend (`api/src/modules/sales/`)
- `POST /stores/:storeId/sales` — create sale (items array + payment info)
- `GET /stores/:storeId/sales` — history with pagination, date filter
- `GET /stores/:storeId/sales/:id` — sale details
- `POST /stores/:storeId/sales/:id/refund` — full or partial refund
- Prisma models: `Sale` (totalAmount, discount, paymentMethod, customerId, storeId, createdAt), `SaleItem` (productId, quantity, price, discount)
- Creating a sale auto-decrements `Product.quantity`

### Shared KMP
- **Entities:** `Sale`, `SaleItem`, `Cart`, `CartItem`, `PaymentMethod` (enum: CASH, CARD, MIXED)
- **Use Cases:** `AddToCartUseCase`, `RemoveFromCartUseCase`, `UpdateCartItemUseCase`, `CompleteSaleUseCase`, `GetSalesHistoryUseCase`, `RefundSaleUseCase`
- **Repository:** `SaleRepository` — offline-first
- **SQLDelight:** `sale.sq`, `sale_item.sq`
- Cart stored in memory only — lives within session

### Android
- **POSScreen** — main cashier screen:
  - Left: product search (by name or scan) + quick tiles for popular items
  - Right: current cart with quantities, prices, total
- **CartPanel** — cart items, +/- quantity, remove, per-item discount
- **CheckoutScreen** — total, payment method, sale-wide discount, "Pay" button
- **ReceiptScreen** — receipt after sale (Bluetooth print later)
- **SalesHistoryScreen** — sales list with date filter
- **POSViewModel**

---

## 5. Customers / CRM

### Backend (`api/src/modules/customers/`)
- `GET /stores/:storeId/customers` — list with search by name/phone
- `POST /stores/:storeId/customers` — create
- `PATCH /stores/:storeId/customers/:id` — update
- `GET /stores/:storeId/customers/:id/purchases` — purchase history
- Prisma model: `Customer` (name, phone, email, notes, totalSpent, visitCount, storeId, createdAt)
- Relation: `Sale.customerId -> Customer.id`

### Shared KMP
- **Entities:** `Customer`
- **Use Cases:** `GetCustomersUseCase`, `CreateCustomerUseCase`, `UpdateCustomerUseCase`, `GetCustomerPurchasesUseCase`, `SearchCustomerUseCase`
- **Repository:** `CustomerRepository` — offline-first
- **SQLDelight:** `customer.sq` — indexes on `store_id`, `phone`

### Android
- **CustomerListScreen** — customer list, search, FAB
- **CustomerDetailScreen** — profile + purchase history + total spent
- **AddCustomerScreen** — form: name, phone, email, notes
- **CustomerViewModel**
- POS integration: attach customer to sale during checkout

---

## 6. Finance

### Backend (`api/src/modules/finance/`)
- `GET /stores/:storeId/finance/summary` — revenue, expenses, profit for period
- `GET /stores/:storeId/finance/transactions` — all transactions, filter by type/date
- `POST /stores/:storeId/finance/expenses` — add expense
- `GET /stores/:storeId/finance/reports` — daily/weekly/monthly reports
- Prisma models: `Transaction` (type: INCOME/EXPENSE, amount, description, categoryId, storeId, createdAt), `ExpenseCategory` (name, storeId)
- Sales automatically create INCOME transactions

### Shared KMP
- **Entities:** `Transaction`, `ExpenseCategory`, `FinanceSummary` (revenue, expenses, profit, period)
- **Use Cases:** `GetFinanceSummaryUseCase`, `GetTransactionsUseCase`, `AddExpenseUseCase`, `GetReportUseCase`
- **Repository:** `FinanceRepository` — offline-first
- **SQLDelight:** `transaction.sq`, `expense_category.sq`

### Android
- **FinanceDashboardScreen** — cards: revenue, expenses, profit for today/week/month
- **TransactionListScreen** — all transactions, filter by type and date
- **AddExpenseScreen** — form: amount, category, description, date
- **ReportScreen** — bar chart of revenue by day
- **FinanceViewModel**

---

## 7. Staff

### Backend (`api/src/modules/staff/`)
- `GET /stores/:storeId/staff` — staff list
- `POST /stores/:storeId/staff` — add staff (creates User + UserStore with role)
- `PATCH /stores/:storeId/staff/:id` — update role/status
- `DELETE /stores/:storeId/staff/:id` — deactivate
- Roles: `OWNER`, `MANAGER`, `CASHIER` via `UserStore.role`
- Permissions: Owner — all; Manager — all except delete store; Cashier — POS + view products only

### Shared KMP
- **Entities:** `Staff`, `Role` (enum)
- **Use Cases:** `GetStaffUseCase`, `AddStaffUseCase`, `UpdateStaffRoleUseCase`, `DeactivateStaffUseCase`
- **Repository:** `StaffRepository` — offline-first
- **SQLDelight:** `staff.sq`
- `PermissionManager` — checks current user's permissions, used by other use cases

### Android
- **StaffListScreen** — staff list with roles and status
- **AddStaffScreen** — phone + role (invitation via OTP)
- **StaffDetailScreen** — change role, deactivate
- **StaffViewModel**
- UI elements hidden/disabled based on current user's role

---

## 8. Zakat

### Backend (`api/src/modules/zakat/`)
- `GET /stores/:storeId/zakat/calculate` — calculate based on inventory + finances
- `GET /stores/:storeId/zakat/history` — calculation history
- `POST /stores/:storeId/zakat/save` — save calculation

### Calculation Logic
- Nisab threshold: equivalent of 85g gold or 595g silver in TJS
- Zakatable assets: inventory value (products x costPrice) + cash balance + receivables
- Deductions: liabilities/payables
- Rate: 2.5% of amount above nisab
- Gold/silver rate: manually configurable by admin

### Shared KMP
- **Entities:** `ZakatCalculation` (inventoryValue, cashBalance, receivables, liabilities, nisabThreshold, zakatableAmount, zakatDue, calculatedAt)
- **Use Cases:** `CalculateZakatUseCase`, `GetZakatHistoryUseCase`, `SaveZakatCalculationUseCase`
- **Repository:** `ZakatRepository`
- **SQLDelight:** `zakat_calculation.sq`

### Android
- **ZakatScreen** — current calculation: asset breakdown, nisab threshold, zakat amount due
- **ZakatHistoryScreen** — past calculations
- **ZakatSettingsScreen** — gold/silver rate input in TJS
- **ZakatViewModel**

---

## 9. Settings

### Backend (`api/src/modules/stores/` — extends existing)
- `GET /stores/:storeId` — store data
- `PATCH /stores/:storeId` — update store settings
- Settings: name, address, phone, currency (TJS default), logo, receipt template

### Shared KMP
- **Entities:** `StoreSettings` (name, address, phone, currency, logoUrl, receiptHeader, receiptFooter)
- **Use Cases:** `GetStoreSettingsUseCase`, `UpdateStoreSettingsUseCase`
- **Repository:** `StoreSettingsRepository`
- **SQLDelight:** `store_settings.sq`

### Android
- **SettingsScreen** — main settings:
  - Store profile (name, address, phone)
  - Language (Russian / Tajik)
  - Currency
  - Receipt header/footer
  - Bluetooth printer (scan + connect — stub, full implementation later)
  - About / version
- **EditStoreScreen** — store data edit form
- **SettingsViewModel**
- Language change restarts Activity with new locale
