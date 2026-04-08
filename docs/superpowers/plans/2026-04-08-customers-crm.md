# Customers / CRM Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Customers / CRM vertical slice — customer CRUD with purchase history, offline-first storage, and POS integration to attach customers to sales during checkout.

**Architecture:** Backend provides REST CRUD for customers scoped by storeId, plus a purchase history endpoint joining Sales. KMP shared module stores customers in SQLDelight with offline sync. Android app shows customer list with search, detail view with purchase history, and integrates with the existing POS checkout flow to optionally select a customer.

**Tech Stack:** NestJS 11, Prisma 6 | Kotlin 2.1.0, SQLDelight 2.0.2, Ktor 3.0.3, Koin 4.0.2 | Jetpack Compose, Material 3

---

## File Structure

```
api/
├── prisma/schema.prisma                               # MODIFY: add Customer model
├── src/
│   ├── app.module.ts                                  # MODIFY: add CustomersModule
│   └── modules/customers/
│       ├── customers.module.ts                        # CREATE
│       ├── customers.controller.ts                    # CREATE
│       ├── customers.service.ts                       # CREATE
│       └── dto/
│           ├── create-customer.dto.ts                 # CREATE
│           ├── update-customer.dto.ts                 # CREATE
│           └── customer-query.dto.ts                  # CREATE

shared/src/commonMain/
├── sqldelight/com/dokonpro/shared/db/
│   └── customer.sq                                    # CREATE
├── kotlin/com/dokonpro/shared/
│   ├── domain/
│   │   ├── entity/Customer.kt                         # MODIFY: expand from Phase 1 stub
│   │   ├── repository/CustomerRepository.kt           # CREATE
│   │   └── usecase/
│   │       ├── GetCustomersUseCase.kt                 # CREATE
│   │       ├── CreateCustomerUseCase.kt               # CREATE
│   │       ├── UpdateCustomerUseCase.kt               # CREATE
│   │       ├── SearchCustomerUseCase.kt               # CREATE
│   │       └── GetCustomerPurchasesUseCase.kt         # CREATE
│   ├── data/
│   │   ├── remote/
│   │   │   ├── CustomerApiClient.kt                   # CREATE
│   │   │   └── dto/CustomerDtos.kt                    # CREATE
│   │   ├── local/CustomerLocalDataSource.kt           # CREATE
│   │   └── repository/CustomerRepositoryImpl.kt       # CREATE
│   └── di/
│       ├── CustomerModule.kt                          # CREATE
│       └── SharedModule.kt                            # MODIFY

shared/src/commonTest/
└── kotlin/.../domain/usecase/CustomerUseCaseTest.kt   # CREATE

androidApp/src/main/
├── java/com/dokonpro/android/
│   ├── ui/customers/
│   │   ├── CustomerListScreen.kt                      # CREATE
│   │   ├── CustomerDetailScreen.kt                    # CREATE
│   │   └── AddEditCustomerScreen.kt                   # CREATE
│   ├── viewmodel/CustomerViewModel.kt                 # CREATE
│   ├── ui/pos/CheckoutScreen.kt                       # MODIFY: add customer picker
│   ├── viewmodel/POSViewModel.kt                      # MODIFY: add customerId
│   ├── navigation/AppNavigation.kt                    # MODIFY
│   └── DokonProApp.kt                                 # MODIFY
└── res/
    ├── values/strings.xml                             # MODIFY
    └── values-tg/strings.xml                          # MODIFY
```

---

## Task 1: Backend — Customer Prisma Model + Endpoints

**Files:**
- Modify: `api/prisma/schema.prisma`
- Create: `api/src/modules/customers/dto/create-customer.dto.ts`
- Create: `api/src/modules/customers/dto/update-customer.dto.ts`
- Create: `api/src/modules/customers/dto/customer-query.dto.ts`
- Create: `api/src/modules/customers/customers.service.ts`
- Create: `api/src/modules/customers/customers.controller.ts`
- Create: `api/src/modules/customers/customers.module.ts`
- Modify: `api/src/app.module.ts`

- [ ] **Step 1: Add Customer Prisma model**

