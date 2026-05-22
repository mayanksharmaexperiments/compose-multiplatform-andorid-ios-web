---
name: datastore-preferences-cmp
description: Enforces best practices for Jetpack DataStore preferences, platform-specific builders, and flow-based key-value storage in Compose Multiplatform (CMP) projects.
---
# Jetpack DataStore Preferences in Kotlin Multiplatform (KMP)

This skill provides guidelines and patterns to configure asynchronous key-value storage using **Jetpack DataStore** across target platforms.

---

## 1. Creating Platform-Independent DataStore Builders
*DataStore initialization requires a platform-specific file path. Define an `expect` builder in commonMain and implement in native modules.*

### 1. Common main builder contract:
```kotlin
// commonMain
package org.example.project.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )
}
```

### 2. Platform Implementations:
```kotlin
// androidMain
package org.example.project.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

actual fun getLocalDataStore(): DataStore<Preferences> {
    val context = getAndroidContext()
    return createDataStore {
        context.filesDir.resolve("app_prefs.preferences_pb").absolutePath
    }
}
```

```kotlin
// iosMain
package org.example.project.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.UserDomainMask

actual fun getLocalDataStore(): DataStore<Preferences> {
    return createDataStore {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = UserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        (documentDirectory?.path ?: "") + "/app_prefs.preferences_pb"
    }
}
```

---

## 2. Managing DataStore Read/Write Safely
*Create a local data source wrapper class inside the Data layer to read and write typed preference keys via Flow.*

```kotlin
package org.example.project.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import okio.IOException

class AppPreferencesDataSource(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_KEY = booleanPreferencesKey("is_dark_theme")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    // 1. Safe Asynchronous Preference Reader
    val isDarkThemeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[THEME_KEY] ?: false
        }

    // 2. Safe Preference Writer
    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = enabled
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Blocking Thread Executions:** Never use blocking operations (like `.first()` or block-running) on the main thread; always retrieve preference streams using async Flows.
*   ❌ **Storing Sensitive Info in Plaintext:** Never store plain passwords, secure session tokens, or sensitive API keys inside standard Jetpack DataStore. Standard Datastores are stored in clear XML/Protobuf formats. For secure data, use platform keys (Keychain/EncryptedSharedPreferences).
*   ❌ **Hardcoded Path Strings:** Avoid scattering direct database or preferences file-writing logic around UI scopes. Keep them bound to repository modules.
