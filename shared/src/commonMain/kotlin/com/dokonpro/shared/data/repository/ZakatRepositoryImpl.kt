package com.dokonpro.shared.data.repository

import com.dokonpro.shared.data.local.ZakatLocalDataSource
import com.dokonpro.shared.data.remote.ZakatApiClient
import com.dokonpro.shared.data.remote.dto.UpdateZakatConfigRequest
import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.entity.ZakatConfig
import com.dokonpro.shared.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow

class ZakatRepositoryImpl(
    private val local: ZakatLocalDataSource,
    private val api: ZakatApiClient
) : ZakatRepository {

    override suspend fun calculate(storeId: String): Result<ZakatCalculation> = runCatching {
        val dto = api.calculate(storeId)
        ZakatCalculation(
            id = dto.id ?: "",
            inventoryValue = dto.inventoryValue,
            cashBalance = dto.cashBalance,
            receivables = dto.receivables,
            liabilities = dto.liabilities,
            nisabThreshold = dto.nisabThreshold,
            zakatableAmount = dto.zakatableAmount,
            zakatDue = dto.zakatDue,
            goldRate = dto.goldRate,
            storeId = dto.storeId,
            calculatedAt = dto.calculatedAt ?: ""
        )
    }

    override suspend fun save(storeId: String): Result<ZakatCalculation> = runCatching {
        val dto = api.save(storeId)
        val calc = ZakatCalculation(
            id = dto.id ?: "",
            inventoryValue = dto.inventoryValue,
            cashBalance = dto.cashBalance,
            receivables = dto.receivables,
            liabilities = dto.liabilities,
            nisabThreshold = dto.nisabThreshold,
            zakatableAmount = dto.zakatableAmount,
            zakatDue = dto.zakatDue,
            goldRate = dto.goldRate,
            storeId = dto.storeId,
            calculatedAt = dto.calculatedAt ?: ""
        )
        local.insert(calc)
        calc
    }

    override fun getHistory(storeId: String): Flow<List<ZakatCalculation>> =
        local.getHistory(storeId)

    override suspend fun getConfig(storeId: String): Result<ZakatConfig> = runCatching {
        val dto = api.getConfig(storeId)
        ZakatConfig(dto.goldRatePerGram, dto.silverRatePerGram)
    }

    override suspend fun updateConfig(
        storeId: String,
        goldRate: Double?,
        silverRate: Double?
    ): Result<ZakatConfig> = runCatching {
        val dto = api.updateConfig(storeId, UpdateZakatConfigRequest(goldRate, silverRate))
        ZakatConfig(dto.goldRatePerGram, dto.silverRatePerGram)
    }
}
