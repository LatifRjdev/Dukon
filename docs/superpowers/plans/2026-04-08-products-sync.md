# Products + Sync Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the offline-first sync engine foundation and the Products vertical slice — from backend CRUD through shared KMP with SQLDelight persistence to Android Compose product management UI.

**Architecture:** Backend provides REST CRUD for products/categories scoped by storeId. KMP shared module stores products in SQLDelight for offline reads, writes go to local DB + sync queue. SyncManager processes the queue FIFO with exponential backoff. Android app shows product list with search, add/edit forms, and sync status indicator.

**Tech Stack:** NestJS 10, Prisma 6 | Kotlin 2.1.0, SQLDelight 2.0.2, Ktor 3.0.3, Koin 4.0.2, kotlinx.serialization | Jetpack Compose, Material 3

**Scope note:** Excel import and camera-based barcode scanning are deferred to a follow-up phase. This plan delivers manual barcode entry, CRUD, offline sync, and search.

---

## File Structure

```
api/
├── prisma/schema.prisma                           # MODIFY: add Product, Category
├── src/
│   ├── app.module.ts                              # MODIFY: add ProductsModule
│   └── modules/products/
│       ├── products.module.ts                     # CREATE
│       ├── products.controller.ts                 # CREATE: CRUD endpoints
│       ├── products.service.ts                    # CREATE: business logic
│       └── dto/
│           ├── create-product.dto.ts              # CREATE
│           ├── update-product.dto.ts              # CREATE
│           └── product-query.dto.ts               # CREATE: pagination + search

shared/
├── src/commonMain/
│   ├── sqldelight/com/dokonpro/shared/db/
│   │   ├── product.sq                             # CREATE: products table + queries
│   │   ├── category.sq                            # CREATE: categories table + queries
│   │   └── sync_queue.sq                          # CREATE: sync queue table + queries
│   └── kotlin/com/dokonpro/shared/
│       ├── domain/
│       │   ├── entity/Product.kt                  # CREATE
│       │   ├── entity/Category.kt                 # CREATE
│       │   ├── repository/ProductRepository.kt    # CREATE: interface
│       │   └── usecase/
│       │       ├── GetProductsUseCase.kt          # CREATE
│       │       ├── CreateProductUseCase.kt        # CREATE
│       │       ├── UpdateProductUseCase.kt        # CREATE
│       │       ├── DeleteProductUseCase.kt        # CREATE
│       │       └── SearchProductUseCase.kt        # CREATE
│       ├── data/
│       │   ├── remote/
│       │   │   ├── ProductApiClient.kt            # CREATE
│       │   │   └── dto/ProductDtos.kt             # CREATE
│       │   ├── local/
│       │   │   ├── ProductLocalDataSource.kt      # CREATE: SQLDelight wrapper
│       │   │   └── DatabaseFactory.kt             # CREATE: expect/actual for driver
│       │   ├── sync/
│       │   │   ├── SyncManager.kt                 # CREATE: queue processor
│       │   │   ├── SyncQueue.kt                   # CREATE: SQLDelight wrapper
│       │   │   └── SyncStatus.kt                  # CREATE: observable sync state
│       │   └── repository/
│       │       └── ProductRepositoryImpl.kt       # CREATE: offline-first
│       └── di/
│           ├── ProductModule.kt                   # CREATE: Koin bindings
│           ├── DatabaseModule.kt                  # CREATE: DB + sync Koin
│           └── SharedModule.kt                    # MODIFY: include new modules
├── src/androidMain/kotlin/.../data/local/
│   └── DatabaseFactory.android.kt                 # CREATE: Android SQLite driver
├── src/iosMain/kotlin/.../data/local/
│   └── DatabaseFactory.ios.kt                     # CREATE: iOS native driver
├── src/commonTest/kotlin/.../
│   ├── domain/usecase/ProductUseCaseTest.kt       # CREATE
│   └── data/sync/SyncQueueTest.kt                 # CREATE

androidApp/
├── src/main/java/com/dokonpro/android/
│   ├── ui/products/
│   │   ├── ProductListScreen.kt                   # CREATE
│   │   ├── ProductDetailScreen.kt                 # CREATE
│   │   └── AddEditProductScreen.kt                # CREATE
│   ├── viewmodel/ProductViewModel.kt              # CREATE
│   ├── navigation/AppNavigation.kt                # MODIFY: add product routes
│   └── DokonProApp.kt                             # MODIFY: add product DI
├── src/main/res/values/strings.xml                # MODIFY: add product strings
└── src/main/res/values-tg/strings.xml             # MODIFY: add product strings
```

---

## Task 1: Backend — Product + Category Prisma Models

**Files:**
- Modify: `api/prisma/schema.prisma`

- [ ] **Step 1: Add Category and Product models**

Append to `api/prisma/schema.prisma` after the OtpCode model:
```prisma
model Category {
  id        String    @id @default(uuid())
  name      String
  storeId   String    @map("store_id")
  createdAt DateTime  @default(now()) @map("created_at")
  products  Product[]

  @@index([storeId])
  @@map("categories")
}

model Product {
  id         String    @id @default(uuid())
  name       String
  barcode    String?
  sku        String?
  price      Float
  costPrice  Float     @default(0) @map("cost_price")
  quantity   Int       @default(0)
  unit       String    @default("шт")
  categoryId String?   @map("category_id")
  category   Category? @relation(fields: [categoryId], references: [id])
  imageUrl   String?   @map("image_url")
  storeId    String    @map("store_id")
  isDeleted  Boolean   @default(false) @map("is_deleted")
  createdAt  DateTime  @default(now()) @map("created_at")
  updatedAt  DateTime  @updatedAt @map("updated_at")

  @@index([storeId])
  @@index([storeId, barcode])
  @@map("products")
}
```