Append to `api/prisma/schema.prisma` after SaleItem model:
```prisma
model Customer {
  id         String   @id @default(uuid())
  name       String
  phone      String?
  email      String?
  notes      String?
  totalSpent Float    @default(0) @map("total_spent")
  visitCount Int      @default(0) @map("visit_count")
  storeId    String   @map("store_id")
  createdAt  DateTime @default(now()) @map("created_at")
  updatedAt  DateTime @updatedAt @map("updated_at")

  @@index([storeId])
  @@index([storeId, phone])
  @@map("customers")
}
```

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npx prisma migrate dev --name add_customers
```

- [ ] **Step 2: Create DTOs**

Create `api/src/modules/customers/dto/create-customer.dto.ts`:
```typescript
import { IsString, IsOptional, IsEmail, MinLength } from 'class-validator';

export class CreateCustomerDto {
  @IsString() @MinLength(2)
  name!: string;

  @IsOptional() @IsString()
  phone?: string;

  @IsOptional() @IsEmail()
  email?: string;

  @IsOptional() @IsString()
  notes?: string;
}
```

Create `api/src/modules/customers/dto/update-customer.dto.ts`:
```typescript
import { IsString, IsOptional, IsEmail, MinLength } from 'class-validator';

export class UpdateCustomerDto {
  @IsOptional() @IsString() @MinLength(2)
  name?: string;

  @IsOptional() @IsString()
  phone?: string;

  @IsOptional() @IsEmail()
  email?: string;

  @IsOptional() @IsString()
  notes?: string;
}
```

Create `api/src/modules/customers/dto/customer-query.dto.ts`:
```typescript
import { IsOptional, IsString, IsNumber, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class CustomerQueryDto {
  @IsOptional() @IsString()
  search?: string;

  @IsOptional() @IsString()
  cursor?: string;

  @IsOptional() @Type(() => Number) @IsNumber() @Min(1)
  limit?: number = 20;
}
```

- [ ] **Step 3: Create CustomersService**

Create `api/src/modules/customers/customers.service.ts`:
```typescript
import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateCustomerDto } from './dto/create-customer.dto';
import { UpdateCustomerDto } from './dto/update-customer.dto';
import { CustomerQueryDto } from './dto/customer-query.dto';

