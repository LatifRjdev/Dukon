package com.dokonpro.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun `should return non-empty platform name`() {
        val name = platformName()
        assertTrue(name.isNotBlank(), "Platform name should not be blank")
    }
}
