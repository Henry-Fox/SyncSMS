plugins {
    // 顶层仅声明，版本由 gradle/libs.versions.toml 管理（简化后续升级）
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

