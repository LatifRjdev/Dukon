# DokonPro Native — Phase 1: Foundation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the complete project skeleton — Git repo, Gradle multi-module build, KMP shared module, Android Compose app shell, NestJS backend with Docker Compose — so that all subsequent vertical slices have a working foundation to build on.

**Architecture:** Gradle multi-module KMP project (`:shared` + `:androidApp`) with a separate NestJS backend in `api/`. The shared module uses Clean Architecture (domain/data/di). Android app uses Compose + Material 3 + Koin. Backend uses NestJS + Prisma + PostgreSQL + Redis via Docker Compose.

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.3, Compose BOM 2025.01.01, SQLDelight 2.0.2, Ktor 3.0.3, Koin 4.0.2, kotlinx.serialization 1.7.3, kotlinx.coroutines 1.9.0, NestJS 10, Prisma 6, Node 22, PostgreSQL 16, Redis 7

---

## File Structure

```
/
├── .gitignore
├── build.gradle.kts                          # Root — plugin declarations only
├── settings.gradle.kts                       # Module includes + plugin repos
├── gradle.properties                         # JVM args, Android/KMP flags
├── gradle/
│   └── libs.versions.toml                    # Version catalog
├── shared/
│   ├── build.gradle.kts                      # KMP module config
│   └── src/
│       ├── commonMain/kotlin/com/dokonpro/shared/
│       │   ├── Platform.kt                   # expect declaration
│       │   ├── domain/entity/.gitkeep
│       │   ├── domain/repository/.gitkeep
│       │   ├── domain/usecase/.gitkeep
│       │   ├── data/remote/.gitkeep
│       │   ├── data/local/.gitkeep
│       │   ├── data/sync/.gitkeep
│       │   └── di/SharedModule.kt            # Root Koin module
│       ├── commonTest/kotlin/com/dokonpro/shared/
│       │   └── PlatformTest.kt               # Verify shared tests run
│       ├── androidMain/kotlin/com/dokonpro/shared/
│       │   └── Platform.android.kt           # actual implementation
│       └── iosMain/kotlin/com/dokonpro/shared/
│           └── Platform.ios.kt               # actual implementation
├── androidApp/
│   ├── build.gradle.kts                      # Android app config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/dokonpro/android/
│       │   ├── DokonProApp.kt                # Application class + Koin init
│       │   ├── MainActivity.kt               # Single activity, Compose host
│       │   ├── navigation/AppNavigation.kt   # NavHost + routes
│       │   └── ui/
│       │       ├── theme/Theme.kt            # Material 3 theme
│       │       ├── theme/Color.kt            # Color palette
│       │       ├── theme/Type.kt             # Typography
│       │       └── MainScreen.kt             # Placeholder main screen
│       └── res/
│           ├── values/strings.xml            # Russian (default)
│           └── values-tg/strings.xml         # Tajik
├── api/
│   ├── docker-compose.yml                    # Postgres 16 + Redis 7
│   ├── .env                                  # DB connection vars (dev only)
│   ├── .env.example                          # Template without secrets
│   ├── package.json
│   ├── tsconfig.json
│   ├── nest-cli.json
│   └── src/
│       ├── main.ts                           # Bootstrap
│       ├── app.module.ts                     # Root module
│       ├── prisma/
│       │   ├── schema.prisma                 # Initial schema (User, Store, UserStore)
│       │   └── prisma.service.ts             # Prisma client provider
│       └── modules/
│           └── health/
│               ├── health.module.ts
│               └── health.controller.ts      # GET /health
└── docs/superpowers/specs/...                # Already exists
```

---

## Task 1: Git Init + .gitignore

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: Initialize git repo**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native && git init
```
Expected: `Initialized empty Git repository`

- [ ] **Step 2: Create .gitignore**

Create `.gitignore`:
```gitignore
# Kotlin / Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
local.properties

# Android
*.apk
*.aab
*.ap_
*.dex
.cxx/

