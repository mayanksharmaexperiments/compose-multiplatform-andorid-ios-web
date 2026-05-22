import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val supabaseUrl = System.getenv("SUPABASE_URL")
    ?: project.findProperty("supabase.url")?.toString()
    ?: localProperties.getProperty("supabase.url")
    ?: ""

val supabaseKey = System.getenv("SUPABASE_KEY")
    ?: project.findProperty("supabase.key")?.toString()
    ?: localProperties.getProperty("supabase.key")
    ?: ""

val generateConfigTask = tasks.register("generateConfig") {
    val outputDir = layout.buildDirectory.dir("generated/source/config/commonMain/kotlin/org/example/project/data/config").get().asFile
    outputs.dir(outputDir)
    
    val configOutputFile = File(outputDir, "Config.kt")
    val urlValue = supabaseUrl
    val keyValue = supabaseKey

    doLast {
        outputDir.mkdirs()
        configOutputFile.writeText("""
            package org.example.project.data.config
            
            object Config {
                const val SUPABASE_URL = "$urlValue"
                const val SUPABASE_KEY = "$keyValue"
            }
        """.trimIndent())
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "org.example.project.data"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
    }
    
    sourceSets {
        commonMain {
            kotlin.srcDir(generateConfigTask)
        }
        commonMain.dependencies {
            implementation(project(":domain"))
            
            // Supabase and Serialization
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
