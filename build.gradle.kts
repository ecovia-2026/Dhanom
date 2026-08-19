// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  // Kotlin Android and Kapt are provided via the version catalog aliases; avoid duplicate plugin registrations
  // (Removed explicit id(...) entries to prevent 'kotlin' extension registration conflicts)
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