# iOS
iosApp/build/
iosApp/*.xcworkspace/xcuserdata/
iosApp/*.xcodeproj/xcuserdata/
iosApp/Pods/
*.ipa
DerivedData/

# Node / NestJS
api/node_modules/
api/dist/
api/.env

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
.DS_Store

# SQLDelight
shared/src/commonMain/sqldelight/databases/
```

- [ ] **Step 3: Initial commit**

Run:
```bash
git add .gitignore CLAUDE.md .claude/ docs/
git commit -m "chore: initial project setup with CLAUDE.md and design spec"
```

---

## Task 2: Gradle Wrapper + Version Catalog

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`

- [ ] **Step 1: Generate Gradle wrapper**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
gradle wrapper --gradle-version 8.12
```

If `gradle` is not installed globally, download the wrapper manually:
```bash
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar "https://raw.githubusercontent.com/gradle/gradle/v8.12.0/gradle/wrapper/gradle-wrapper.jar"
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.12-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF
curl -L -o gradlew "https://raw.githubusercontent.com/gradle/gradle/v8.12.0/gradlew"
curl -L -o gradlew.bat "https://raw.githubusercontent.com/gradle/gradle/v8.12.0/gradlew.bat"
chmod +x gradlew
```

- [ ] **Step 2: Create version catalog**

Create `gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "2.1.0"
agp = "8.7.3"
composeBom = "2025.01.01"
composeNavigation = "2.8.6"
koin = "4.0.2"
ktor = "3.0.3"
sqldelight = "2.0.2"
kotlinxSerialization = "1.7.3"
kotlinxCoroutines = "1.9.0"
androidxCore = "1.15.0"
androidxLifecycle = "2.8.7"
androidxActivity = "1.9.3"
androidxSecurity = "1.1.0-alpha06"
junit = "4.13.2"
kotlinTest = "2.1.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "composeNavigation" }

# AndroidX
androidx-core = { group = "androidx.core", name = "core-ktx", version.ref = "androidxCore" }
androidx-lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "androidxLifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidxActivity" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "androidxSecurity" }

# Koin
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
koin-compose = { group = "io.insert-koin", name = "koin-androidx-compose", version.ref = "koin" }
koin-test = { group = "io.insert-koin", name = "koin-test", version.ref = "koin" }

# Ktor
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { group = "io.ktor", name = "ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-auth = { group = "io.ktor", name = "ktor-client-auth", version.ref = "ktor" }

# SQLDelight
sqldelight-android-driver = { group = "app.cash.sqldelight", name = "android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { group = "app.cash.sqldelight", name = "native-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { group = "app.cash.sqldelight", name = "coroutines-extensions", version.ref = "sqldelight" }

# Kotlinx
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }

# Testing
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlinTest" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 3: Create gradle.properties**

Create `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

android.useAndroidX=true
android.nonTransitiveRClass=true

kotlin.code.style=official
kotlin.mpp.androidSourceSetLayoutVersion=2
```

- [ ] **Step 4: Commit**

Run:
```bash
git add gradle/ gradle.properties gradlew gradlew.bat
git commit -m "chore: add Gradle wrapper 8.12 and version catalog"
```

---

## Task 3: Root Build Scripts

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`

- [ ] **Step 1: Create root build.gradle.kts**

Create `build.gradle.kts` (replace existing CLAUDE.md-only root — this is the Gradle root):
```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 2: Create settings.gradle.kts**

Create `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DokonPro"
include(":shared")
include(":androidApp")
```

- [ ] **Step 3: Commit**

Run:
```bash
git add build.gradle.kts settings.gradle.kts
git commit -m "chore: add root Gradle build scripts"
```

---

## Task 4: KMP Shared Module

**Files:**
- Create: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/Platform.kt`
- Create: `shared/src/commonTest/kotlin/com/dokonpro/shared/PlatformTest.kt`
- Create: `shared/src/androidMain/kotlin/com/dokonpro/shared/Platform.android.kt`
- Create: `shared/src/iosMain/kotlin/com/dokonpro/shared/Platform.ios.kt`
- Create: `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`
- Create: `.gitkeep` files for empty package dirs

- [ ] **Step 1: Create shared/build.gradle.kts**

Create `shared/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "com.dokonpro.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("DokonProDatabase") {
            packageName.set("com.dokonpro.shared.db")
        }
    }
}
```

- [ ] **Step 2: Create Platform expect/actual declarations**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/Platform.kt`:
```kotlin
package com.dokonpro.shared

expect fun platformName(): String
```

Create `shared/src/androidMain/kotlin/com/dokonpro/shared/Platform.android.kt`:
```kotlin
package com.dokonpro.shared

actual fun platformName(): String = "Android"
```

Create `shared/src/iosMain/kotlin/com/dokonpro/shared/Platform.ios.kt`:
```kotlin
package com.dokonpro.shared

import platform.UIKit.UIDevice

actual fun platformName(): String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
```

- [ ] **Step 3: Create shared test**

Create `shared/src/commonTest/kotlin/com/dokonpro/shared/PlatformTest.kt`:
```kotlin
package com.dokonpro.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun `should return non-empty platform name`() {
        val name = platformName()
        assertTrue(name.isNotBlank(), "Platform name should not be blank")
    }
}
```

- [ ] **Step 4: Create Koin shared module**

Create `shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt`:
```kotlin
package com.dokonpro.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule: Module = module {
    // Feature modules will be added here as vertical slices are built
}
```

- [ ] **Step 5: Create empty package directories with .gitkeep**

Run:
```bash
BASE="shared/src/commonMain/kotlin/com/dokonpro/shared"
for dir in domain/entity domain/repository domain/usecase data/remote data/local data/sync data/repository; do
    mkdir -p "$BASE/$dir"
    touch "$BASE/$dir/.gitkeep"
done
mkdir -p shared/src/commonMain/sqldelight/com/dokonpro/shared/db
touch shared/src/commonMain/sqldelight/com/dokonpro/shared/db/.gitkeep
```

- [ ] **Step 6: Commit**

Run:
```bash
git add shared/
git commit -m "feat: add KMP shared module with Clean Architecture package structure"
```

---

## Task 5: Android App Module — Build Config

**Files:**
- Create: `androidApp/build.gradle.kts`
- Create: `androidApp/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create androidApp/build.gradle.kts**

Create `androidApp/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.dokonpro.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dokonpro.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.security.crypto)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
```

- [ ] **Step 2: Create AndroidManifest.xml**

Create `androidApp/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".DokonProApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.DokonPro">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.DokonPro">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Create proguard-rules.pro and themes**

Create `androidApp/proguard-rules.pro`:
```proguard
# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
```

Create `androidApp/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DokonPro" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 4: Commit**

Run:
```bash
git add androidApp/
git commit -m "feat: add Android app module with Compose and Koin config"
```

---

## Task 6: Android App — Application + MainActivity

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/MainActivity.kt`

- [ ] **Step 1: Create Application class with Koin**

Create `androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt`:
```kotlin
package com.dokonpro.android

import android.app.Application
import com.dokonpro.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DokonProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@DokonProApp)
            modules(sharedModule)
        }
    }
}
```

- [ ] **Step 2: Create MainActivity**

Create `androidApp/src/main/java/com/dokonpro/android/MainActivity.kt`:
```kotlin
package com.dokonpro.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dokonpro.android.navigation.AppNavigation
import com.dokonpro.android.ui.theme.DokonProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DokonProTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

Run:
```bash
git add androidApp/src/main/java/
git commit -m "feat: add DokonProApp and MainActivity with Koin init"
```

---

## Task 7: Android Design System — Theme, Colors, Typography

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/theme/Color.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/theme/Type.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color palette**

Create `androidApp/src/main/java/com/dokonpro/android/ui/theme/Color.kt`:
```kotlin
package com.dokonpro.android.ui.theme

import androidx.compose.ui.graphics.Color

// Primary — Deep Teal (trust, retail)
val Primary = Color(0xFF00796B)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFB2DFDB)
val OnPrimaryContainer = Color(0xFF00251A)

// Secondary — Amber (action, attention)
val Secondary = Color(0xFFFFA000)
val OnSecondary = Color(0xFF000000)
val SecondaryContainer = Color(0xFFFFECB3)
val OnSecondaryContainer = Color(0xFF261900)

// Tertiary — Slate Blue (finance)
val Tertiary = Color(0xFF5C6BC0)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFC5CAE9)
val OnTertiaryContainer = Color(0xFF1A237E)

// Error
val Error = Color(0xFFD32F2F)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFCDD2)
val OnErrorContainer = Color(0xFF601410)

// Neutral
val Background = Color(0xFFFAFAFA)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFE0E0E0)
val OnSurfaceVariant = Color(0xFF49454F)
val Outline = Color(0xFFBDBDBD)
```

- [ ] **Step 2: Create Typography**

Create `androidApp/src/main/java/com/dokonpro/android/ui/theme/Type.kt`:
```kotlin
package com.dokonpro.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DokonProTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
```

- [ ] **Step 3: Create Theme**

Create `androidApp/src/main/java/com/dokonpro/android/ui/theme/Theme.kt`:
```kotlin
package com.dokonpro.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline
)

@Composable
fun DokonProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = DokonProTypography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

Run:
```bash
git add androidApp/src/main/java/com/dokonpro/android/ui/theme/
git commit -m "feat: add Material 3 design system with retail color palette"
```

---

## Task 8: Android String Resources (ru + tg)

**Files:**
- Create: `androidApp/src/main/res/values/strings.xml`
- Create: `androidApp/src/main/res/values-tg/strings.xml`

- [ ] **Step 1: Create Russian strings (default)**

Create `androidApp/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">ДоконПро</string>

    <!-- Common -->
    <string name="loading">Загрузка...</string>
    <string name="error_generic">Произошла ошибка</string>
    <string name="retry">Повторить</string>
    <string name="cancel">Отмена</string>
    <string name="save">Сохранить</string>
    <string name="delete">Удалить</string>
    <string name="search">Поиск</string>
    <string name="back">Назад</string>
    <string name="confirm">Подтвердить</string>
    <string name="offline">Нет подключения</string>
    <string name="syncing">Синхронизация...</string>

    <!-- Navigation -->
    <string name="nav_pos">Касса</string>
    <string name="nav_products">Товары</string>
    <string name="nav_sales">Продажи</string>
    <string name="nav_customers">Клиенты</string>
    <string name="nav_finance">Финансы</string>
    <string name="nav_settings">Настройки</string>
</resources>
```

- [ ] **Step 2: Create Tajik strings**

Create `androidApp/src/main/res/values-tg/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">ДуконПро</string>

    <!-- Common -->
    <string name="loading">Боргирӣ...</string>
    <string name="error_generic">Хатогӣ рух дод</string>
    <string name="retry">Такрор</string>
    <string name="cancel">Бекор</string>
    <string name="save">Нигоҳ доштан</string>
    <string name="delete">Нест кардан</string>
    <string name="search">Ҷустуҷӯ</string>
    <string name="back">Бозгашт</string>
    <string name="confirm">Тасдиқ</string>
    <string name="offline">Пайвастшавӣ нест</string>
    <string name="syncing">Ҳамоҳангсозӣ...</string>

    <!-- Navigation -->
    <string name="nav_pos">Касса</string>
    <string name="nav_products">Молҳо</string>
    <string name="nav_sales">Фурӯш</string>
    <string name="nav_customers">Мизоҷон</string>
    <string name="nav_finance">Молия</string>
    <string name="nav_settings">Танзимот</string>
</resources>
```

- [ ] **Step 3: Commit**

Run:
```bash
git add androidApp/src/main/res/
git commit -m "feat: add string resources for Russian and Tajik"
```

---

## Task 9: Android Navigation + MainScreen

**Files:**
- Create: `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`
- Create: `androidApp/src/main/java/com/dokonpro/android/ui/MainScreen.kt`

- [ ] **Step 1: Create AppNavigation**

Create `androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt`:
```kotlin
package com.dokonpro.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dokonpro.android.ui.MainScreen

object Routes {
    const val MAIN = "main"
    // Auth, POS, Products, etc. will be added per vertical slice
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen()
        }
    }
}
```

- [ ] **Step 2: Create MainScreen placeholder**

Create `androidApp/src/main/java/com/dokonpro/android/ui/MainScreen.kt`:
```kotlin
package com.dokonpro.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dokonpro.android.R
import com.dokonpro.android.ui.theme.DokonProTheme
import com.dokonpro.shared.platformName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DokonPro — ${platformName()}",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    DokonProTheme {
        MainScreen()
    }
}
```

- [ ] **Step 3: Commit**

Run:
```bash
git add androidApp/src/main/java/com/dokonpro/android/navigation/ androidApp/src/main/java/com/dokonpro/android/ui/MainScreen.kt
git commit -m "feat: add Compose navigation shell and MainScreen"
```

---

## Task 10: Verify Gradle Build

- [ ] **Step 1: Run shared module build**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native && ./gradlew :shared:build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Run shared tests**

Run:
```bash
./gradlew :shared:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 3: Run Android app build**

Run:
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit if any fixes were needed**

If build required fixes, commit them:
```bash
git add -A
git commit -m "fix: resolve build issues in foundation setup"
```

---

## Task 11: NestJS Backend — Project Init

**Files:**
- Create: `api/package.json`
- Create: `api/tsconfig.json`
- Create: `api/tsconfig.build.json`
- Create: `api/nest-cli.json`
- Create: `api/src/main.ts`
- Create: `api/src/app.module.ts`

- [ ] **Step 1: Initialize NestJS project**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
mkdir -p api
cd api
npm init -y
npm install @nestjs/core @nestjs/common @nestjs/platform-express reflect-metadata rxjs
npm install -D @nestjs/cli @nestjs/schematics typescript @types/node @types/express ts-node tsconfig-paths
```

- [ ] **Step 2: Create tsconfig.json**

Create `api/tsconfig.json`:
```json
{
  "compilerOptions": {
    "module": "commonjs",
    "declaration": true,
    "removeComments": true,
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true,
    "allowSyntheticDefaultImports": true,
    "target": "ES2021",
    "sourceMap": true,
    "outDir": "./dist",
    "baseUrl": "./",
    "incremental": true,
    "skipLibCheck": true,
    "strictNullChecks": true,
    "noImplicitAny": true,
    "strictBindCallApply": true,
    "forceConsistentCasingInFileNames": true,
    "noFallthroughCasesInSwitch": true
  }
}
```

Create `api/tsconfig.build.json`:
```json
{
  "extends": "./tsconfig.json",
  "exclude": ["node_modules", "test", "dist", "**/*spec.ts"]
}
```

- [ ] **Step 3: Create nest-cli.json**

Create `api/nest-cli.json`:
```json
{
  "$schema": "https://json.schemastore.org/nest-cli",
  "collection": "@nestjs/schematics",
  "sourceRoot": "src",
  "compilerOptions": {
    "deleteOutDir": true
  }
}
```

- [ ] **Step 4: Create app.module.ts**

Create `api/src/app.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { HealthModule } from './modules/health/health.module';

