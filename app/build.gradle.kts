import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

// Resolve the optional Gemini API key. The app is fully offline-first; this key
// only enables optional cloud answers in Dhanom Chat. Resolution order:
//   1. GEMINI_API_KEY environment variable
//   2. -PGEMINI_API_KEY Gradle property
//   3. .env file (gitignored)
//   4. .env.example placeholder
// The placeholder is detected at runtime and the app falls back to offline NLU.
val envProps = Properties()
listOf(".env", ".env.example").forEach { name ->
    val f = rootProject.file(name)
    if (f.exists()) f.inputStream().use { envProps.load(it) }
}
val geminiApiKey: String = System.getenv("GEMINI_API_KEY")
    ?: (project.findProperty("GEMINI_API_KEY") as? String)?.takeIf { it.isNotBlank() }
    ?: envProps.getProperty("GEMINI_API_KEY")
    ?: "MY_GEMINI_API_KEY"

android {
  namespace = "com.example"
  compileSdk { version = release(36) }

  // Use AGP's well-known Android debug certificate (every phone accepts it)
  // and FORCE v1 JAR signing. AGP skips v1 when minSdk>=24; Xiaomi/Vivo/Oppo/
  // Realme/Samsung then show "App not installed" / "blocked for security".
  // The previous OpenSSL PKCS12 key produced a signature some OEMs reject.
  signingConfigs {
    getByName("debug") {
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
  }

  defaultConfig {
    // New applicationId so this installs even if an older Dhan-OM (different
    // signature) is stuck on the phone. It will appear as a fresh app.
    applicationId = "com.dhanom.finance"
    minSdk = 24
    // 34 = widest sideload compatibility. 16 KB phones still install a
    // Kotlin-only APK (no native .so). Targeting 35+ + LiteRT 4 KB .so is
    // what made every previous build show "App not installed".
    targetSdk = 34
    versionCode = 8
    versionName = "1.8"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField(
      "String",
      "GEMINI_API_KEY",
      "\"${geminiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
    // Match the 64 MB APK that DID install: default debug, v1+v2+v3, all ABIs.
    debug {
      isMinifyEnabled = false
      isCrunchPngs = false
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      // Uncompressed + 16 KB ZIP-aligned (AGP 8.5.1+). Do NOT enable
      // useLegacyPackaging / abiFilters — those produced the 22 MB APK that
      // failed to install.
      useLegacyPackaging = false
    }
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  // LiteRT-LM is intentionally NOT packaged: its native .so files block
  // install on Android 15+ 16 KB devices. Cloud Brain + local math remain.
  implementation(libs.nanohttpd)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}


