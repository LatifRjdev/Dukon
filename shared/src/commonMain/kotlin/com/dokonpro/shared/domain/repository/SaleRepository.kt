package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.Cart
import com.dokonpro.shared.domain.entity.PaymentMethod
import com.dokonpro.shared.domain.entity.Sale
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun completeSale(storeId: String, cart: Cart, discount: Double, paymentMethod: PaymentMethod, customerId: String?): Result<Sale>
    fun getSalesHistory(storeId: String): Flow<List<Sale>>
    suspend fun getSaleById(id: String): Sale?
    suspend fun refundSale(storeId: String, saleId: String): Result<Sale>
    suspend fun syncFromRemote(storeId: String): Result<Unit>
}