@Module({
  imports: [HealthModule],
})
export class AppModule {}
```

- [ ] **Step 5: Create main.ts**

Create `api/src/main.ts`:
```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors();
  await app.listen(3000);
  console.log(`Server running on http://localhost:3000`);
}
bootstrap();
```

- [ ] **Step 6: Add scripts to package.json**

Update `api/package.json` scripts section:
```json
{
  "scripts": {
    "build": "nest build",
    "start": "nest start",
    "start:dev": "nest start --watch",
    "start:prod": "node dist/main"
  }
}
```

- [ ] **Step 7: Commit**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/package.json api/package-lock.json api/tsconfig.json api/tsconfig.build.json api/nest-cli.json api/src/
git commit -m "feat: initialize NestJS backend project"
```

---

## Task 12: Docker Compose — Postgres + Redis

**Files:**
- Create: `api/docker-compose.yml`
- Create: `api/.env.example`
- Create: `api/.env`

- [ ] **Step 1: Create docker-compose.yml**

Create `api/docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: dokonpro-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: dokonpro
      POSTGRES_PASSWORD: dokonpro_dev
      POSTGRES_DB: dokonpro
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dokonpro"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: dokonpro-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
  redis_data:
```

- [ ] **Step 2: Create .env files**

Create `api/.env.example`:
```env
DATABASE_URL="postgresql://dokonpro:dokonpro_dev@localhost:5432/dokonpro?schema=public"
REDIS_URL="redis://localhost:6379"
JWT_SECRET="change-me-in-production"
JWT_REFRESH_SECRET="change-me-in-production-refresh"
OTP_DEV_MODE="true"
```

