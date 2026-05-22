---
name: compose-ui-best-practices
description: Enforces Jetpack Compose and Compose Multiplatform rendering performance, state hoisting, modifier optimization, stable list keys, and clean M3 themes.
---
# Declarative Compose UI Best Practices

This skill provides guidelines and patterns to build responsive, fluid, and highly optimized layouts using **Jetpack Compose** and **Compose Multiplatform (CMP)**.

---

## 1. Unified State Hoisting (Stateless vs. Stateful)
*Split your layout components into stateless presenters (handling pure layout) and stateful containers (binding ViewModel states). This facilitates maximum component reusability and isolated unit testing.*

```kotlin
// 1. Stateful Container Composable
@Composable
fun ClientScreen(viewModel: ClientViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ClientContent(
        uiState = state,
        onRetryClick = { viewModel.reload() }
    )
}

// 2. Stateless Presenter Composable
@Composable
fun ClientContent(
    uiState: ClientUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is ClientUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is ClientUiState.Error -> ErrorLayout(uiState.message, onRetryClick)
            is ClientUiState.Success -> ClientList(uiState.clients)
        }
    }
}
```

---

## 2. Recomposition Optimization & Lazy Lists
*Compose processes recompositions in phases: Composition, Layout, and Drawing. Optimize rendering by deferring state reads and utilizing stable lazy list configuration.*

### 🛠️ Approved Patterns:
*   **Stable Keys in Lazy Lists:** Always supply unique stable keys in lists to avoid redrawing the entire list when a single item is inserted, deleted, or shifted.
*   **List Content Type:** Use `contentType` inside lazy items to improve view recycling efficiency.
*   **Deferring State Reads via Lambdas:** When a state value is fast-changing (like a scroll offset or animation float), read it inside a lambda block (e.g. `offset { IntOffset(...) }` or `drawBehind { ... }`) to completely bypass the recomposition phase.

```kotlin
@Composable
fun ClientList(clients: List<Client>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = clients,
            key = { client -> client.id }, // 1. Stable key
            contentType = { "ClientCardItem" } // 2. Content Type for recycling
        ) { client ->
            ClientCard(client)
        }
    }
}

@Composable
fun ScrollSensitiveHeader(scrollState: LazyListState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            // GOOD: Deferring state read of scrollState using lambda offsets
            .offset { IntOffset(x = 0, y = -scrollState.firstVisibleItemIndex) }
            .background(MaterialTheme.colorScheme.primary)
    )
}
```

---

## 3. Throttling State with `derivedStateOf`
*Use `derivedStateOf` to compute derived states that change less frequently than their raw inputs (e.g., displaying a "Scroll to top" button only when scroll offset exceeds a threshold).*

```kotlin
@Composable
fun Dashboard(listState: LazyListState) {
    // Throttles changes: true/false state changes only twice, rather than recomposing on every pixel scroll
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    if (showScrollToTop) {
        ScrollToTopButton(onClick = { /* scroll to 0 */ })
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **State Reads in Composition Root:** Avoid reading active animation values or scroll positions directly inside parent composables. Always push state reads down to the layout/draw phases using lambdas.
*   ❌ **Unstable Keys:** Never rely on indices (e.g., `key = { index }`) inside lazy lists if items can change positions. This breaks state preservation.
*   ❌ **Composed Modifiers:** Avoid creating custom modifiers using the legacy `composed {}` API, as it allocates duplicate modifier states. Implement custom behaviors using optimized `Modifier.Node` APIs.
