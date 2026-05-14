import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.example.celestis.database")
            generateAsync.set(false)
            deriveSchemaFromMigrations.set(false)
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android)
            implementation(libs.coil.android)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.core)
            implementation(libs.androidx.palette)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.glance)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.crashlytics)
            implementation(libs.androidx.glance.material3)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
            implementation(libs.coroutines.extensions)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.navigation.compose)
            implementation(libs.components.resources.v1101)
            implementation(libs.material.icons.extended)
            implementation(libs.slf4j.nop)
            implementation(libs.compose.material3.windowSizeClass)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.haze)
       }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
buildkonfig {
    packageName = "com.example.celestis"

    // Default values (used if local.properties is missing)
    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", localProperties.getProperty("BASE_URL") ?: "https://fallback.com")
    }

    targetConfigs {
        create("main") {
            buildConfigField(STRING, "BASE_URL", localProperties.getProperty("BASE_URL") ?: "")
        }
    }
}

android {
    namespace = "com.example.celestis"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    val releaseStoreFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
        ?: System.getenv("CELESTIS_RELEASE_STORE_FILE")
    val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
        ?: System.getenv("CELESTIS_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
        ?: System.getenv("CELESTIS_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        ?: System.getenv("CELESTIS_RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStoreFilePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.example.celestis"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