@Injectable()
export class CustomersService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(storeId: string, query: CustomerQueryDto) {
    const limit = query.limit ?? 20;
    const where: any = { storeId };
    if (query.search) {
      where.OR = [
        { name: { contains: query.search, mode: 'insensitive' } },
        { phone: { contains: query.search } },
      ];
    }
    const items = await this.prisma.customer.findMany({
      where, orderBy: { createdAt: 'desc' }, take: limit + 1,
      ...(query.cursor ? { cursor: { id: query.cursor }, skip: 1 } : {}),
    });
    const hasMore = items.length > limit;
    const customers = hasMore ? items.slice(0, limit) : items;
    const nextCursor = hasMore ? customers[customers.length - 1].id : null;
    return { customers, nextCursor };
  }

  async findOne(storeId: string, id: string) {
    const customer = await this.prisma.customer.findFirst({ where: { id, storeId } });
    if (!customer) throw new NotFoundException('Customer not found');
    return customer;
  }

  async create(storeId: string, dto: CreateCustomerDto) {
    return this.prisma.customer.create({
      data: { name: dto.name, phone: dto.phone, email: dto.email, notes: dto.notes, storeId },
    });
  }

  async update(storeId: string, id: string, dto: UpdateCustomerDto) {
    await this.findOne(storeId, id);
    return this.prisma.customer.update({ where: { id }, data: dto });
  }

  async getPurchases(storeId: string, customerId: string) {
    await this.findOne(storeId, customerId);
    return this.prisma.sale.findMany({
      where: { storeId, customerId },
      include: { items: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async updateStatsAfterSale(customerId: string, saleAmount: number) {
    await this.prisma.customer.update({
      where: { id: customerId },
      data: {
        totalSpent: { increment: saleAmount },
        visitCount: { increment: 1 },
      },
    });
  }
}
```

- [ ] **Step 4: Create CustomersController**

Create `api/src/modules/customers/customers.controller.ts`:
```typescript
import { Controller, Get, Post, Patch, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { CustomersService } from './customers.service';
import { CreateCustomerDto } from './dto/create-customer.dto';
import { UpdateCustomerDto } from './dto/update-customer.dto';
import { CustomerQueryDto } from './dto/customer-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/customers')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class CustomersController {
  constructor(private readonly customersService: CustomersService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string, @Query() query: CustomerQueryDto) {
    return this.customersService.findAll(storeId, query);
  }

  @Get(':id')
  async findOne(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.customersService.findOne(storeId, id);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateCustomerDto) {
    return this.customersService.create(storeId, dto);
  }

  @Patch(':id')
  async update(@Param('storeId') storeId: string, @Param('id') id: string, @Body() dto: UpdateCustomerDto) {
    return this.customersService.update(storeId, id, dto);
  }

  @Get(':id/purchases')
  async getPurchases(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.customersService.getPurchases(storeId, id);
  }
}
```

- [ ] **Step 5: Create module and register**

Create `api/src/modules/customers/customers.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { CustomersController } from './customers.controller';
import { CustomersService } from './customers.service';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [AuthModule],
  controllers: [CustomersController],
  providers: [CustomersService],
  exports: [CustomersService],
})
export class CustomersModule {}
```

Add `CustomersModule` to `api/src/app.module.ts` imports.

- [ ] **Step 6: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && npm run build
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/
git commit -m "feat(customers): add Customer model and CRUD endpoints with purchase history"
```

---

## Task 2: KMP — SQLDelight Table + Customer Entity

**Files:**
- Create: `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/customer.sq`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Customer.kt` (replace existing minimal version)

- [ ] **Step 1: Create customer.sq**

Create `shared/src/commonMain/sqldelight/com/dokonpro/shared/db/customer.sq`:
```sql
CREATE TABLE IF NOT EXISTS customers (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    notes TEXT,
    total_spent REAL NOT NULL DEFAULT 0,
    visit_count INTEGER NOT NULL DEFAULT 0,
    store_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customers_store_id ON customers(store_id);
CREATE INDEX IF NOT EXISTS idx_customers_store_phone ON customers(store_id, phone);

selectByStoreId:
SELECT * FROM customers WHERE store_id = ? ORDER BY name ASC;

searchByNameOrPhone:
SELECT * FROM customers
WHERE store_id = ? AND (name LIKE '%' || ? || '%' OR phone LIKE '%' || ? || '%')
ORDER BY name ASC;

selectById:
SELECT * FROM customers WHERE id = ?;

insertOrReplace:
INSERT OR REPLACE INTO customers (id, name, phone, email, notes, total_spent, visit_count, store_id, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteById:
DELETE FROM customers WHERE id = ?;
```

- [ ] **Step 2: Update Customer entity**

Replace `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/entity/Customer.kt`:
```kotlin
package com.dokonpro.shared.domain.entity

data class Customer(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val notes: String?,
    val totalSpent: Double,
    val visitCount: Int,
    val storeId: String,
    val createdAt: String,
    val updatedAt: String
)
```

- [ ] **Step 3: Verify and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:generateCommonMainDokonProDatabaseInterface
git add shared/src/commonMain/
git commit -m "feat(customers): add SQLDelight customer table and expanded Customer entity"
```

---

## Task 3: KMP — CustomerRepository + Use Cases + Tests

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/repository/CustomerRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetCustomersUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/CreateCustomerUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/UpdateCustomerUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/SearchCustomerUseCase.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetCustomerPurchasesUseCase.kt`
- Create: `shared/src/commonTest/kotlin/com/dokonpro/shared/domain/usecase/CustomerUseCaseTest.kt`

- [ ] **Step 1: Create CustomerRepository interface**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/repository/CustomerRepository.kt`:
```kotlin
package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getCustomers(storeId: String): Flow<List<Customer>>
    fun searchCustomers(storeId: String, query: String): Flow<List<Customer>>
    suspend fun getCustomerById(id: String): Customer?
    suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer>
    suspend fun updateCustomer(customer: Customer): Result<Customer>
    suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
}
```

- [ ] **Step 2: Create use cases**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetCustomersUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

class GetCustomersUseCase(private val repository: CustomerRepository) {
    operator fun invoke(storeId: String): Flow<List<Customer>> =
        repository.getCustomers(storeId)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/CreateCustomerUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository

class CreateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(storeId: String, customer: Customer): Result<Customer> =
        repository.createCustomer(storeId, customer)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/UpdateCustomerUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository

class UpdateCustomerUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(customer: Customer): Result<Customer> =
        repository.updateCustomer(customer)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/SearchCustomerUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow

class SearchCustomerUseCase(private val repository: CustomerRepository) {
    operator fun invoke(storeId: String, query: String): Flow<List<Customer>> =
        repository.searchCustomers(storeId, query)
}
```

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/domain/usecase/GetCustomerPurchasesUseCase.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.CustomerRepository

class GetCustomerPurchasesUseCase(private val repository: CustomerRepository) {
    suspend operator fun invoke(storeId: String, customerId: String): Result<List<Sale>> =
        repository.getCustomerPurchases(storeId, customerId)
}
```

- [ ] **Step 3: Create tests**

Create `shared/src/commonTest/kotlin/com/dokonpro/shared/domain/usecase/CustomerUseCaseTest.kt`:
```kotlin
package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeCustomerRepository : CustomerRepository {
    val customers = mutableListOf<Customer>()

    override fun getCustomers(storeId: String): Flow<List<Customer>> =
        flowOf(customers.filter { it.storeId == storeId })

    override fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> =
        flowOf(customers.filter { it.storeId == storeId && (it.name.contains(query, true) || it.phone?.contains(query) == true) })

    override suspend fun getCustomerById(id: String): Customer? =
        customers.find { it.id == id }

    override suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer> {
        customers.add(customer); return Result.success(customer)
    }

    override suspend fun updateCustomer(customer: Customer): Result<Customer> {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx >= 0) customers[idx] = customer
        return Result.success(customer)
    }

    override suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>> =
        Result.success(emptyList())

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = Result.success(Unit)
}

private fun testCustomer(id: String = "c1", name: String = "Али Ахмедов") = Customer(
    id = id, name = name, phone = "+992901234567", email = null, notes = null,
    totalSpent = 1500.0, visitCount = 12, storeId = "store-1",
    createdAt = "2026-01-01", updatedAt = "2026-01-01"
)

class GetCustomersUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = GetCustomersUseCase(repo)

    @Test
    fun `should return customers for store`() = runTest {
        repo.customers.add(testCustomer())
        val result = useCase("store-1").first()
        assertEquals(1, result.size)
        assertEquals("Али Ахмедов", result[0].name)
    }
}

class CreateCustomerUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = CreateCustomerUseCase(repo)

    @Test
    fun `should create customer`() = runTest {
        val result = useCase("store-1", testCustomer())
        assertTrue(result.isSuccess)
        assertEquals(1, repo.customers.size)
    }
}

class SearchCustomerUseCaseTest {
    private val repo = FakeCustomerRepository()
    private val useCase = SearchCustomerUseCase(repo)

    @Test
    fun `should find customer by name`() = runTest {
        repo.customers.add(testCustomer())
        repo.customers.add(testCustomer(id = "c2", name = "Бобо Саидов"))
        val result = useCase("store-1", "али").first()
        assertEquals(1, result.size)
        assertEquals("Али Ахмедов", result[0].name)
    }

    @Test
    fun `should find customer by phone`() = runTest {
        repo.customers.add(testCustomer())
        val result = useCase("store-1", "901234").first()
        assertEquals(1, result.size)
    }
}
```

- [ ] **Step 4: Run tests and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
git add shared/src/
git commit -m "feat(customers): add CustomerRepository, use cases, and tests"
```

---

## Task 4: KMP — Customer Data Layer + DI

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/CustomerDtos.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/CustomerApiClient.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/CustomerLocalDataSource.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/CustomerRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/CustomerModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`

- [ ] **Step 1: Create Customer DTOs**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/dto/CustomerDtos.kt`:
```kotlin
package com.dokonpro.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CustomerDto(
    val id: String, val name: String, val phone: String? = null,
    val email: String? = null, val notes: String? = null,
    val totalSpent: Double = 0.0, val visitCount: Int = 0,
    val storeId: String, val createdAt: String, val updatedAt: String
)

@Serializable
data class CustomerListResponse(val customers: List<CustomerDto>, val nextCursor: String? = null)

@Serializable
data class CreateCustomerRequest(val name: String, val phone: String? = null, val email: String? = null, val notes: String? = null)

@Serializable
data class UpdateCustomerRequest(val name: String? = null, val phone: String? = null, val email: String? = null, val notes: String? = null)
```

- [ ] **Step 2: Create CustomerApiClient**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/remote/CustomerApiClient.kt`:
```kotlin
package com.dokonpro.shared.data.remote

import com.dokonpro.shared.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CustomerApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private fun authHeader(): String = "Bearer ${tokenProvider() ?: ""}"

    suspend fun getCustomers(storeId: String, search: String? = null): CustomerListResponse =
        client.get("$baseUrl/stores/$storeId/customers") {
            header("Authorization", authHeader())
            search?.let { parameter("search", it) }
        }.body()

    suspend fun createCustomer(storeId: String, request: CreateCustomerRequest): CustomerDto =
        client.post("$baseUrl/stores/$storeId/customers") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun updateCustomer(storeId: String, customerId: String, request: UpdateCustomerRequest): CustomerDto =
        client.patch("$baseUrl/stores/$storeId/customers/$customerId") {
            header("Authorization", authHeader()); contentType(ContentType.Application.Json); setBody(request)
        }.body()

    suspend fun getPurchases(storeId: String, customerId: String): List<SaleDto> =
        client.get("$baseUrl/stores/$storeId/customers/$customerId/purchases") {
            header("Authorization", authHeader())
        }.body()
}
```

- [ ] **Step 3: Create CustomerLocalDataSource**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/local/CustomerLocalDataSource.kt`:
```kotlin
package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerLocalDataSource(private val db: DokonProDatabase) {

    fun getCustomers(storeId: String): Flow<List<Customer>> =
        db.customerQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toCustomer() } }

    fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> =
        db.customerQueries.searchByNameOrPhone(storeId, query, query).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toCustomer() } }

    fun getCustomerById(id: String): Customer? =
        db.customerQueries.selectById(id).executeAsOneOrNull()?.toCustomer()

    fun insertCustomer(customer: Customer) {
        db.customerQueries.insertOrReplace(
            id = customer.id, name = customer.name, phone = customer.phone,
            email = customer.email, notes = customer.notes,
            total_spent = customer.totalSpent, visit_count = customer.visitCount.toLong(),
            store_id = customer.storeId, created_at = customer.createdAt, updated_at = customer.updatedAt
        )
    }

    fun insertCustomers(customers: List<Customer>) {
        db.transaction { customers.forEach { insertCustomer(it) } }
    }

    private fun com.dokonpro.shared.db.Customers.toCustomer() = Customer(
        id = id, name = name, phone = phone, email = email, notes = notes,
        totalSpent = total_spent, visitCount = visit_count.toInt(),
        storeId = store_id, createdAt = created_at, updatedAt = updated_at
    )
}
```

- [ ] **Step 4: Create CustomerRepositoryImpl**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/data/repository/CustomerRepositoryImpl.kt`:
```kotlin
package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.CustomerLocalDataSource
import com.dokonpro.shared.data.remote.CustomerApiClient
import com.dokonpro.shared.data.remote.dto.CreateCustomerRequest
import com.dokonpro.shared.data.remote.dto.UpdateCustomerRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.*
import com.dokonpro.shared.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomerRepositoryImpl(
    private val local: CustomerLocalDataSource,
    private val api: CustomerApiClient,
    private val syncQueue: SyncQueue
) : CustomerRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getCustomers(storeId: String): Flow<List<Customer>> = local.getCustomers(storeId)
    override fun searchCustomers(storeId: String, query: String): Flow<List<Customer>> = local.searchCustomers(storeId, query)
    override suspend fun getCustomerById(id: String): Customer? = local.getCustomerById(id)

    override suspend fun createCustomer(storeId: String, customer: Customer): Result<Customer> = runCatching {
        local.insertCustomer(customer)
        val request = CreateCustomerRequest(customer.name, customer.phone, customer.email, customer.notes)
        syncQueue.enqueue("customers", "$storeId:${customer.id}", "CREATE", json.encodeToString(request))
        customer
    }

    override suspend fun updateCustomer(customer: Customer): Result<Customer> = runCatching {
        local.insertCustomer(customer)
        val request = UpdateCustomerRequest(customer.name, customer.phone, customer.email, customer.notes)
        syncQueue.enqueue("customers", "${customer.storeId}:${customer.id}", "UPDATE", json.encodeToString(request))
        customer
    }

    override suspend fun getCustomerPurchases(storeId: String, customerId: String): Result<List<Sale>> = runCatching {
        val dtos = api.getPurchases(storeId, customerId)
        dtos.map { dto ->
            Sale(dto.id, dto.totalAmount, dto.discount, PaymentMethod.valueOf(dto.paymentMethod),
                dto.customerId, dto.storeId, dto.isRefunded, dto.createdAt,
                dto.items.map { SaleItem(it.id, it.saleId, it.productId, it.name, it.quantity, it.price, it.discount) })
        }
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val response = api.getCustomers(storeId)
        val customers = response.customers.map { dto ->
            Customer(dto.id, dto.name, dto.phone, dto.email, dto.notes,
                dto.totalSpent, dto.visitCount, dto.storeId, dto.createdAt, dto.updatedAt)
        }
        local.insertCustomers(customers)
    }
}
```

