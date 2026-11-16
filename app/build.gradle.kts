import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.footprint"
    compileSdk = 36
    layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("app_build_fresh"))

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.footprint"
        minSdk = 24
        targetSdk = 35
        versionCode = 58
        versionName = "3.8.2"

        manifestPlaceholders["AMAP_KEY"] = localProperties.getProperty("AMAP_KEY") ?: "YOUR_AMAP_API_KEY"

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
        resourceConfigurations += listOf("zh", "zh-rCN", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("release")

            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
        debug {
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/rust/**"
            excludes += "**/target/**"
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "**/libVkLayer_khronos_validation.so"
            excludes += "**/libflutter.so.debug"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.core:core-ktx:1.15.0")
            force("androidx.core:core:1.15.0")
            force("androidx.activity:activity:1.9.3")
            force("androidx.activity:activity-compose:1.9.3")
            force("androidx.activity:activity-ktx:1.9.3")
            force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.6")

            // 方案A：定位类由 3dmap 内联提供，不再强制锁定独立 location 版本
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ===== 高德地图：方案A，定位类由 3dmap 内联提供 =====
    // 3dmap:9.4.0 已内联定位核心类，作为定位类的唯一提供者
    implementation("com.amap.api:3dmap:9.4.0")
    // search:9.4.0 提供搜索功能，排除其传递的 location 依赖，避免重复类
    implementation("com.amap.api:search:9.4.0") {
        exclude(group = "com.amap.api", module = "location")
    }
    // 不再单独引入 com.amap.api:location

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Haze
    implementation("dev.chrisbanes.haze:haze-jetpack-compose:0.5.2")

    // Flutter Module
    "debugImplementation"(project(path = ":flutter", configuration = "debugRuntimeElements"))
    "releaseImplementation"(project(path = ":flutter", configuration = "releaseRuntimeElements"))
}

val buildRustTask = tasks.register<Exec>("buildRust") {
    workingDir = file("rust")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-o", "../src/main/jniLibs",
        "build", "--release"
    )
}

tasks.named("preBuild") {
    dependsOn(buildRustTask)
}
