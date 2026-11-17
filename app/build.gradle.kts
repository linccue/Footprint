import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
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
            abiFilters += "arm64-v8a"
        }
        resourceConfigurations += listOf("zh", "zh-rCN", "en")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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
        }
        debug {
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
            force("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
            force("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
            force("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
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
    val composeBom = platform("androidx.compose:compose-bom:2025.03.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

// 高德地图 3D 地图SDK（必须，你用到 AMap / MapView / LatLng）
    implementation 'com.amap.api:3dmap:latest.release'
    // 高德定位（amap_flutter_location 已经依赖，但加上保险）
    implementation 'com.amap.api:location:latest.release'
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("com.amap.api:search:9.4.0")
    implementation("com.amap.api:location:6.4.3")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    implementation("dev.chrisbanes.haze:haze-jetpack-compose:0.5.2")

    debugImplementation(project(path = ":flutter", configuration = "debugRuntimeElements"))
    releaseImplementation(project(path = ":flutter", configuration = "releaseRuntimeElements"))
}

val buildRustTask = tasks.register<Exec>("buildRust") {
    workingDir = file("rust")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "--output-dir", "../src/main/jniLibs",
        "build", "--release"
    )
}

tasks.named("preBuild") {
    dependsOn(buildRustTask)
}