- [ ] **Step 2: Run migration**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npx prisma migrate dev --name add_products_categories
```

- [ ] **Step 3: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/prisma/
git commit -m "feat(products): add Product and Category Prisma models"
```

---

## Task 2: Backend — Products DTOs

**Files:**
- Create: `api/src/modules/products/dto/create-product.dto.ts`
- Create: `api/src/modules/products/dto/update-product.dto.ts`
- Create: `api/src/modules/products/dto/product-query.dto.ts`

- [ ] **Step 1: Create DTOs**

Create `api/src/modules/products/dto/create-product.dto.ts`:
```typescript
import { IsString, IsNumber, IsOptional, Min } from 'class-validator';

export class CreateProductDto {
  @IsString()
  name!: string;

  @IsOptional()
  @IsString()
  barcode?: string;

  @IsOptional()
  @IsString()
  sku?: string;

  @IsNumber()
  @Min(0)
  price!: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  costPrice?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  quantity?: number;

  @IsOptional()
  @IsString()
  unit?: string;

  @IsOptional()
  @IsString()
  categoryId?: string;

  @IsOptional()
  @IsString()
  imageUrl?: string;
}
```

Create `api/src/modules/products/dto/update-product.dto.ts`:
```typescript
import { IsString, IsNumber, IsOptional, Min } from 'class-validator';

export class UpdateProductDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsString()
  barcode?: string;

  @IsOptional()
  @IsString()
  sku?: string;

  @IsOptional()
  @IsNumber()
  @Min(0)
  price?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  costPrice?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  quantity?: number;

  @IsOptional()
  @IsString()
  unit?: string;

  @IsOptional()
  @IsString()
  categoryId?: string;

  @IsOptional()
  @IsString()
  imageUrl?: string;
}
```

Create `api/src/modules/products/dto/product-query.dto.ts`:
```typescript
import { IsOptional, IsString, IsNumber, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class ProductQueryDto {
  @IsOptional()
  @IsString()
  search?: string;

  @IsOptional()
  @IsString()
  cursor?: string;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(1)
  limit?: number = 20;
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/src/modules/products/dto/
git commit -m "feat(products): add product DTOs with validation"
```

---

## Task 3: Backend — Products Service + Controller + Module

**Files:**
- Create: `api/src/modules/products/products.service.ts`
- Create: `api/src/modules/products/products.controller.ts`
- Create: `api/src/modules/products/products.module.ts`
- Modify: `api/src/app.module.ts`

- [ ] **Step 1: Create ProductsService**

Create `api/src/modules/products/products.service.ts`:
```typescript
import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { ProductQueryDto } from './dto/product-query.dto';

@Injectable()
export class ProductsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(storeId: string, query: ProductQueryDto) {
    const limit = query.limit ?? 20;
    const where: any = { storeId, isDeleted: false };

    if (query.search) {
      where.OR = [
        { name: { contains: query.search, mode: 'insensitive' } },
        { barcode: { contains: query.search } },
      ];
    }

    const items = await this.prisma.product.findMany({
      where,
      include: { category: true },
      orderBy: { createdAt: 'desc' },
      take: limit + 1,
      ...(query.cursor ? { cursor: { id: query.cursor }, skip: 1 } : {}),
    });

    const hasMore = items.length > limit;
    const products = hasMore ? items.slice(0, limit) : items;
    const nextCursor = hasMore ? products[products.length - 1].id : null;

    return { products, nextCursor };
  }

  async findOne(storeId: string, id: string) {
    const product = await this.prisma.product.findFirst({
      where: { id, storeId, isDeleted: false },
      include: { category: true },
    });
    if (!product) throw new NotFoundException('Product not found');
    return product;
  }

  async create(storeId: string, dto: CreateProductDto) {
    return this.prisma.product.create({
      data: {
        name: dto.name,
        barcode: dto.barcode,
        sku: dto.sku,
        price: dto.price,
        costPrice: dto.costPrice ?? 0,
        quantity: dto.quantity ?? 0,
        unit: dto.unit ?? 'шт',
        categoryId: dto.categoryId,
        imageUrl: dto.imageUrl,
        storeId,
      },
      include: { category: true },
    });
  }

  async update(storeId: string, id: string, dto: UpdateProductDto) {
    await this.findOne(storeId, id);
    return this.prisma.product.update({
      where: { id },
      data: dto,
      include: { category: true },
    });
  }

  async softDelete(storeId: string, id: string) {
    await this.findOne(storeId, id);
    return this.prisma.product.update({
      where: { id },
      data: { isDeleted: true },
    });
  }

  async getCategories(storeId: string) {
    return this.prisma.category.findMany({
      where: { storeId },
      orderBy: { name: 'asc' },
    });
  }

  async createCategory(storeId: string, name: string) {
    return this.prisma.category.create({
      data: { name, storeId },
    });
  }
}
```

- [ ] **Step 2: Create ProductsController**

Create `api/src/modules/products/products.controller.ts`:
```typescript
import { Controller, Get, Post, Patch, Delete, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { ProductsService } from './products.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { ProductQueryDto } from './dto/product-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/products')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string, @Query() query: ProductQueryDto) {
    return this.productsService.findAll(storeId, query);
  }

  @Get(':id')
  async findOne(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.productsService.findOne(storeId, id);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateProductDto) {
    return this.productsService.create(storeId, dto);
  }

  @Patch(':id')
  async update(@Param('storeId') storeId: string, @Param('id') id: string, @Body() dto: UpdateProductDto) {
    return this.productsService.update(storeId, id, dto);
  }

  @Delete(':id')
  async remove(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.productsService.softDelete(storeId, id);
  }
}
```

- [ ] **Step 3: Create categories controller (same module)**