Create `api/.env` (same content for local dev):
```env
DATABASE_URL="postgresql://dokonpro:dokonpro_dev@localhost:5432/dokonpro?schema=public"
REDIS_URL="redis://localhost:6379"
JWT_SECRET="dev-jwt-secret-key-32chars-min!!"
JWT_REFRESH_SECRET="dev-refresh-secret-key-32chars!!"
OTP_DEV_MODE="true"
```

- [ ] **Step 3: Start Docker containers**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api && docker compose up -d
```
Expected: Both containers start and become healthy.

Verify:
```bash
docker compose ps
```
Expected: `dokonpro-postgres` and `dokonpro-redis` both `running (healthy)`

- [ ] **Step 4: Commit**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/docker-compose.yml api/.env.example
git commit -m "feat: add Docker Compose for PostgreSQL 16 and Redis 7"
```

Note: `api/.env` is in `.gitignore` — not committed.

---

## Task 13: Prisma Setup + Initial Schema

**Files:**
- Create: `api/prisma/schema.prisma`
- Create: `api/src/prisma/prisma.service.ts`
- Create: `api/src/prisma/prisma.module.ts`

- [ ] **Step 1: Install Prisma**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npm install @prisma/client
npm install -D prisma
npx prisma init
```

- [ ] **Step 2: Create Prisma schema**

Replace `api/prisma/schema.prisma`:
```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

