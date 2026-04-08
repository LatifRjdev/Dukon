package com.dokonpro.shared.domain.repository

import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.entity.ZakatConfig
import kotlinx.coroutines.flow.Flow

interface ZakatRepository {
    suspend fun calculate(storeId: String): Result<ZakatCalculation>
    suspend fun save(storeId: String): Result<ZakatCalculation>
    fun getHistory(storeId: String): Flow<List<ZakatCalculation>>
    suspend fun getConfig(storeId: String): Result<ZakatConfig>
    suspend fun updateConfig(storeId: String, goldRate: Double?, silverRate: Double?): Result<ZakatConfig>
}
