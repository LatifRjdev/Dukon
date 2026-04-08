package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.SaleLocalDataSource
import com.dokonpro.shared.data.remote.SaleApiClient
import com.dokonpro.shared.data.remote.dto.CreateSaleItemRequest
import com.dokonpro.shared.data.remote.dto.CreateSaleRequest
import com.dokonpro.shared.data.sync.SyncQueue
import com.dokonpro.shared.domain.entity.*
import com.dokonpro.shared.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SaleRepositoryImpl(
    private val local: SaleLocalDataSource,
    private val api: SaleApiClient,
    private val syncQueue: SyncQueue
) : SaleRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun completeSale(
        storeId: String, cart: Cart, discount: Double, paymentMethod: PaymentMethod, customerId: String?
    ): Result<Sale> = runCatching {
        val now = Clock.System.now().toString()
        val saleId = "sale-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
        val total = cart.totalWithDiscount(discount)

        val items = cart.items.map { cartItem ->
            SaleItem(
                id = "si-${Clock.System.now().toEpochMilliseconds()}-${(100..999).random()}",
                saleId = saleId, productId = cartItem.productId, name = cartItem.name,
                quantity = cartItem.quantity, price = cartItem.price, discount = cartItem.discount
            )
        }

        val sale = Sale(saleId, total, discount, paymentMethod, customerId, storeId, false, now, items)
        local.insertSale(sale)

        val request = CreateSaleRequest(
            items = items.map { CreateSaleItemRequest(it.productId, it.name, it.quantity, it.price, it.discount) },
            totalAmount = total, discount = discount, paymentMethod = paymentMethod.name, customerId = customerId
        )
        syncQueue.enqueue("sales", "$storeId:$saleId", "CREATE", json.encodeToString(request))
        sale
    }

    override fun getSalesHistory(storeId: String): Flow<List<Sale>> = local.getSales(storeId)
    override suspend fun getSaleById(id: String): Sale? = local.getSaleById(id)

    override suspend fun refundSale(storeId: String, saleId: String): Result<Sale> = runCatching {
        api.refundSale(storeId, saleId)
        val refunded = local.getSaleById(saleId)?.copy(isRefunded = true)
            ?: throw IllegalStateException("Sale not found locally")
        local.insertSale(refunded)
        refunded
    }

    override suspend fun syncFromRemote(storeId: String): Result<Unit> = runCatching {
        val response = api.getSales(storeId)
        response.sales.forEach { dto ->
            val sale = Sale(dto.id, dto.totalAmount, dto.discount, PaymentMethod.valueOf(dto.paymentMethod),
                dto.customerId, dto.storeId, dto.isRefunded, dto.createdAt,
                dto.items.map { SaleItem(it.id, it.saleId, it.productId, it.name, it.quantity, it.price, it.discount) })
            local.insertSale(sale)
        }
    }
}
