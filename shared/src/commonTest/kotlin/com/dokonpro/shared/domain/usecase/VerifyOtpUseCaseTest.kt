package com.dokonpro.shared.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifyOtpUseCaseTest {
    private val repository = FakeAuthRepository()
    private val useCase = VerifyOtpUseCase(repository)

    @Test
    fun `should verify OTP and return tokens`() = runTest {
        val result = useCase("+992901234567", "123456")
        assertTrue(result.isSuccess)
        assertEquals("+992901234567", repository.lastVerifyPhone)
        assertEquals("123456", repository.lastVerifyCode)
        assertFalse(result.getOrThrow().isNewUser)
    }

    @Test
    fun `should return failure for invalid OTP`() = runTest {
        repository.verifyOtpResult = Result.failure(RuntimeException("Invalid OTP"))
        val result = useCase("+992901234567", "000000")
        assertTrue(result.isFailure)
    }
}
