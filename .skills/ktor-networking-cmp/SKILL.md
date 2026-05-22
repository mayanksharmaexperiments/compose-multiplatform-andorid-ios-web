---
name: ktor-networking-cmp
description: Enforces best practices for Ktor HTTP client configuration, network operations, serialization, and error handling in Compose Multiplatform (CMP) projects.
---
# Ktor Networking in Kotlin Multiplatform (KMP)

This skill provides guidelines and patterns to configure and execute network requests using **Ktor Client** across all targets in Compose Multiplatform.

---

## 1. Single HttpClient Setup with ContentNegotiation
*Declare a single, reusable `HttpClient` instance in your Koin graph configured with kotlinx.serialization and standard plugins.*

```kotlin
package org.example.project.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createKtorClient(): HttpClient {
    return HttpClient {
        // 1. ContentNegotiation for JSON parsing
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        
        // 2. Logging for dev debugging
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    println("KtorNetwork: $message")
                }
            }
        }

        // 3. Default Configuration (Base URL and Headers)
        defaultRequest {
            url("https://api.example.com/")
            contentType(ContentType.Application.Json)
        }
    }
}
```

---

## 2. API Request Structure & Return Types
*API requests live strictly in the Data layer. Implement Ktor calls within Repository classes using typed requests.*

```kotlin
package org.example.project.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.call.body
import org.example.project.data.model.ClientDto
import org.example.project.data.model.toDomain
import org.example.project.domain.model.Client
import org.example.project.domain.repository.ClientRepository

class ClientRepositoryImpl(
    private val httpClient: HttpClient
) : ClientRepository {
    override suspend fun getClients(): List<Client> {
        val dtos = handleNetworkCall<List<ClientDto>> {
            httpClient.get("v1/clients") {
                parameter("status", "active")
            }
        }
        return dtos.map { it.toDomain() }
    }
}
```

---

## 3. Asynchronous Safe Error Mapping
*Map generic HTTP/Ktor client exceptions to unified Domain-level exceptions so that ViewModels can present user-friendly error states without import leaks.*

### Unified Domain Exceptions:
```kotlin
package org.example.project.domain.model

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object NoConnectionException : DomainException("No internet connection. Please retry.")
    object UnauthorizedException : DomainException("Session expired. Please log in.")
    class ServerException(val code: Int, message: String) : DomainException("Server error ($code): $message")
    class UnknownNetworkException(cause: Throwable) : DomainException("An unexpected error occurred.", cause)
}
```

### Generic Call Wrapper:
```kotlin
package org.example.project.data.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import org.example.project.domain.model.DomainException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okio.IOException

suspend inline fun <reified T> handleNetworkCall(
    crossinline block: suspend () -> HttpResponse
): T = withContext(Dispatchers.Default) {
    try {
        val response = block()
        response.body<T>()
    } catch (e: ResponseException) {
        throw when (e.response.status.value) {
            401, 403 -> DomainException.UnauthorizedException
            else -> DomainException.ServerException(e.response.status.value, e.message)
        }
    } catch (e: IOException) {
        throw DomainException.NoConnectionException
    } catch (e: Exception) {
        throw DomainException.UnknownNetworkException(e)
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Platform-Specific Engines Leakage:** Avoid hardcoding specific networking engines (e.g. `OkHttp` or `Darwin`) inside `commonMain` HTTP setup directly unless done through DI or `expect`/`actual` structures.
*   ❌ **Leaking DTOs:** Never return Ktor `HttpResponse` or serializable DTOs to the Presentation or Domain layers. Always parse and map them immediately to domain entities.
*   ❌ **Blocking UI Threads:** Never execute Ktor network calls synchronously. Always launch them on asynchronous Coroutine dispatchers (`Dispatchers.Default`).
