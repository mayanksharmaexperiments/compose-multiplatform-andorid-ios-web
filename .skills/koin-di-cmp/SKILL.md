---
name: koin-di-cmp
description: Enforces best practices for Koin Dependency Injection in Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) projects.
---
# Koin Dependency Injection in Kotlin Multiplatform (KMP)

This skill provides guidelines and concrete patterns to implement **Koin** as the unified Dependency Injection framework in Kotlin Multiplatform and Compose Multiplatform codebases.

---

## 1. Modular DI Declarations in `commonMain`
*Split your dependency injection graph into focused modules (Network, Database, Repository, Use Case, and ViewModel) to maintain clean separation.*

```kotlin
// commonMain source set
package org.example.project.di

import org.koin.core.module.Module
import org.koin.dsl.module
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import org.example.project.data.repository.ClientRepositoryImpl
import org.example.project.domain.repository.ClientRepository
import org.example.project.domain.usecase.GetActiveClientsUseCase
import org.example.project.ui.ClientListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.bind

val networkModule = module {
    single { 
        createSupabaseClient(
            supabaseUrl = "YOUR_URL",
            supabaseKey = "YOUR_KEY"
        ) { install(Postgrest) }
    }
    single { get<SupabaseClient>().postgrest }
}

val repositoryModule = module {
    // Inject interface to achieve Dependency Inversion
    single<ClientRepository> { ClientRepositoryImpl(get()) }
}

val useCaseModule = module {
    factory { GetActiveClientsUseCase(get()) }
}

val viewModelModule = module {
    // Use Koin's multiplatform ViewModel support
    viewModel { ClientListViewModel(get()) }
}
```

---

## 2. Platform-Specific Injector Initialization
*Koin must be initialized inside the application startup process on all target platforms, enabling platform-specific modules if required.*

### Standard Initialization Setup in `commonMain`:
```kotlin
// commonMain
package org.example.project.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(additionalModules: List<Module> = emptyList(), appDeclaration: KoinAppDeclaration = {}) = 
    startKoin {
        appDeclaration()
        modules(
            networkModule,
            repositoryModule,
            useCaseModule,
            viewModelModule
        )
        modules(additionalModules)
    }
```

### Initializing on Android (in custom `Application` class):
```kotlin
// androidMain
package org.example.project

import android.app.Application
import org.example.project.di.initKoin
import org.koin.android.ext.koin.koinContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
```

### Initializing on iOS (in Swift App entry point or helper class):
```kotlin
// iosMain
package org.example.project

import org.example.project.di.initKoin

fun initKoinHelper() = initKoin()
```

---

## 3. Injecting Dependencies in Compose UI
*Use Compose-Koin integration libraries to fetch ViewModels and singletons cleanly.*

```kotlin
package org.example.project.ui

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ClientScreen() {
    // Safe injection of shared Androidx ViewModels inside Compose Multiplatform
    val viewModel: ClientListViewModel = koinViewModel()
    
    // Injecting standard services/utilities
    val analytics: AnalyticsHelper = koinInject()
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **No Staggered Singletons:** Never use `object` singletons to manage API services or repositories. Always use Koin's `single {}` definition and inject them.
*   ❌ **No Service Locator Abuse:** Avoid retrieving dependencies directly by calling `get()` or using `KoinComponent` helper traits inside models, adapters, or views unless absolutely necessary.
*   ❌ **Android Context Leaks:** Never pass an Android Context into `commonMain` repositories or use cases. Keep platform-specific contexts encapsulated inside `androidMain` modules.
