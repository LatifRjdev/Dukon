package com.dokonpro.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dokonpro.shared.db.DokonProDatabase
import com.dokonpro.shared.domain.entity.ZakatCalculation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ZakatLocalDataSource(private val db: DokonProDatabase) {

    fun getHistory(storeId: String): Flow<List<ZakatCalculation>> =
        db.zakat_calculationQueries.selectByStoreId(storeId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toEntity() } }

    fun insert(calc: ZakatCalculation) {
        db.zakat_calculationQueries.insert(
            calc.id, calc.inventoryValue, calc.cashBalance,
            calc.receivables, calc.liabilities, calc.nisabThreshold, calc.zakatableAmount,
            calc.zakatDue, calc.goldRate, calc.storeId, calc.calculatedAt
        )
    }

    private fun com.dokonpro.shared.db.Zakat_calculations.toEntity() = ZakatCalculation(
        id = id,
        inventoryValue = inventory_value,
        cashBalance = cash_balance,
        receivables = receivables,
        liabilities = liabilities,
        nisabThreshold = nisab_threshold,
        zakatableAmount = zakatable_amount,
        zakatDue = zakat_due,
        goldRate = gold_rate,
        storeId = store_id,
        calculatedAt = calculated_at
    )
}