- [ ] **Step 5: Create Koin CustomerModule and update SharedModule**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/di/CustomerModule.kt`:
```kotlin
package com.dokonpro.shared.di

import com.dokonpro.shared.data.local.CustomerLocalDataSource
import com.dokonpro.shared.data.remote.CustomerApiClient
import com.dokonpro.shared.data.repository.CustomerRepositoryImpl
import com.dokonpro.shared.domain.repository.CustomerRepository
import com.dokonpro.shared.domain.usecase.*
import org.koin.dsl.module

val customerModule = module {
    single { CustomerLocalDataSource(get()) }
    single {
        CustomerApiClient(client = get(), baseUrl = "http://10.0.2.2:3000",
            tokenProvider = { get<com.dokonpro.shared.data.local.TokenStorage>().getTokens()?.accessToken })
    }
    single<CustomerRepository> { CustomerRepositoryImpl(get(), get(), get()) }
    factory { GetCustomersUseCase(get()) }
    factory { CreateCustomerUseCase(get()) }
    factory { UpdateCustomerUseCase(get()) }
    factory { SearchCustomerUseCase(get()) }
    factory { GetCustomerPurchasesUseCase(get()) }
}
```

Update `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`:
```kotlin
package com.dokonpro.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module = module {
    includes(authModule, databaseModule, productModule, salesModule, customerModule)
}
```

- [ ] **Step 6: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
git add shared/src/
git commit -m "feat(customers): add Customer data layer, API client, repository, and Koin module"
```