enum Role {
  OWNER
  MANAGER
  CASHIER
}

model User {
  id        String     @id @default(uuid())
  phone     String     @unique
  name      String?
  createdAt DateTime   @default(now()) @map("created_at")
  updatedAt DateTime   @updatedAt @map("updated_at")
  stores    UserStore[]

  @@map("users")
}

model Store {
  id        String     @id @default(uuid())
  name      String
  address   String?
  phone     String?
  currency  String     @default("TJS")
  logoUrl   String?    @map("logo_url")
  createdAt DateTime   @default(now()) @map("created_at")
  updatedAt DateTime   @updatedAt @map("updated_at")
  users     UserStore[]

  @@map("stores")
}

model UserStore {
  id      String @id @default(uuid())
  userId  String @map("user_id")
  storeId String @map("store_id")
  role    Role   @default(CASHIER)
  user    User   @relation(fields: [userId], references: [id])
  store   Store  @relation(fields: [storeId], references: [id])

  @@unique([userId, storeId])
  @@map("user_stores")
}
```

- [ ] **Step 3: Run initial migration**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npx prisma migrate dev --name init
```
Expected: Migration created and applied successfully.

- [ ] **Step 4: Create PrismaService**

Create `api/src/prisma/prisma.service.ts`:
```typescript
import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';

@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit, OnModuleDestroy {
  async onModuleInit() {
    await this.$connect();
  }

  async onModuleDestroy() {
    await this.$disconnect();
  }
}
```