Create `api/src/modules/products/categories.controller.ts`:
```typescript
import { Controller, Get, Post, Body, Param, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { ProductsService } from './products.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { IsString, MinLength } from 'class-validator';

class CreateCategoryDto {
  @IsString()
  @MinLength(1)
  name!: string;
}

@Controller('stores/:storeId/categories')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true }))
export class CategoriesController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string) {
    return this.productsService.getCategories(storeId);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateCategoryDto) {
    return this.productsService.createCategory(storeId, dto.name);
  }
}
```

- [ ] **Step 4: Create ProductsModule and register in AppModule**

Create `api/src/modules/products/products.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { ProductsController } from './products.controller';
import { CategoriesController } from './categories.controller';
import { ProductsService } from './products.service';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [AuthModule],
  controllers: [ProductsController, CategoriesController],
  providers: [ProductsService],
})
export class ProductsModule {}
```

Add `ProductsModule` to `api/src/app.module.ts` imports:
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PrismaModule } from './prisma/prisma.module';
import { HealthModule } from './modules/health/health.module';
import { AuthModule } from './modules/auth/auth.module';
import { ProductsModule } from './modules/products/products.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    PrismaModule,
    HealthModule,
    AuthModule,
    ProductsModule,
  ],
})
export class AppModule {}
```

- [ ] **Step 5: Build and test**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && npm run build
```

- [ ] **Step 6: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/src/modules/products/ api/src/app.module.ts
git commit -m "feat(products): add products and categories CRUD endpoints"
```

---

## Task 4: KMP — SQLDelight Tables (product, category, sync_queue)

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/category.sq`
- Create: `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/product.sq`
- Create: `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/sync_queue.sq`

- [ ] **Step 1: Create category.sq**

Create `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/category.sq`:
```sql
CREATE TABLE IF NOT EXISTS categories (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    store_id TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_categories_store_id ON categories(store_id);

selectByStoreId:
SELECT * FROM categories WHERE store_id = ? ORDER BY name ASC;

insertOrReplace:
INSERT OR REPLACE INTO categories (id, name, store_id, created_at)
VALUES (?, ?, ?, ?);

deleteById:
DELETE FROM categories WHERE id = ?;
```

- [ ] **Step 2: Create product.sq**

Create `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/product.sq`:
```sql
CREATE TABLE IF NOT EXISTS products (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    barcode TEXT,
    sku TEXT,
    price REAL NOT NULL DEFAULT 0,
    cost_price REAL NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit TEXT NOT NULL DEFAULT 'шт',
    category_id TEXT,
    image_url TEXT,
    store_id TEXT NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_products_store_id ON products(store_id);
CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(store_id, barcode);

selectByStoreId:
SELECT * FROM products WHERE store_id = ? AND is_deleted = 0 ORDER BY created_at DESC;

searchByNameOrBarcode:
SELECT * FROM products
WHERE store_id = ? AND is_deleted = 0
AND (name LIKE '%' || ? || '%' OR barcode LIKE '%' || ? || '%')
ORDER BY name ASC;

selectById:
SELECT * FROM products WHERE id = ? AND is_deleted = 0;

insertOrReplace:
INSERT OR REPLACE INTO products (id, name, barcode, sku, price, cost_price, quantity, unit, category_id, image_url, store_id, is_deleted, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

softDelete:
UPDATE products SET is_deleted = 1, updated_at = ? WHERE id = ?;

deleteById:
DELETE FROM products WHERE id = ?;

countByStoreId:
SELECT COUNT(*) FROM products WHERE store_id = ? AND is_deleted = 0;
```

- [ ] **Step 3: Create sync_queue.sq**

Create `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/sync_queue.sq`:
```sql
CREATE TABLE IF NOT EXISTS sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL
);

selectPending:
SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC;

selectPendingCount:
SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING';

insert:
INSERT INTO sync_queue (entity_type, entity_id, operation, payload, retry_count, status, created_at)
VALUES (?, ?, ?, ?, 0, 'PENDING', ?);

updateStatus:
UPDATE sync_queue SET status = ?, retry_count = ? WHERE id = ?;

deleteById:
DELETE FROM sync_queue WHERE id = ?;

deleteCompleted:
DELETE FROM sync_queue WHERE status = 'COMPLETED';
```

Remove `.gitkeep` from `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/` if it exists.

- [ ] **Step 4: Verify shared module compiles**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:generateDebugDokonProDatabaseInterface
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/sqldelight/
git commit -m "feat(products): add SQLDelight tables for products, categories, and sync queue"
```

---

## Task 5: KMP — DatabaseFactory (expect/actual) + Domain Entities

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.kt`
- Create: `shared/src/androidMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.android.kt`
- Create: `shared/src/iosMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.ios.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Product.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Category.kt`

- [ ] **Step 1: Create DatabaseFactory expect/actual**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.kt`:
```kotlin
package com.dokonpro.shared.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

Create `shared/src/androidMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.android.kt`:
```kotlin
package com.dokonpro.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.dokonpro.shared.db.DokonProDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(DokonProDatabase.Schema, context, "dokonpro.db")
}
```

Create `shared/src/iosMain/kotlin/com/dokonpro/shared/data/local/DatabaseFactory.ios.kt`:
```kotlin
package com.dokonpro.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.dokonpro.shared.db.DokonProDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(DokonProDatabase.Schema, "dokonpro.db")
}
```

- [ ] **Step 2: Create domain entities**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Product.kt`:
```kotlin
package com.dokonpro.shared.domain.entity

