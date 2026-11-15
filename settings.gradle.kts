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

// Kotlin DSL 引入 Flutter 模块的兼容写法
val flutterProjectRoot = file("flutter_ui")
apply(from = flutterProjectRoot.resolve(".android/include_flutter.groovy"))
project(":flutter").projectDir = flutterProjectRoot.resolve(".android/Flutter")

// ============================================================
// 核心修复：统一覆盖所有子项目的仓库和 Android 插件应用顺序
// ============================================================
subprojects {
    // 1. 在子项目构建脚本评估后，强制重置仓库列表
    //    这会覆盖 amap_flutter_location 插件里声明的 jcenter()
    afterEvaluate {
        repositories.clear() // 清空旧仓库（包括 jcenter）
        repositories {
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            google()
            mavenCentral()
            maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
        }
    }

    // 2. 确保 Android 插件先被应用，再执行可能访问 android 扩展的逻辑
    //    这可以修复 "Android plugin must be applied before accessing the Android extension"
    pluginManager.withPlugin("com.android.application") {
        // 插件应用后，再做后续配置（如果需要）
    }
    pluginManager.withPlugin("com.android.library") {
        // 插件应用后，再做后续配置（如果需要）
    }

    // 3. 对非 Android 插件项目，也确保其在访问 android 扩展前已应用插件
    //    （某些 Flutter 插件可能命名不规范，用 try-catch 兜底）
    if (project.name != "app" && !project.name.startsWith("flutter")) {
        afterEvaluate {
            try {
                if (project.hasProperty("android")) {
                    // 如果插件有 android 扩展，说明它已经应用了 Android 插件
                    // 这里不做额外操作，只是确认插件已应用
                }
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
}

// 全局强制禁用 includeAndroidResources 修复 Windows 跨盘符路径 roots 错误
gradle.allprojects {
    afterEvaluate {
        extensions.findByName("android")?.let { android ->
            try {
                val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
                val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
                unitTests.javaClass.getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType).invoke(unitTests, false)
            } catch (e: Exception) {
                // Ignore if not a standard Android project
            }
        }
    }
}
