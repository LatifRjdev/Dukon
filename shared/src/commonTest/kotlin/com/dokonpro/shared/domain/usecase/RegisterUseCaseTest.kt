package com.dokonpro.shared.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterUseCaseTest {
    private val repository = FakeAuthRepository()
    private val useCase = RegisterUseCase(repository)

    @Test
    fun `should register user and return tokens with user and store`() = runTest {
        val result = useCase("+992901234567", "Latif", "My Shop")
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("Test", data.user.name)
        assertEquals("Test Store", data.store.name)
        assertEquals("test-access", data.tokens.accessToken)
    }
}