---

## Task 5: Android — CustomerViewModel + Screens

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/viewmodel/CustomerViewModel.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/customers/CustomerListScreen.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/customers/CustomerDetailScreen.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/customers/AddEditCustomerScreen.kt`

- [ ] **Step 1: Create CustomerViewModel**

Create `androidApp/src/main/java/com/dokonpro/android/viewmodel/CustomerViewModel.kt`:
```kotlin
package com.dokonpro.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokonpro.shared.domain.entity.Customer
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CustomerListState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CustomerDetailState(
    val customer: Customer? = null,
    val purchases: List<Sale> = emptyList(),
    val isLoading: Boolean = false
)

class CustomerViewModel(
    private val getCustomers: GetCustomersUseCase,
    private val createCustomer: CreateCustomerUseCase,
    private val updateCustomer: UpdateCustomerUseCase,
    private val searchCustomers: SearchCustomerUseCase,
    private val getPurchases: GetCustomerPurchasesUseCase,
    private val storeId: String
) : ViewModel() {

    private val _listState = MutableStateFlow(CustomerListState())
    val listState: StateFlow<CustomerListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerDetailState())
    val detailState: StateFlow<CustomerDetailState> = _detailState.asStateFlow()

    init { loadCustomers() }

    private fun loadCustomers() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true)
            getCustomers(storeId).collect { customers ->
                _listState.value = _listState.value.copy(customers = customers, isLoading = false)
            }
        }
    }

    fun onSearchChange(query: String) {
        _listState.value = _listState.value.copy(searchQuery = query)
        viewModelScope.launch {
            val flow = if (query.isBlank()) getCustomers(storeId) else searchCustomers(storeId, query)
            flow.collect { _listState.value = _listState.value.copy(customers = it) }
        }
    }

    fun addCustomer(name: String, phone: String?, email: String?, notes: String?) {
        val now = Clock.System.now().toString()
        val customer = Customer(
            id = "cust-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}",
            name = name, phone = phone, email = email, notes = notes,
            totalSpent = 0.0, visitCount = 0, storeId = storeId,
            createdAt = now, updatedAt = now
        )
        viewModelScope.launch {
            createCustomer(storeId, customer)
                .onFailure { _listState.value = _listState.value.copy(error = it.message) }
        }
    }

    fun loadCustomerDetail(customerId: String) {
        viewModelScope.launch {
            _detailState.value = CustomerDetailState(isLoading = true)
            val customers = _listState.value.customers
            val customer = customers.find { it.id == customerId }
            _detailState.value = _detailState.value.copy(customer = customer, isLoading = false)

            getPurchases(storeId, customerId)
                .onSuccess { _detailState.value = _detailState.value.copy(purchases = it) }
        }
    }

    fun clearError() { _listState.value = _listState.value.copy(error = null) }
}
```

- [ ] **Step 2: Create CustomerListScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/customers/CustomerListScreen.kt`:
```kotlin
package com.dokonpro.android.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokonpro.android.R
import com.dokonpro.shared.domain.entity.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customers: List<Customer>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onCustomerClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_customers)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, stringResource(R.string.customer_add))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.customer_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (customers.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(R.string.customer_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerListItem(customer) { onCustomerClick(customer.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerListItem(customer: Customer, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(16.dp), MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                customer.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${customer.totalSpent} с", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${customer.visitCount} ${stringResource(R.string.customer_visits)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 3: Create CustomerDetailScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/customers/CustomerDetailScreen.kt`:
```kotlin
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
fun CustomerDetailScreen(
    customer: Customer?,
    purchases: List<Sale>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: stringResource(R.string.customer_detail)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.product_edit)) } }
            )
        }
    ) { padding ->
        if (customer == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(stringResource(R.string.customer_not_found)) }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                // Profile card
                Surface(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.primaryContainer, Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        customer.phone?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        customer.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }

                Spacer(Modifier.height(12.dp))
                // Stats
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                    StatCard(stringResource(R.string.customer_total_spent), "${customer.totalSpent} с", Modifier.weight(1f))
                    StatCard(stringResource(R.string.customer_visits), "${customer.visitCount}", Modifier.weight(1f))
                }

                customer.notes?.let {
                    Spacer(Modifier.height(12.dp))
                    Surface(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surfaceVariant, Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.customer_notes), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.customer_purchases), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            if (purchases.isEmpty()) {
                item {
                    Text(stringResource(R.string.customer_no_purchases), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(purchases, key = { it.id }) { sale ->
                    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("#${sale.id.takeLast(8).uppercase()}", fontWeight = FontWeight.SemiBold)
                                Text("${sale.items.size} ${stringResource(R.string.pos_items)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${sale.totalAmount} с", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 4: Create AddEditCustomerScreen**

Create `androidApp/src/main/java/com/dokonpro/android/ui/customers/AddEditCustomerScreen.kt`:
```kotlin
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    onSave: (name: String, phone: String?, email: String?, notes: String?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.customer_add)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.customer_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.customer_phone)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.customer_email)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.customer_notes)) },
                modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp))

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onSave(name, phone.ifBlank { null }, email.ifBlank { null }, notes.ifBlank { null }) },
                enabled = name.length >= 2,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.save), fontSize = 16.sp) }

            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add androidApp/src/main/java/com/dokonpro/android/viewmodel/CustomerViewModel.kt
