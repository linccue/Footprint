import com.android.build.api.dsl.CommonExtension

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    // 给缺少 flutter 配置的插件注入默认值
    if (project.name == "flutter_plugin_android_lifecycle" || project.name.contains("city_picker")) {
        project.extensions.extraProperties.set("flutter", mapOf(
            "compileSdkVersion" to 35,
            "minSdkVersion" to 21,
            "targetSdkVersion" to 35
        ))
    }

    project.afterEvaluate {
        val androidExt = extensions.findByName("android") as? CommonExtension
        if (androidExt != null) {
            // ========== 1. namespace 兜底 ==========
            if (androidExt.namespace == null) {
                val targetNamespace = when {
                    project.name == "amap_flutter_location" -> "com.amap.flutter.location"
                    project.name == "flutter_plugin_android_lifecycle" -> "io.flutter.plugins.android_lifecycle"
                    project.name.contains("_") -> "com.footprint." + project.name.replace("_", ".")
                    else -> "com.footprint." + project.name
                }
                androidExt.namespace = targetNamespace
                println("Manually set namespace for ${project.name} to $targetNamespace")
            }

            // ========== 2. compileSdk 兜底（修复可空Int?） ==========
            val currentCompileSdk = androidExt.compileSdk
            if (currentCompileSdk == null || currentCompileSdk < 35) {
                androidExt.compileSdk = 35
                println("Forced compileSdk to 35 for ${project.name} (was $currentCompileSdk)")
            }

            // ========== 3. 关闭 unitTests includeAndroidResources ==========
            androidExt.testOptions.unitTests.isIncludeAndroidResources = false
        }
    }
}
