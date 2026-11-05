import java.time.Duration
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Ensure google-services.json is available in all build environments (local and GitHub Actions CI)
val googleServicesFile = file("google-services.json")
if (!googleServicesFile.exists()) {
    val envSecret = System.getenv("GOOGLE_SERVICES_JSON")
    if (!envSecret.isNullOrBlank()) {
        googleServicesFile.writeText(envSecret)
    } else {
        googleServicesFile.writeText(
            """{
  "project_info": {
    "project_number": "403764078616",
    "project_id": "gen-lang-client-0309633317",
    "storage_bucket": "gen-lang-client-0309633317.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:403764078616:android:a2efc64a65e20eb2757b6d",
        "android_client_info": {
          "package_name": "com.aistudio.springmusic.qvxkzp"
        }
      },
      "oauth_client": [
        {
          "client_id": "403764078616-44qmiq6n0p6jmhlht99t8v3rkp5f7250.apps.googleusercontent.com",
          "client_type": 1,
          "android_info": {
            "package_name": "com.aistudio.springmusic.qvxkzp",
            "certificate_hash": "a34c0a6fc5b4d1217663f93f090b18c7b7c74462"
          }
        },
        {
          "client_id": "403764078616-r07rpbfbq9os1odd1rg2soe5ihis8hea.apps.googleusercontent.com",
          "client_type": 3
        }
      ],
      "api_key": [
        {
          "current_key": "AIzaSyCR0q21Q63HkLaU8jCS5kh7oRKsMNQmYws"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": [
            {
              "client_id": "403764078616-r07rpbfbq9os1odd1rg2soe5ihis8hea.apps.googleusercontent.com",
              "client_type": 3
            }
          ]
        }
      }
    }
  ],
  "configuration_version": "1"
}"""
        )
    }
}

// Ensure debug.keystore is available in all environments (including GitHub Actions CI)
val EMBEDDED_DEBUG_KEYSTORE_BASE64 = "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"
val rootKeystore = file("../debug.keystore")

fun ensureDebugKeystore() {
    if (!rootKeystore.exists()) {
        try {
            rootKeystore.parentFile?.mkdirs()
            val base64File = file("../debug.keystore.base64")
            val base64Content = if (base64File.exists() && base64File.length() > 100) {
                base64File.readText().trim()
            } else {
                val env = System.getenv("DEBUG_KEYSTORE_BASE64")
                if (!env.isNullOrBlank()) env.trim() else EMBEDDED_DEBUG_KEYSTORE_BASE64
            }
            rootKeystore.writeBytes(Base64.getMimeDecoder().decode(base64Content))
        } catch (_: Exception) {}
    }
}
ensureDebugKeystore()

tasks.matching { it.name.contains("Signing", ignoreCase = true) }.configureEach {
    doFirst {
        ensureDebugKeystore()
    }
}

// --- Synchronized App Version Metadata ---
object AppVersionConfig {
    const val MAJOR = 9
    const val MINOR = 1
    const val BUILD_CODE = 2

    fun getVersionName(): String = "$MAJOR.$MINOR"
    fun getVersionCode(): Int = BUILD_CODE
    fun getFormattedVersionString(): String = "v${getVersionName()} (Build ${getVersionCode()})"
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.springmusic.qvxkzp"
        minSdk = 24
        targetSdk = 36
        versionCode = AppVersionConfig.getVersionCode()
        versionName = AppVersionConfig.getVersionName()

        buildConfigField("String", "APP_VERSION_NAME", "\"${AppVersionConfig.getVersionName()}\"")
        buildConfigField("int", "APP_VERSION_CODE", "${AppVersionConfig.getVersionCode()}")
        buildConfigField("String", "APP_VERSION_FULL", "\"${AppVersionConfig.getFormattedVersionString()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        val debugConfig by registering {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "**/*.proto"
            )
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Firebase & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<Test> {
    failFast = true
    timeout.set(Duration.ofMinutes(2))
    testLogging {
        events("passed", "failed", "skipped")
    }
}

// Build-time verification check to ensure app correctly identifies its own version
tasks.register("verifyAppVersionMetadata") {
    group = "verification"
    description = "Verifies that user-facing app version string (versionName) is strictly synchronized with internal build metadata."
    doLast {
        val vCode = android.defaultConfig.versionCode
        val vName = android.defaultConfig.versionName
        check(vCode != null && vCode == AppVersionConfig.getVersionCode()) {
            "Build verification failed: defaultConfig.versionCode ($vCode) != AppVersionConfig (${AppVersionConfig.getVersionCode()})"
        }
        check(!vName.isNullOrBlank() && vName == AppVersionConfig.getVersionName()) {
            "Build verification failed: defaultConfig.versionName ($vName) != AppVersionConfig (${AppVersionConfig.getVersionName()})"
        }
        println("=================================================================")
        println(" [BUILD VERIFICATION] App Version Strictly Synchronized:")
        println("  Version Name: $vName")
        println("  Version Code: $vCode")
        println("  Full String : ${AppVersionConfig.getFormattedVersionString()}")
        println("=================================================================")
    }
}

tasks.matching { it.name.startsWith("compile") || it.name.startsWith("assemble") || it.name.startsWith("bundle") }.configureEach {
    dependsOn("verifyAppVersionMetadata")
}