Create `api/src/prisma/prisma.module.ts`:
```typescript
import { Global, Module } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Global()
@Module({
  providers: [PrismaService],
  exports: [PrismaService],
})
export class PrismaModule {}
```

- [ ] **Step 5: Register PrismaModule in AppModule**

Update `api/src/app.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { PrismaModule } from './prisma/prisma.module';
import { HealthModule } from './modules/health/health.module';

@Module({
  imports: [PrismaModule, HealthModule],
})
export class AppModule {}
```

- [ ] **Step 6: Commit**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/prisma/ api/src/prisma/ api/src/app.module.ts
git commit -m "feat: add Prisma with initial User/Store/UserStore schema"
```

---

## Task 14: Health Check Endpoint

**Files:**
- Create: `api/src/modules/health/health.module.ts`
- Create: `api/src/modules/health/health.controller.ts`

- [ ] **Step 1: Create health controller**

Create `api/src/modules/health/health.controller.ts`:
```typescript
import { Controller, Get } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Controller('health')
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  @Get()
  async check() {
    try {
      await this.prisma.$queryRaw`SELECT 1`;
      return {
        status: 'ok',
        timestamp: new Date().toISOString(),
        database: 'connected',
      };
    } catch {
      return {
        status: 'error',
        timestamp: new Date().toISOString(),
        database: 'disconnected',
      };
    }
  }
}
```

- [ ] **Step 2: Create health module**

Create `api/src/modules/health/health.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { HealthController } from './health.controller';