data class Product(
    val id: String,
    val name: String,
    val barcode: String?,
    val sku: String?,
    val price: Double,
    val costPrice: Double,
    val quantity: Int,
    val unit: String,
    val categoryId: String?,
    val categoryName: String?,
    val imageUrl: String?,
    val storeId: String,
    val createdAt: String,
    val updatedAt: String
)
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Category.kt`:
```kotlin
package com.dokonpro.shared.domain.entity

data class Category(
    val id: String,
    val name: String,
    val storeId: String
)
```

- [ ] **Step 3: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add shared/src/
git commit -m "feat(products): add DatabaseFactory, Product and Category entities"
```

---

## Task 6: KMP — Sync Engine (SyncQueue + SyncManager + SyncStatus)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncStatus.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncQueue.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncManager.kt`

- [ ] **Step 1: Create SyncStatus**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncStatus.kt`:
```kotlin
package com.dokonpro.shared.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncState {
    IDLE,
    SYNCING,
    ERROR
}

data class SyncStatusData(
    val state: SyncState = SyncState.IDLE,
    val pendingCount: Long = 0
)

class SyncStatus {
    private val _status = MutableStateFlow(SyncStatusData())
    val status: StateFlow<SyncStatusData> = _status.asStateFlow()

    fun update(state: SyncState, pendingCount: Long) {
        _status.value = SyncStatusData(state, pendingCount)
    }
}
```

- [ ] **Step 2: Create SyncQueue**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncQueue.kt`:
```kotlin
package com.dokonpro.shared.data.sync

import com.dokonpro.shared.db.DokonProDatabase
import kotlinx.datetime.Clock

data class SyncEntry(
    val id: Long,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val retryCount: Long,
    val status: String,
    val createdAt: String
)

class SyncQueue(private val db: DokonProDatabase) {

    fun enqueue(entityType: String, entityId: String, operation: String, payload: String) {
        db.sync_queueQueries.insert(
            entity_type = entityType,
            entity_id = entityId,
            operation = operation,
            payload = payload,
            created_at = Clock.System.now().toString()
        )
    }

    fun getPending(): List<SyncEntry> =
        db.sync_queueQueries.selectPending().executeAsList().map { row ->
            SyncEntry(
                id = row.id,
                entityType = row.entity_type,
                entityId = row.entity_id,
                operation = row.operation,
                payload = row.payload,
                retryCount = row.retry_count,
                status = row.status,
                createdAt = row.created_at
            )
        }

    fun getPendingCount(): Long =
        db.sync_queueQueries.selectPendingCount().executeAsOne()

    fun markCompleted(id: Long) {
        db.sync_queueQueries.deleteById(id)
    }

    fun markFailed(id: Long, retryCount: Long) {
        val newStatus = if (retryCount >= 5) "FAILED" else "PENDING"
        db.sync_queueQueries.updateStatus(
            status = newStatus,
            retry_count = retryCount,
            id = id
        )
    }

    fun cleanCompleted() {
        db.sync_queueQueries.deleteCompleted()
    }
}
```

- [ ] **Step 3: Create SyncManager**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/sync/SyncManager.kt`:
```kotlin
package com.dokonpro.shared.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min
import kotlin.math.pow

class SyncManager(
    private val queue: SyncQueue,
    private val client: HttpClient,
    private val baseUrl: String,
    private val syncStatus: SyncStatus,
    private val tokenProvider: () -> String?
) {
    private var syncJob: Job? = null

    val status: StateFlow<SyncStatusData> = syncStatus.status

    fun startSync(scope: CoroutineScope) {
        syncJob?.cancel()
        syncJob = scope.launch {
            processQueue()
        }
    }

    suspend fun processQueue() {
        val pending = queue.getPending()
        if (pending.isEmpty()) {
            syncStatus.update(SyncState.IDLE, 0)
            return
        }

        syncStatus.update(SyncState.SYNCING, pending.size.toLong())

        for (entry in pending) {
            try {
                executeSync(entry)
                queue.markCompleted(entry.id)
            } catch (e: Exception) {
                val newRetry = entry.retryCount + 1
                queue.markFailed(entry.id, newRetry)

                if (newRetry < 5) {
                    val delayMs = (1000.0 * 2.0.pow(newRetry.toDouble())).toLong()
                    delay(min(delayMs, 30_000L))
                }
            }

            val remaining = queue.getPendingCount()
            if (remaining > 0) {
                syncStatus.update(SyncState.SYNCING, remaining)
            }
        }

        val finalPending = queue.getPendingCount()
        syncStatus.update(
            if (finalPending > 0) SyncState.ERROR else SyncState.IDLE,
            finalPending
        )
    }

    private suspend fun executeSync(entry: SyncEntry) {
        val token = tokenProvider() ?: throw IllegalStateException("No auth token")
        val url = "$baseUrl/stores/${extractStoreId(entry)}"

        when (entry.operation) {
            "CREATE" -> {
                client.post("$url/${entry.entityType}") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(entry.payload)
                }
            }
            "UPDATE" -> {
                client.patch("$url/${entry.entityType}/${entry.entityId}") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(entry.payload)
                }
            }
            "DELETE" -> {
                client.delete("$url/${entry.entityType}/${entry.entityId}") {
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }

    private fun extractStoreId(entry: SyncEntry): String {
        // Entity IDs are stored as "storeId:entityId" for scoped resources
        return if (entry.entityId.contains(":")) {
            entry.entityId.substringBefore(":")
        } else {
            entry.entityId
        }
    }

    fun stop() {
        syncJob?.cancel()
    }
}
```

- [ ] **Step 4: Add kotlinx-datetime dependency**

Add to `gradle/libs.versions.toml` under `[versions]`:
```toml
kotlinxDatetime = "0.6.1"
```

Under `[libraries]`:
```toml
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
```

Add to `shared/build.gradle.kts` `commonMain.dependencies`:
```kotlin
implementation(libs.kotlinx.datetime)
```

- [ ] **Step 5: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add shared/src/ gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "feat(sync): add SyncQueue, SyncManager with exponential backoff, and SyncStatus"
```

---

## Task 7: KMP — ProductRepository Interface + Use Cases

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/repository/ProductRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetProductsUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/CreateProductUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/UpdateProductUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/DeleteProductUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/SearchProductUseCase.kt`

- [ ] **Step 1: Create ProductRepository interface**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/repository/ProductRepository.kt`:
```kotlin
package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(storeId: String): Flow<List<Product>>
    fun searchProducts(storeId: String, query: String): Flow<List<Product>>
    suspend fun getProductById(id: String): Product?
    suspend fun createProduct(storeId: String, product: Product): Result<Product>
    suspend fun updateProduct(product: Product): Result<Product>
    suspend fun deleteProduct(storeId: String, productId: String): Result<Unit>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
    fun getCategories(storeId: String): Flow<List<Category>>
}
```

- [ ] **Step 2: Create use cases**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetProductsUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(storeId: String): Flow<List<Product>> =
        repository.getProducts(storeId)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/CreateProductUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository

class CreateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(storeId: String, product: Product): Result<Product> =
        repository.createProduct(storeId, product)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/UpdateProductUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product): Result<Product> =
        repository.updateProduct(product)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/DeleteProductUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.repository.ProductRepository

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(storeId: String, productId: String): Result<Unit> =
        repository.deleteProduct(storeId, productId)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/SearchProductUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class SearchProductUseCase(private val repository: ProductRepository) {
    operator fun invoke(storeId: String, query: String): Flow<List<Product>> =
        repository.searchProducts(storeId, query)
}
```

- [ ] **Step 3: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add shared/src/commonMain/kotlin/com/dokonpro/shared/domain/
git commit -m "feat(products): add ProductRepository interface and product use cases"
```