git add androidApp/src/main/java/com/dokonpro/android/ui/customers/
git commit -m "feat(customers): add CustomerViewModel and customer Compose screens"
```

---

## Task 6: Android — Customer Strings + Navigation + DI + POS Integration

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml`
- Modify: `androidApp/src/main/res/values-tg/strings.xml`
- Modify: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`
- Modify: `androidApp/src/main/java/com/dokonpro/android/viewmodel/POSViewModel.kt`

- [ ] **Step 1: Add customer strings (Russian)**

Add before closing `</resources>` in `androidApp/src/main/res/values/strings.xml`:
```xml
    <!-- Customers -->
    <string name="customer_add">Добавить клиента</string>
    <string name="customer_detail">Клиент</string>
    <string name="customer_search_hint">Поиск по имени или телефону…</string>
    <string name="customer_empty">Нет клиентов</string>
    <string name="customer_not_found">Клиент не найден</string>
    <string name="customer_name">Имя</string>
    <string name="customer_phone">Телефон</string>
    <string name="customer_email">Email</string>
    <string name="customer_notes">Заметки</string>
    <string name="customer_total_spent">Всего потрачено</string>
    <string name="customer_visits">визитов</string>
    <string name="customer_purchases">История покупок</string>
    <string name="customer_no_purchases">Нет покупок</string>
    <string name="pos_select_customer">Выбрать клиента</string>
