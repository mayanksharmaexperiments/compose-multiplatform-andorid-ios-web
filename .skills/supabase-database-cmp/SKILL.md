---
name: supabase-database-cmp
description: Enforces best practices for Supabase Postgrest database integration, queries, data mapping, and error handling in Compose Multiplatform (CMP) projects.
---
# Supabase Database in Kotlin Multiplatform (KMP)

This skill provides guidelines and patterns to interact with a remote Supabase Postgres database using the official **`supabase-kt` (Postgrest)** client library.

---

## 1. Supabase Client Configuration & Injection
*Define a single `SupabaseClient` containing the Postgrest plugin, and inject the `Postgrest` instance via Koin.*

```kotlin
package org.example.project.data.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import org.example.project.config.Config

fun createSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) {
        // Install Postgres Remote Database support
        install(Postgrest)
    }
}
```

---

## 2. Query Patterns & Safe DTO Mappings
*Database operations are restricted to Repository implementations in the Data layer. Implement Postgrest calls using type-safe serialization models.*

### 1. Database DTO definitions:
```kotlin
package org.example.project.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.domain.model.Client

@Serializable
data class ClientDto(
    val id: Long,
    val name: String,
    @SerialName("phone_no")
    val phoneNo: String? = null,
    val address: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)

// Extension mapper to domain
fun ClientDto.toDomain() = Client(
    id = id,
    name = name,
    phoneNo = phoneNo,
    address = address,
    isActive = isActive
)
```

### 2. Querying Database with Filters:
```kotlin
package org.example.project.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import org.example.project.data.model.ClientDto
import org.example.project.data.model.toDomain
import org.example.project.domain.model.Client
import org.example.project.domain.repository.ClientRepository

class ClientRepositoryImpl(
    private val postgrest: Postgrest
) : ClientRepository {

    override suspend fun getActiveClients(): List<Client> {
        val dtos = postgrest["clients"].select(columns = Columns.ALL) {
            filter {
                // Type-safe column filtering
                eq("is_active", true)
            }
        }.decodeList<ClientDto>()
        
        return dtos.map { it.toDomain() }
    }

    override suspend fun insertClient(client: Client) {
        val dto = ClientDto(
            id = client.id,
            name = client.name,
            phoneNo = client.phoneNo,
            address = client.address,
            isActive = client.isActive
        )
        // Inserting data into table
        postgrest["clients"].insert(dto)
    }
}
```

---

## 3. Exception Handling & Mappings
*Wrap database interactions in safe trial handlers, translating remote db exceptions into clean domain models.*

```kotlin
import io.github.jan.supabase.exceptions.RestException
import org.example.project.domain.model.DomainException

suspend fun <T> safeDbQuery(queryBlock: suspend () -> T): T {
    return try {
        queryBlock()
    } catch (e: RestException) {
        // Translate Postgrest REST exceptions to domain-understandable errors
        throw DomainException.ServerException(e.hashCode(), e.message ?: "Database query failed")
    } catch (e: Exception) {
        throw DomainException.UnknownNetworkException(e)
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Exposing DTO Tables:** Never expose raw `ClientDto` rows or table definitions directly to Composable views or ViewModels.
*   ❌ **Hardcoded Database Strings:** Avoid scattering raw database column names all over your code; keep database table bindings enclosed inside the Data Repository module.
*   ❌ **Client Instantiation Leaks:** Never call `createSupabaseClient()` inside ViewModels or individual Compose items.
