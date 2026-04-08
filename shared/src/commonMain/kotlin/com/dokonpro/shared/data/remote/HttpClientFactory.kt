package com.dokonpro.shared.data.remote

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
