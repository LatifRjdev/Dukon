package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.ZakatCalculation
import com.dokonpro.shared.domain.entity.ZakatConfig
import com.dokonpro.shared.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeZakatRepository : ZakatRepository {
    val calculations = mutableListOf<ZakatCalculation>()
    var calcResult = ZakatCalculation(
        "z1", 50000.0, 20000.0, 0.0, 0.0, 63750.0, 6250.0, 156.25, 750.0, "store-1", "2026-04-08"
    )
    var config = ZakatConfig(750.0, 10.0)

    override suspend fun calculate(storeId: String): Result<ZakatCalculation> = Result.success(calcResult)
    override suspend fun save(storeId: String): Result<ZakatCalculation> {
        calculations.add(calcResult); return Result.success(calcResult)
    }
    override fun getHistory(storeId: String): Flow<List<ZakatCalculation>> = flowOf(calculations)
    override suspend fun getConfig(storeId: String): Result<ZakatConfig> = Result.success(config)
    override suspend fun updateConfig(storeId: String, goldRate: Double?, silverRate: Double?): Result<ZakatConfig> {
        config = config.copy(goldRatePerGram = goldRate ?: config.goldRatePerGram, silverRatePerGram = silverRate ?: config.silverRatePerGram)
        return Result.success(config)
    }
}

class CalculateZakatUseCaseTest {
    private val repo = FakeZakatRepository()
    private val useCase = CalculateZakatUseCase(repo)

    @Test
    fun `should calculate zakat`() = runTest {
        val result = useCase("store-1")
        assertTrue(result.isSuccess)
        val calc = result.getOrThrow()
        assertEquals(50000.0, calc.inventoryValue)
        assertEquals(156.25, calc.zakatDue)
        assertEquals(63750.0, calc.nisabThreshold) // 85g * 750 TJS
    }
}

class SaveZakatUseCaseTest {
    private val repo = FakeZakatRepository()
    private val useCase = SaveZakatCalculationUseCase(repo)

    @Test
    fun `should save calculation`() = runTest {
        val result = useCase("store-1")
        assertTrue(result.isSuccess)
        assertEquals(1, repo.calculations.size)
    }
}