@Module({
  controllers: [HealthController],
})
export class HealthModule {}
```

- [ ] **Step 3: Test the endpoint**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npm run start:dev &
sleep 3
curl http://localhost:3000/health
kill %1
```
Expected: `{"status":"ok","timestamp":"...","database":"connected"}`

- [ ] **Step 4: Commit**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git add api/src/modules/health/
git commit -m "feat: add health check endpoint with database status"
```

---

## Task 15: Final Verification + Tag

- [ ] **Step 1: Verify full Gradle build**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
./gradlew build
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify backend starts**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native/api
npm run build
```
Expected: No errors

- [ ] **Step 3: Verify project structure matches spec**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
find . -type f -name "*.kt" -o -name "*.ts" -o -name "*.kts" | grep -v node_modules | grep -v .gradle | sort
```

Expected output should include:
```
./androidApp/build.gradle.kts
./androidApp/src/main/java/com/dokonpro/android/DokonProApp.kt
./androidApp/src/main/java/com/dokonpro/android/MainActivity.kt
./androidApp/src/main/java/com/dokonpro/android/navigation/AppNavigation.kt
./androidApp/src/main/java/com/dokonpro/android/ui/MainScreen.kt
./androidApp/src/main/java/com/dokonpro/android/ui/theme/Color.kt
./androidApp/src/main/java/com/dokonpro/android/ui/theme/Theme.kt
./androidApp/src/main/java/com/dokonpro/android/ui/theme/Type.kt
./build.gradle.kts
./settings.gradle.kts
./shared/build.gradle.kts
./shared/src/androidMain/kotlin/com/dokonpro/shared/Platform.android.kt
./shared/src/commonMain/kotlin/com/dokonpro/shared/Platform.kt
./shared/src/commonMain/kotlin/com/dokonpro/shared/di/SharedModule.kt
./shared/src/commonTest/kotlin/com/dokonpro/shared/PlatformTest.kt
./shared/src/iosMain/kotlin/com/dokonpro/shared/Platform.ios.kt
./api/src/app.module.ts
./api/src/main.ts
./api/src/prisma/prisma.module.ts
./api/src/prisma/prisma.service.ts
./api/src/modules/health/health.controller.ts
./api/src/modules/health/health.module.ts
```

- [ ] **Step 4: Tag the foundation milestone**

Run:
```bash
cd /Users/latifrjdev/Desktop/DokonPro-Native
git tag v0.1.0-foundation
```

- [ ] **Step 5: Summary**

Foundation is complete. The following is now in place:
- Git repo initialized
- Gradle multi-module build (`:shared` + `:androidApp`)
- KMP shared module with Clean Architecture packages, SQLDelight, Ktor, Koin
- Android Compose app with Material 3 theme, navigation, Koin DI
- Russian + Tajik string resources
- NestJS backend with Prisma, PostgreSQL, Redis via Docker Compose
- Health check endpoint
- Ready for Phase 2: Auth vertical slice
