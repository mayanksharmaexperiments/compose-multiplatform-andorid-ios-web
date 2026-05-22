---
name: solid-principles
description: Enforces strict adherence to SOLID design principles within Kotlin and Compose Multiplatform (CMP) codebases for high maintainability, readability, and decoupling.
---
# SOLID Principles in Kotlin & Compose Multiplatform

This skill instructs the agent to strictly apply the SOLID design principles when writing or refactoring Kotlin and Compose Multiplatform code.

## 1. Single Responsibility Principle (SRP)
*Each class, function, or component must have exactly one reason to change, meaning it serves a single actor or responsibility.*

### Guidelines:
*   **Decouple UI and Logic:** Keep Composable functions entirely presentation-focused. ViewModels must handle state production, and Use Cases must handle business logic.
*   **Avoid Helper/Util Bloat:** Never create generic `Utils.kt` or `Helper.kt` classes. Instead, create focused, single-responsibility files or extension functions (e.g., `DateFormatter.kt` or `StringExtensions.kt`).
*   **Focused Use Cases:** Each Use Case in the domain layer must perform exactly *one* operation (e.g., `GetClientsUseCase`, `AuthenticateUserUseCase`).

### Example (Bad vs. Good):
```kotlin
// BAD: Violates SRP by doing network calling, parsing, caching, and state holding in the ViewModel
class BadViewModel : ViewModel() {
    fun fetchAndSaveUser(id: String) {
        viewModelScope.launch {
            val response = ktorClient.get("https://api.com/users/$id") // Network
            val user = json.decodeFromString<User>(response.bodyAsText()) // Parsing
            database.insertUser(user) // Caching/Storage
        }
    }
}

// GOOD: Strictly adheres to SRP
class GoodViewModel(
    private val fetchAndSaveUserUseCase: FetchAndSaveUserUseCase
) : ViewModel() {
    fun fetchUser(id: String) {
        viewModelScope.launch {
            fetchAndSaveUserUseCase(id)
        }
    }
}
```

---

## 2. Open/Closed Principle (OCP)
*Software entities (classes, modules, functions) must be open for extension but closed for modification.*

### Guidelines:
*   **Sealed Hierarchies for Known States:** Use `sealed class` or `sealed interface` for closed sets of states (e.g., UI States like `Loading`, `Success`, `Error`). This ensures compile-time exhaustive checks using `when`.
*   **Behavior Extension via Composition:** Prefer composing small interfaces or abstract classes over modifying existing complex classes when new behavior is introduced.
*   **Compose Slots Pattern:** In Compose, use slot-based APIs (e.g., taking `content: @Composable () -> Unit` parameters) to make UI components highly extensible without requiring parameter changes.

---

## 3. Liskov Substitution Principle (LSP)
*Objects of a superclass must be replaceable with objects of its subclasses without affecting the correctness of the program.*

### Guidelines:
*   **No Dummy Implementations:** Subclasses or mock/fake implementations must never throw `NotImplementedError` or `UnsupportedOperationException` for required methods.
*   **Maintain Preconditions/Postconditions:** Ensure that implementations of interfaces (e.g., Local and Remote data sources) behave consistently in terms of nullability and exceptions thrown.

---

## 4. Interface Segregation Principle (ISP)
*Clients should not be forced to depend on methods they do not use. Prefer small, highly-targeted interfaces over bloated ones.*

### Guidelines:
*   **Granular Interfaces:** Break down broad contracts. For instance, instead of a massive `UserServices` interface, segregate it into `UserAuthenticationService` and `UserProfileService`.
*   **Focused DAOs:** In Room, separate entity operations into specific DAOs rather than a single database-wide DAO.

---

## 5. Dependency Inversion Principle (DIP)
*High-level modules must not depend on low-level modules; both must depend on abstractions. Abstractions must not depend on details; details must depend on abstractions.*

### Guidelines:
*   **Depend on Interfaces:** ViewModels must depend on Use Case or Repository *interfaces*, never on concrete database classes, HTTP clients, or repository implementations.
*   **Constructor Injection:** Pass all dependencies through the constructor. Never instantiate network clients or database helper objects directly inside consumer classes.

### Anti-Patterns to Avoid:
*   ❌ Instantiating concrete Singletons directly using `object` references inside high-level modules (e.g., calling `ClientRepository.getClients()` statically instead of injecting `clientRepository: ClientRepository` interface).
*   ❌ Directly importing low-level network frameworks (e.g., Ktor/OkHttp) or storage libraries inside presentation viewmodels or domain entities.