---

## Task 8: KMP — ProductApiClient + ProductLocalDataSource + ProductRepositoryImpl

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/ProductDtos.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/ProductApiClient.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/ProductLocalDataSource.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/ProductRepositoryImpl.kt`

- [ ] **Step 1: Create Product DTOs**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/ProductDtos.kt`:
```kotlin
package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "шт",
    val categoryId: String? = null,
    val category: CategoryDto? = null,
    val imageUrl: String? = null,
    val storeId: String,
    val isDeleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val storeId: String? = null
)

@Serializable
data class ProductListResponse(
    val products: List<ProductDto>,
    val nextCursor: String? = null
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "шт",
    val categoryId: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val price: Double? = null,
    val costPrice: Double? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null
)
```

- [ ] **Step 2: Create ProductApiClient**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/ProductApiClient.kt`:
```kotlin
package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ProductApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getProducts(storeId: String, cursor: String? = null, search: String? = null): ProductListResponse {
        return client.get("$baseUrl/stores/$storeId/products") {
            header("Authorization", authHeader())
            cursor?.let { parameter("cursor", it) }
            search?.let { parameter("search", it) }
        }.body()
    }

    suspend fun createProduct(storeId: String, request: CreateProductRequest): ProductDto {
        return client.post("$baseUrl/stores/$storeId/products") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateProduct(storeId: String, productId: String, request: UpdateProductRequest): ProductDto {
        return client.patch("$baseUrl/stores/$storeId/products/$productId") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteProduct(storeId: String, productId: String) {
        client.delete("$baseUrl/stores/$storeId/products/$productId") {
            header("Authorization", authHeader())
        }
    }

    suspend fun getCategories(storeId: String): List<CategoryDto> {
        return client.get("$baseUrl/stores/$storeId/categories") {
            header("Authorization", authHeader())
        }.body()
    }
}
```

- [ ] **Step 3: Create ProductLocalDataSource**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/ProductLocalDataSource.kt`:
```kotlin
package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ProductLocalDataSource(private val db: DokonProDatabase) {

    fun getProducts(storeId: String): Flow<List<Product>> =
        db.productQueries.selectByStoreId(storeId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toProduct() } }

    fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        db.productQueries.searchByNameOrBarcode(storeId, query, query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toProduct() } }

    fun getProductById(id: String): Product? =
        db.productQueries.selectById(id).executeAsOneOrNull()?.toProduct()

    fun insertProduct(product: Product) {
        db.productQueries.insertOrReplace(
            id = product.id,
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            cost_price = product.costPrice,
            quantity = product.quantity.toLong(),
            unit = product.unit,
            category_id = product.categoryId,
            image_url = product.imageUrl,
            store_id = product.storeId,
            is_deleted = 0,
            created_at = product.createdAt,
            updated_at = product.updatedAt
        )
    }

    fun softDeleteProduct(id: String) {
        db.productQueries.softDelete(Clock.System.now().toString(), id)
    }

    fun insertProducts(products: List<Product>) {
        db.transaction {
            products.forEach { insertProduct(it) }
        }
    }

    fun getCategories(storeId: String): Flow<List<Category>> =
        db.categoryQueries.selectByStoreId(storeId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { Category(it.id, it.name, it.store_id) } }

    fun insertCategory(category: Category) {
        db.categoryQueries.insertOrReplace(category.id, category.name, category.storeId, Clock.System.now().toString())
    }

    private fun com.dokonpro.shared.db.Products.toProduct() = Product(
        id = id,
        name = name,
        barcode = barcode,
        sku = sku,
        price = price,
        costPrice = cost_price,
        quantity = quantity.toInt(),
        unit = unit,
        categoryId = category_id,
        categoryName = null,
        imageUrl = image_url,
        storeId = store_id,
        createdAt = created_at,
        updatedAt = updated_at
    )
}
```

- [ ] **Step 4: Create ProductRepositoryImpl**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/ProductRepositoryImpl.kt`:
```kotlin
package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.ProductLocalDataSource
import com.dokonpro.shared.data.remote.ProductApiClient
import com.dokonpro.shared.data.remote.dto.CreateProductRequest
import com.dokonpro.shared.data.remote.dto.UpdateProductRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProductRepositoryImpl(
    private val local: ProductLocalDataSource,
    private val api: ProductApiClient,
    private val syncQueue: SyncQueue
) : ProductRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getProducts(storeId: String): Flow<List<Product>> =
        local.getProducts(storeId)

    override fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        local.searchProducts(storeId, query)

    override suspend fun getProductById(id: String): Product? =
        local.getProductById(id)

    override suspend fun createProduct(storeId: String, product: Product): Result<Product> = runCatching {
        local.insertProduct(product)

        val request = CreateProductRequest(
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            costPrice = product.costPrice,
            quantity = product.quantity,
            unit = product.unit,
            categoryId = product.categoryId,
            imageUrl = product.imageUrl
        )
        syncQueue.enqueue(
            entityType = "products",
            entityId = "$storeId:${product.id}",
            operation = "CREATE",
            payload = json.encodeToString(request)
        )
        product
    }

    override suspend fun updateProduct(product: Product): Result<Product> = runCatching {
        local.insertProduct(product)

        val request = UpdateProductRequest(
            name = product.name,
            barcode = product.barcode,
            sku = product.sku,
            price = product.price,
            costPrice = product.costPrice,
            quantity = product.quantity,
            unit = product.unit,
            categoryId = product.categoryId,
            imageUrl = product.imageUrl
        )
        syncQueue.enqueue(
            entityType = "products",
            entityId = "${product.storeId}:${product.id}",
            operation = "UPDATE",
            payload = json.encodeToString(request)
        )
        product
    }

    override suspend fun deleteProduct(storeId: String, productId: String): Result<Unit> = runCatching {
        local.softDeleteProduct(productId)
        syncQueue.enqueue(
            entityType = "products",
            entityId = "$storeId:$productId",
            operation = "DELETE",
            payload = "{}"
        )
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val response = api.getProducts(storeId)
        val products = response.products.map { dto ->
            Product(
                id = dto.id,
                name = dto.name,
                barcode = dto.barcode,
                sku = dto.sku,
                price = dto.price,
                costPrice = dto.costPrice,
                quantity = dto.quantity,
                unit = dto.unit,
                categoryId = dto.categoryId,
                categoryName = dto.category?.name,
                imageUrl = dto.imageUrl,
                storeId = dto.storeId,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
        local.insertProducts(products)

        val categories = api.getCategories(storeId)
        categories.forEach { dto ->
            local.insertCategory(Category(dto.id, dto.name, dto.storeId ?: storeId))
        }
    }

    override fun getCategories(storeId: String): Flow<List<Category>> =
        local.getCategories(storeId)
}
```

- [ ] **Step 5: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add shared/src/commonMain/kotlin/com/dokonpro/shared/data/
git commit -m "feat(products): add ProductApiClient, LocalDataSource, and offline-first ProductRepositoryImpl"
```

---

## Task 9: KMP — Koin Product + Database Modules

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/DatabaseModule.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/ProductModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`

- [ ] **Step 1: Create DatabaseModule**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/di/DatabaseModule.kt`:
```kotlin
package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.DatabaseDriverFactory
import com.dokonpro.shared.data.local.ProductLocalDataSource
import com.dokonpro.shared.data.sync.SyncManager
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.data.sync.SyncStatus
import com.dokonpro.shared.db.DokonProDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { DokonProDatabase(get()) }
    single { ProductLocalDataSource(get()) }
    single { SyncQueue(get()) }
    single { SyncStatus() }
    single {
        SyncManager(
            queue = get(),
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            syncStatus = get(),
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken }
        )
    }
}
```

- [ ] **Step 2: Create ProductModule**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/di/ProductModule.kt`:
```kotlin
package com.dokonpro.shared.di

import com.dokonpro.shared.data.remote.ProductApiClient
import com.dokonpro.shared.data.repository.ProductRepositoryImpl
import com.dokonpro.shared.domain.repository.ProductRepository
import org.koin.dsl.module

val productModule = module {
    single {
        ProductApiClient(
            client = get(),
            baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken }
        )
    }
    single<ProductRepository> { ProductRepositoryImpl(get(), get(), get()) }
    factory { com.dokonpro.shared.domain.usecase.GetProductsUseCase(get()) }
    factory { com.dokonpro.shared.domain.usecase.CreateProductUseCase(get()) }
    factory { com.dokonpro.shared.domain.usecase.UpdateProductUseCase(get()) }
    factory { com.dokonpro.shared.domain.usecase.DeleteProductUseCase(get()) }
    factory { com.dokonpro.shared.domain.usecase.SearchProductUseCase(get()) }
}
```

- [ ] **Step 3: Update SharedModule**

Replace `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`:
```kotlin
package com.dokonpro.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module = module {
    includes(authModule, databaseModule, productModule)
}
```

- [ ] **Step 4: Build shared module**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
```

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/dokonpro/shared/di/
git commit -m "feat(products): add Koin database, sync, and product modules"
```

---

## Task 10: KMP — Use Case Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/com/dokonpro/shared/domain/usecase/ProductUseCaseTest.kt`

- [ ] **Step 1: Create FakeProductRepository and tests**

Create `shared/src/commonTest/kotlin/com/dokonpro/shared/domain/usecase/ProductUseCaseTest.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Category
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeProductRepository : ProductRepository {
    val products = mutableListOf<Product>()
    var deleteResult: Result<Unit> = Result.success(Unit)

    override fun getProducts(storeId: String): Flow<List<Product>> =
        flowOf(products.filter { it.storeId == storeId })

    override fun searchProducts(storeId: String, query: String): Flow<List<Product>> =
        flowOf(products.filter { it.storeId == storeId && (it.name.contains(query, true) || it.barcode?.contains(query) == true) })

    override suspend fun getProductById(id: String): Product? =
        products.find { it.id == id }

    override suspend fun createProduct(storeId: String, product: Product): Result<Product> {
        products.add(product)
        return Result.success(product)
    }

    override suspend fun updateProduct(product: Product): Result<Product> {
        val idx = products.indexOfFirst { it.id == product.id }
        if (idx >= 0) products[idx] = product
        return Result.success(product)
    }

    override suspend fun deleteProduct(storeId: String, productId: String): Result<Unit> {
        products.removeAll { it.id == productId }
        return deleteResult
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)

    override fun getCategories(storeId: String): Flow<List<Category>> = flowOf(emptyList())
}

private fun testProduct(id: String = "p1", name: String = "Coca-Cola", storeId: String = "store-1") = Product(
    id = id, name = name, barcode = "123456", sku = null,
    price = 12.5, costPrice = 8.0, quantity = 48, unit = "шт",
    categoryId = null, categoryName = null, imageUrl = null,
    storeId = storeId, createdAt = "2026-01-01", updatedAt = "2026-01-01"
)

class GetProductsUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = GetProductsUseCase(repo)

    @Test
    fun `should return products for store`() = runTest {
        repo.products.add(testProduct())
        val result = useCase("store-1").first()
        assertEquals(1, result.size)
        assertEquals("Coca-Cola", result[0].name)
    }
}

class CreateProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = CreateProductUseCase(repo)

    @Test
    fun `should create product and return it`() = runTest {
        val product = testProduct()
        val result = useCase("store-1", product)
        assertTrue(result.isSuccess)
        assertEquals(1, repo.products.size)
    }
}

class SearchProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = SearchProductUseCase(repo)

    @Test
    fun `should find product by name`() = runTest {
        repo.products.add(testProduct())
        repo.products.add(testProduct(id = "p2", name = "Fanta"))
        val result = useCase("store-1", "coca").first()
        assertEquals(1, result.size)
        assertEquals("Coca-Cola", result[0].name)
    }
}

class DeleteProductUseCaseTest {
    private val repo = FakeProductRepository()
    private val useCase = DeleteProductUseCase(repo)

    @Test
    fun `should delete product`() = runTest {
        repo.products.add(testProduct())
        val result = useCase("store-1", "p1")
        assertTrue(result.isSuccess)
        assertEquals(0, repo.products.size)
    }
}
```

- [ ] **Step 2: Run tests**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/
git commit -m "test(products): add product use case tests with FakeProductRepository"
```

---

## Task 11: Android — ProductViewModel

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/viewmodel/ProductViewModel.kt`

- [ ] **Step 1: Create ProductViewModel**

Create `androidApp/src/main/java/com/dokonpro/android/viewmodel/ProductViewModel.kt`:
```kotlin
package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.data.sync.SyncManager
import com.dokonpro.shared.data.sync.SyncStatusData
import com.dokonpro.shared.domain.entity.Product
import com.dokonpro.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ProductListState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class ProductViewModel(
    private val getProducts: GetProductsUseCase,
    private val createProduct: CreateProductUseCase,
    private val updateProduct: UpdateProductUseCase,
    private val deleteProduct: DeleteProductUseCase,
    private val searchProducts: SearchProductUseCase,
    private val syncManager: SyncManager,
    private val storeId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState())
    val state: StateFlow<ProductListState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatusData> = syncManager.status

    init {
        loadProducts()
        syncManager.startSync(viewModelScope)
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getProducts(storeId).collect { products ->
                _state.value = _state.value.copy(products = products, isLoading = false)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                getProducts(storeId).collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            } else {
                searchProducts(storeId, query).collect { products ->
                    _state.value = _state.value.copy(products = products)
                }
            }
        }
    }

    fun addProduct(
        name: String,
        barcode: String?,
        price: Double,
        costPrice: Double,
        quantity: Int,
        unit: String,
        categoryId: String?
    ) {
        val now = Clock.System.now().toString()
        val product = Product(
            id = generateId(),
            name = name,
            barcode = barcode,
            sku = null,
            price = price,
            costPrice = costPrice,
            quantity = quantity,
            unit = unit,
            categoryId = categoryId,
            categoryName = null,
            imageUrl = null,
            storeId = storeId,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            createProduct(storeId, product)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun removeProduct(productId: String) {
        viewModelScope.launch {
            deleteProduct(storeId, productId)
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun generateId(): String =
        "local-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add androidApp/src/main/java/com/dokonpro/android/viewmodel/ProductViewModel.kt
git commit -m "feat(products): add ProductViewModel with search, CRUD, and sync"
```

---

## Task 12: Android — Product Screens (List, Detail, Add/Edit)

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/products/ProductListScreen.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/products/ProductDetailScreen.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/products/AddEditProductScreen.kt`

- [ ] **Step 1: Create ProductListScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/products/ProductListScreen.kt`:
```kotlin
package com.dokonpro.android.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokonpro.android.R
import com.dokonpro.shared.data.sync.SyncState
import com.dokonpro.shared.data.sync.SyncStatusData
import com.dokonpro.shared.domain.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    products: List<Product>,
    searchQuery: String,
    syncStatus: SyncStatusData,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_products)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (syncStatus.state == SyncState.SYNCING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                    if (syncStatus.pendingCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("${syncStatus.pendingCount}")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.product_add))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.product_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.product_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductListItem(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(product: Product, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = product.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                product.barcode?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${product.price} с",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${product.quantity} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.quantity < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create AddEditProductScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/products/AddEditProductScreen.kt`:
```kotlin
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    onSave: (name: String, barcode: String?, price: Double, costPrice: Double, quantity: Int, unit: String, categoryId: String?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("шт") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text(stringResource(R.string.product_barcode)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text(stringResource(R.string.product_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = costPrice, onValueChange = { costPrice = it },
                    label = { Text(stringResource(R.string.product_cost_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity, onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.product_quantity)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit, onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.product_unit)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true, shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onSave(
                        name,
                        barcode.ifBlank { null },
                        price.toDoubleOrNull() ?: 0.0,
                        costPrice.toDoubleOrNull() ?: 0.0,
                        quantity.toIntOrNull() ?: 0,
                        unit,
                        null
                    )
                },
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 3: Create ProductDetailScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/products/ProductDetailScreen.kt`:
```kotlin
package com.dokonpro.android.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.product_edit)) }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (product == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.product_not_found))
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        product.name.take(2).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            product.barcode?.let {
                Text("${stringResource(R.string.product_barcode)}: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(label = stringResource(R.string.product_price), value = "${product.price} с", modifier = Modifier.weight(1f))
                StatCard(label = stringResource(R.string.product_cost_price), value = "${product.costPrice} с", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(label = stringResource(R.string.product_quantity), value = "${product.quantity} ${product.unit}", modifier = Modifier.weight(1f))
                val margin = if (product.price > 0) ((product.price - product.costPrice) / product.price * 100).toInt() else 0
                StatCard(label = stringResource(R.string.product_margin), value = "$margin%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add androidApp/src/main/java/com/dokonpro/android/ui/products/
git commit -m "feat(products): add ProductList, ProductDetail, and AddEditProduct Compose screens"
```

---

## Task 13: Android — Product Strings + Navigation + DI Wiring

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values-tg/strings.xml`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`

- [ ] **Step 1: Add product strings (Russian)**

Add before closing `</resources>` in `androidApp/src/main/res/values/strings.xml`:
```xml
    <!-- Products -->
    <string name="product_add">Добавить товар</string>
    <string name="product_edit">Редактировать</string>
    <string name="product_detail">Товар</string>
    <string name="product_search_hint">Поиск по названию или штрихкоду…</string>
    <string name="product_empty">Нет товаров</string>
    <string name="product_not_found">Товар не найден</string>
    <string name="product_name">Название</string>
    <string name="product_barcode">Штрихкод</string>
    <string name="product_price">Цена</string>
    <string name="product_cost_price">Себестоимость</string>
    <string name="product_quantity">Количество</string>
    <string name="product_unit">Единица</string>
    <string name="product_margin">Маржа</string>
    <string name="product_category">Категория</string>
```

- [ ] **Step 2: Add product strings (Tajik)**

Add before closing `</resources>` in `androidApp/src/main/res/values-tg/strings.xml`:
```xml
    <!-- Products -->
    <string name="product_add">Илова кардани мол</string>
    <string name="product_edit">Таҳрир</string>
    <string name="product_detail">Мол</string>
    <string name="product_search_hint">Ҷустуҷӯ аз рӯи ном ё штрихкод…</string>
    <string name="product_empty">Мол нест</string>
    <string name="product_not_found">Мол ёфт нашуд</string>
    <string name="product_name">Ном</string>
    <string name="product_barcode">Штрихкод</string>
    <string name="product_price">Нарх</string>
    <string name="product_cost_price">Арзиши аслӣ</string>
    <string name="product_quantity">Миқдор</string>
    <string name="product_unit">Воҳид</string>
    <string name="product_margin">Маржа</string>
    <string name="product_category">Категория</string>
```

- [ ] **Step 3: Update AppNavigation with product routes**

Add product routes to `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`. Add to the `Routes` object:
```kotlin
const val PRODUCTS = "products"
const val PRODUCT_DETAIL = "products/{productId}"
const val ADD_PRODUCT = "products/add"
```

Add composable routes inside `NavHost` after the MAIN composable:
```kotlin
composable(Routes.PRODUCTS) {
    val viewModel: ProductViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    ProductListScreen(
        products = state.products,
        searchQuery = state.searchQuery,
        syncStatus = syncStatus,
        isLoading = state.isLoading,
        onSearchChange = viewModel::onSearchQueryChange,
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
        product = product,
        onBack = { navController.popBackStack() },
        onEdit = { /* TODO: edit flow */ },
        onDelete = {
            viewModel.removeProduct(productId)
            navController.popBackStack()
        }
    )
}
```

Add the necessary imports for `ProductListScreen`, `AddEditProductScreen`, `ProductDetailScreen`, `ProductViewModel`.

- [ ] **Step 4: Update DokonProApp with product DI**

Add to the Koin module in `DokonProApp.kt`:
```kotlin
viewModel { ProductViewModel(get(), get(), get(), get(), get(), get(), "default-store") }
```

Also add `DatabaseDriverFactory` to the Android Koin module:
```kotlin
single { com.dokonpro.shared.data.local.DatabaseDriverFactory(get()) }
```

- [ ] **Step 5: Build**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add androidApp/
git commit -m "feat(products): wire product screens, navigation, strings, and DI"
```

---

## Task 14: Build Verification + Tag

- [ ] **Step 1: Run shared tests**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
```

- [ ] **Step 2: Build Android app**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
```

- [ ] **Step 3: Build backend**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && npm run build
```

- [ ] **Step 4: Fix any build issues**

If fixes needed:
```bash
git add -A
git commit -m "fix(products): resolve build issues"
```

- [ ] **Step 5: Tag milestone**

```bash
git tag v0.3.0-products
```
