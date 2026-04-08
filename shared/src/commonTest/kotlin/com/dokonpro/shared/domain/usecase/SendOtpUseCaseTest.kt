package com.dokonpro.shared.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendOtpUseCaseTest {
    private val repository = FakeAuthRepository()
    private val useCase = SendOtpUseCase(repository)

    @Test
    fun `should send OTP to given phone number`() = runTest {
        val result = useCase("+992901234567")
        assertTrue(result.isSuccess)
        assertEquals("+992901234567", repository.lastSentPhone)
    }

    @Test
    fun `should return failure when repository fails`() = runTest {
        repository.sendOtpResult = Result.failure(RuntimeException("Network error"))
        val result = useCase("+992901234567")
        assertTrue(result.isFailure)
    }
}
