---
name: security-secrets-cmp
description: Enforces KMP security best practices, secure environment configuration, credential storage (iOS Keychain / Android EncryptedSharedPreferences), and cryptography.
---
# Security & Secrets in Kotlin Multiplatform (KMP)

This skill provides guidelines and patterns to secure credentials, API keys, and configurations across Android, iOS, and other target platforms.

---

## 1. Multiplatform Config Generation (API Keys & Secrets)
*Never hardcode production API keys, Supabase URLs, or client secrets inside git-tracked codebase files. Instead, use local build configuration injection.*

### 🛠️ Approved Pattern (Gradle Config Generator):
Ensure your project contains a gradle generator task (like the one present in your `shared/build.gradle.kts`) which reads variables from environment variables or `local.properties` and outputs a build-ignored source class:

```kotlin
// Build-time generated Kotlin config (Config.kt) - Ignored in Git
package org.example.project.config

object Config {
    const val SUPABASE_URL = "https://example.supabase.co"
    const val SUPABASE_KEY = "your-encrypted-api-key-here"
}
```

---

## 2. Secure Local Storage (Tokens & Session credentials)
*For sensitive data (like JWT tokens, passwords, and biometrics), standard Datastore preferences are insecure. Use native platform security mechanisms (iOS Keychain / Android EncryptedSharedPreferences) via a unified KMP Settings interface.*

### KMP Unified Encrypted Storage Implementation:
Leverage the standard KMP library `multiplatform-settings-secure` or implement expected secure storage APIs.

### 1. Declaring Secure Settings Contract in `commonMain`:
```kotlin
// commonMain
package org.example.project.data.security

import com.russhwolf.settings.Settings

interface SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String = ""): String
    fun remove(key: String)
    fun clear()
}
```

### 2. Native Platform Delegations:
```kotlin
// androidMain
package org.example.project.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SecureStorageImpl(private val context: Context) : SecureStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_app_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val delegateSettings = SharedPreferencesSettings(sharedPreferences)

    override fun putString(key: String, value: String) = delegateSettings.putString(key, value)
    override fun getString(key: String, defaultValue: String) = delegateSettings.getString(key, defaultValue)
    override fun remove(key: String) = delegateSettings.remove(key)
    override fun clear() = delegateSettings.clear()
}
```

```kotlin
// iosMain
package org.example.project.data.security

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

actual class SecureStorageImpl : SecureStorage {
    // KeychainSettings automatically wraps Apple's Keychain services securely
    private val delegateSettings = KeychainSettings()

    override fun putString(key: String, value: String) = delegateSettings.putString(key, value)
    override fun getString(key: String, defaultValue: String) = delegateSettings.getString(key, defaultValue)
    override fun remove(key: String) = delegateSettings.remove(key)
    override fun clear() = delegateSettings.clear()
}
```

---

## 3. Safe Transport Security (Network)
*Verify all network connections utilize Secure Sockets Layer (HTTPS) and strictly avoid allowing cleartext HTTP traffic.*

*   **HTTPS Requirement:** All endpoint URLs inside Ktor or Supabase builders must start with `https://`.
*   **Android Network Security Config:** Always declare `<application android:usesCleartextTraffic="false">` in `AndroidManifest.xml`.
*   **iOS App Transport Security:** Ensure Apple's `NSAppTransportSecurity` in your `Info.plist` is locked down to block arbitrary plaintext HTTP loads.

---

## Core Constraints and Anti-Patterns
*   ❌ **API Keys in Git:** Never commit plain text environment values, keys, or passwords to code files. Keep `local.properties` and generated `Config.kt` inside your `.gitignore`.
*   ❌ **JWT Tokens in Datastore:** Never write persistent session tokens or authentication secrets into standard `Datastore` preferences files; secure them in the OS Keychain/EncryptedSharedPreferences.
*   ❌ **Weak Cryptography:** Avoid roll-your-own cryptography algorithms (like local Base64 encoding or manual XOR mapping). Always rely on platform-supplied secure storage and GCM algorithms.