```

- [ ] **Step 2: Add customer strings (Tajik)**

Add before closing `</resources>` in `androidApp/src/main/res/values-tg/strings.xml`:
```xml
    <!-- Customers -->
    <string name="customer_add">Илова кардани мизоҷ</string>
    <string name="customer_detail">Мизоҷ</string>
    <string name="customer_search_hint">Ҷустуҷӯ аз рӯи ном ё телефон…</string>
    <string name="customer_empty">Мизоҷон нест</string>
    <string name="customer_not_found">Мизоҷ ёфт нашуд</string>
    <string name="customer_name">Ном</string>
    <string name="customer_phone">Телефон</string>
    <string name="customer_email">Email</string>
    <string name="customer_notes">Қайдҳо</string>
    <string name="customer_total_spent">Ҳамагӣ харҷ</string>
    <string name="customer_visits">ташриф</string>
    <string name="customer_purchases">Таърихи харидҳо</string>
    <string name="customer_no_purchases">Харид нест</string>
    <string name="pos_select_customer">Интихоби мизоҷ</string>
```

- [ ] **Step 3: Update AppNavigation**

Add to Routes object:
```kotlin
const val CUSTOMERS = "customers"
const val CUSTOMER_DETAIL = "customers/{customerId}"
const val ADD_CUSTOMER = "customers/add"
```

Add composable routes using CustomerViewModel via koinViewModel() and collectAsStateWithLifecycle(). Follow same patterns as product routes.

- [ ] **Step 4: Update DokonProApp Koin module**

Add:
```kotlin
viewModel { CustomerViewModel(get(), get(), get(), get(), get(), "default-store") }
```

- [ ] **Step 5: Add customerId to POSViewModel**

Add to POSState:
```kotlin
val selectedCustomerId: String? = null
```

Add to POSViewModel:
```kotlin
fun setCustomer(customerId: String?) {
    _state.value = _state.value.copy(selectedCustomerId = customerId)
}
```

Update checkout() to pass `s.selectedCustomerId` to `completeSale()`.

- [ ] **Step 6: Build and commit**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
git add androidApp/
git commit -m "feat(customers): wire customer screens, navigation, strings, DI, and POS integration"
```

---

## Task 7: Build Verification + Tag + Push

- [ ] **Step 1: Run shared tests**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :shared:testDebugUnitTest
```

- [ ] **Step 2: Build Android**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :androidApp:assembleDebug
```

- [ ] **Step 3: Build backend**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && npm run build
```

- [ ] **Step 4: Tag and push**

```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git tag v0.5.0-customers
git push origin main --tags
```
