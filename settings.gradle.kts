pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }
}

rootProject.name = "Footprint"
include(":app")

val flutterProjectRoot = file("flutter_ui")
apply(from = flutterProjectRoot.resolve(".android/include_flutter.groovy"))
project(":flutter").projectDir = flutterProjectRoot.resolve(".android/Flutter")

// ============================================================
// 核心修复：在子项目评估前提供干净仓库，覆盖 amap_flutter_location 的 jcenter()
// ============================================================
subprojects {
    // 1. 对所有子项目预先声明有效仓库（不在 afterEvaluate 内）
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }

    // 2. 对 amap_flutter_location 额外加一道 beforeEvaluate 保险
    if (name == "amap_flutter_location") {
        beforeEvaluate {
            repositories {
                google()
                mavenCentral()
            }
        }
    }

    // 3. 保留原有的插件应用时序保障（无副作用）
    pluginManager.withPlugin("com.android.application") { }
    pluginManager.withPlugin("com.android.library") { }
}

// 全局强制禁用 includeAndroidResources（保留）
gradle.allprojects {
    afterEvaluate {
        extensions.findByName("android")?.let { android ->
            try {
                val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
                val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
                unitTests.javaClass.getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType).invoke(unitTests, false)
            } catch (e: Exception) { }
        }
    }
}
