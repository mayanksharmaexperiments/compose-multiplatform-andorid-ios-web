---
name: navigation3-cmp
description: Enforces best practices for Google's declarative, state-driven Navigation 3 framework in Compose Multiplatform (CMP) projects.
---
# Navigation 3 in Compose Multiplatform (CMP)

This skill provides guidelines and patterns to implement **Navigation 3**, Google's modern state-driven, declarative navigation architecture, across all targets of your Compose Multiplatform project.

---

## 1. Defining Type-Safe Destinations (Routes)
*Instead of string-based paths (e.g., `"details/{id}"`), Navigation 3 uses pure Kotlin types or serialization data objects for routes. This ensures type safety and clean compile-time validations.*

```kotlin
package org.example.project.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    object Splash : Route
    
    @Serializable
    object ClientList : Route
    
    @Serializable
    data class ClientDetails(val clientId: Long) : Route
}
```

---

## 2. Declarative State-Driven Navigation Controller
*The navigation state (the backstack) is modeled simply as a `List<Route>` stream. This backstack can live inside your commonMain ViewModels or singletons, enabling business logic to drive navigation events directly.*

### Multiplatform Navigation Controller:
```kotlin
package org.example.project.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppNavController {
    private val _backstack = MutableStateFlow<List<Route>>(listOf(Route.Splash))
    val backstack: StateFlow<List<Route>> = _backstack.asStateFlow()

    fun navigateTo(route: Route) {
        _backstack.update { currentStack ->
            // Prevent pushing the same route consecutively
            if (currentStack.lastOrNull() == route) currentStack 
            else currentStack + route
        }
    }

    fun popBack(): Boolean {
        var popped = false
        _backstack.update { currentStack ->
            if (currentStack.size > 1) {
                popped = true
                currentStack.dropLast(1)
            } else {
                currentStack
            }
        }
        return popped
    }

    fun popToRoot() {
        _backstack.update { listOf(it.first()) }
    }
}
```

---

## 3. Rendering Screens Declaratively in Compose UI
*Observe the backstack flow inside your root Compose composable and render the active screen. You can map destinations cleanly and support standard system back gestures.*

```kotlin
package org.example.project.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import org.example.project.ui.navigation.AppNavController
import org.example.project.ui.navigation.Route

@Composable
fun MainNavigationGraph(
    navController: AppNavController = koinInject()
) {
    val backstack by navController.backstack.collectAsState()
    val currentRoute = backstack.lastOrNull() ?: Route.Splash

    // Dynamic, state-driven rendering
    when (currentRoute) {
        is Route.Splash -> SplashScreen(
            onTimeOut = { navController.navigateTo(Route.ClientList) }
        )
        is Route.ClientList -> ClientListScreen(
            onClientClick = { client -> 
                navController.navigateTo(Route.ClientDetails(client.id)) 
            }
        )
        is Route.ClientDetails -> ClientDetailScreen(
            clientId = currentRoute.clientId,
            onBackPress = { navController.popBack() }
        )
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Complex String Routes:** Avoid string routes (e.g. `"/client/detail?id=1"`) which bypass compiler safety checks. Always use serialized Kotlin data objects.
*   ❌ **Fragile Fragment Managers:** Avoid referencing Android `FragmentManager` or native controllers inside standard platform-independent presentation libraries.
*   ❌ **Hardcoded UI Navigation Action:** Do not write ad-hoc navigation bindings inside small UI items; delegate navigation routes up to parent view structures or centralized controllers.
