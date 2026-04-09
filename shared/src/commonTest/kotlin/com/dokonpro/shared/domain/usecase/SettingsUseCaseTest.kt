package com.dokonpro.shared.domain.usecase

import com.dokonpro.shared.domain.entity.StoreSettings
import com.dokonpro.shared.domain.repository.StoreSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeStoreSettingsRepository : StoreSettingsRepository {
    var settings = StoreSettings(
        id = "s1",
        name = "Test Store",
        address = "Dushanbe",
        phone = "+992901234567",
        currency = "TJS",
        logoUrl = null,
        receiptHeader = "Welcome!",
        receiptFooter = "Thank you!",
        storeId = "store-1"
    )

    override suspend fun getSettings(storeId: String): Result<StoreSettings> = Result.success(settings)

    override suspend fun updateSettings(storeId: String, settings: StoreSettings): Result<StoreSettings> {
        this.settings = settings
        return Result.success(settings)
    }
}

class GetStoreSettingsUseCaseTest {
    private val repo = FakeStoreSettingsRepository()
    private val useCase = GetStoreSettingsUseCase(repo)

    @Test
    fun `should return store settings`() = runTest {
        val result = useCase("store-1")
        assertTrue(result.isSuccess)
        assertEquals("Test Store", result.getOrThrow().name)
        assertEquals("TJS", result.getOrThrow().currency)
    }
}

class UpdateStoreSettingsUseCaseTest {
    private val repo = FakeStoreSettingsRepository()
    private val useCase = UpdateStoreSettingsUseCase(repo)

    @Test
    fun `should update settings`() = runTest {
        val updated = repo.settings.copy(name = "New Name", receiptHeader = "New Header")
        val result = useCase("store-1", updated)
        assertTrue(result.isSuccess)
        assertEquals("New Name", repo.settings.name)
        assertEquals("New Header", repo.settings.receiptHeader)
    }
}
