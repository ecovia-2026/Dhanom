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
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.dhanom.finance"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "1.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Ship ALL native ABIs so the APK installs on any phone, tablet, or
    // emulator (arm64, armv7, x86, x86_64). Removes "no matching ABI" failures.
    ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    }

    buildConfigField(
      "String",
      "GEMINI_API_KEY",
      "\"${geminiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    )
  }

  signingConfigs {
    create("stable") {
      storeFile = rootProject.file("dhanom.p12")
      storePassword = "dhanom123"
      keyAlias = "dhanom"
      keyPassword = "dhanom123"
      storeType = "PKCS12"
      // Sign with ALL schemes so EVERY Android installer accepts it:
      // v1 (JAR) for older/3rd-party installers, v2 for Android 7+, v3 for 9+.
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("stable")
    }
    debug {
      // Release-quality sideload build: stable committed key + NOT debuggable,
      // so Play Protect and strict OEM installers don't silently reject it.
      signingConfig = signingConfigs.getByName("stable")
      isDebuggable = false
      isMinifyEnabled = false
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
  // On-device Gemma 4 E4B brain (LiteRT-LM) + local HTTP bridge for other apps/phones
  implementation(libs.litertlm.android)
  implementation(libs.nanohttpd)
  // OCR (image + PDF) and PDF tools (password removal, merge, split)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.pdfbox.android)
  // Scheduled monthly "autopilot" agent (analyzes data & savings every month)
  implementation(libs.androidx.work.runtime.ktx)
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



// APK self-inspection (CI diagnostic): after assembleDebug, dump the built
// APK's signature, badging, ABIs and zip contents to the build log so the
// package can be verified end-to-end. Never fails the build.
tasks.register("dumpApkInfo") {
    dependsOn("assembleDebug")
    doLast {
        val apk = project.file("build/outputs/apk/debug/app-debug.apk")
        fun run(cmd: String) {
            try {
                val p = ProcessBuilder("bash", "-c", cmd).redirectErrorStream(true).start()
                p.inputStream.bufferedReader().forEachLine { println("APKINFO: $it") }
                p.waitFor()
            } catch (t: Throwable) {
                println("APKINFO: cmd failed :: ${t.message}")
            }
        }
        println("APKINFO: exists=${apk.exists()} size=${if (apk.exists()) apk.length() else -1}")
        val sdk = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: "/usr/local/lib/android/sdk"
        run("ls -d $sdk/build-tools/* | tail -3")
        run("BT=\$(ls -d $sdk/build-tools/* | tail -1); \$BT/apksigner verify --verbose --print-certs ${apk.absolutePath}")
        run("BT=\$(ls -d $sdk/build-tools/* | tail -1); \$BT/aapt dump badging ${apk.absolutePath}")
        run("BT=\$(ls -d $sdk/build-tools/* | tail -1); \$BT/aapt dump badging ${apk.absolutePath} | grep -iE 'package:|native-code|debuggable'")
        run("unzip -l ${apk.absolutePath} | grep -E 'lib/|classes.dex|AndroidManifest'")
        run("BT=\$(ls -d $sdk/build-tools/* | tail -1); \$BT/aapt dump xmltree ${apk.absolutePath} AndroidManifest.xml | grep -iE 'testOnly|debuggable|installLocation|uses-sdk' | head -20")
    }
}
// Wire the dump AFTER AGP registers the assemble tasks (AGP 9 registers them lazily).
afterEvaluate {
    tasks.named("assembleDebug") { finalizedBy("dumpApkInfo") }
}
