import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}

fun credential(name: String): String? =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val appVersionCode = 1
val appVersionName = "0.1.0"

android {
    namespace = "com.pixelgram.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pixelgram.android"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("int", "API_ID", (credential("API_ID")?.toIntOrNull() ?: 0).toString())
        buildConfigField("String", "API_HASH", "\"${credential("API_HASH").orEmpty()}\"")
        ndk {
            abiFilters += (providers.gradleProperty("targetAbi").orNull?.split(",")
                ?: listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    signingConfigs {
        create("release") {
            val store = credential("RELEASE_STORE_FILE")
            if (store != null) {
                storeFile = rootProject.file(store)
                storePassword = credential("RELEASE_STORE_PASSWORD")
                keyAlias = credential("RELEASE_KEY_ALIAS")
                keyPassword = credential("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (credential("RELEASE_STORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs.useLegacyPackaging = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("pixelgram-$appVersionName-${variant.buildType}.apk")
        }
    }
}

dependencies {
    implementation(project(":tdlib"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.qrcodegen)
    implementation(libs.libphonenumber)
    implementation(libs.phonecarrier)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
