package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.PaymentMethod
import com.dokonpro.shared.domain.entity.Sale
import com.dokonpro.shared.domain.entity.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaleLocalDataSource(private val db: DokonProDatabase) {

    fun getSales(storeId: String): Flow<List<Sale>> =
        db.saleQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toSale() } }

    fun getSaleById(id: String): Sale? =
        db.saleQueries.selectById(id).executeAsOneOrNull()?.toSale()

    fun insertSale(sale: Sale) {
        db.transaction {
            db.saleQueries.insert(
                id = sale.id, total_amount = sale.totalAmount, discount = sale.discount,
                payment_method = sale.paymentMethod.name, customer_id = sale.customerId,
                store_id = sale.storeId, is_refunded = if (sale.isRefunded) 1L else 0L,
                created_at = sale.createdAt
            )
            sale.items.forEach { item ->
                db.sale_itemQueries.insert(
                    id = item.id, sale_id = item.saleId, product_id = item.productId,
                    name = item.name, quantity = item.quantity.toLong(),
                    price = item.price, discount = item.discount
                )
            }
        }
    }

    private fun com.dokonpro.shared.db.Sales.toSale(): Sale {
        val saleItems = db.sale_itemQueries.selectBySaleId(id).executeAsList().map { row ->
            SaleItem(row.id, row.sale_id, row.product_id, row.name, row.quantity.toInt(), row.price, row.discount)
        }
        return Sale(id, total_amount, discount, PaymentMethod.valueOf(payment_method),
            customer_id, store_id, is_refunded != 0L, created_at, saleItems)
    }
}
