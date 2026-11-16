import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
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

    // 修复第三方插件的 namespace 缺失 / compileSdk 过低
    project.afterEvaluate {
        if (project.hasProperty("android")) {
            val android = project.extensions.getByName("android")

            // 1. namespace 兜底
            try {
                val getNamespace = android.javaClass.getMethod("getNamespace")
                val namespace = getNamespace.invoke(android)
                if (namespace == null) {
                    val targetNamespace = when {
                        project.name == "amap_flutter_location" -> "com.amap.flutter.location"
                        project.name == "flutter_plugin_android_lifecycle" -> "io.flutter.plugins.android_lifecycle"
                        project.name.contains("_") -> "com.footprint." + project.name.replace("_", ".")
                        else -> "com.footprint." + project.name
                    }
                    val setNamespace = android.javaClass.getMethod("setNamespace", String::class.java)
                    setNamespace.invoke(android, targetNamespace)
                    println("Manually set namespace for ${project.name} to $targetNamespace")
                }
            } catch (e: Exception) {}

            // 2. compileSdk 兜底（至少 35）
            try {
                var currentSdkVersion: Int? = null
                try {
                    val getCompileSdk = android.javaClass.getMethod("getCompileSdk")
                    currentSdkVersion = getCompileSdk.invoke(android) as? Int
                } catch (e: Exception) {
                    try {
                        val getCompileSdkVersion = android.javaClass.getMethod("getCompileSdkVersion")
                        val sdkStr = getCompileSdkVersion.invoke(android) as? String
                        if (sdkStr != null && sdkStr.startsWith("android-")) {
                            currentSdkVersion = sdkStr.substringAfter("android-").toIntOrNull()
                        }
                    } catch (e2: Exception) {}
                }

                if (currentSdkVersion != null && currentSdkVersion < 35) {
                    try {
                        val setCompileSdk = android.javaClass.getMethod("setCompileSdk", java.lang.Integer.TYPE)
                        setCompileSdk.invoke(android, 35)
                        println("Forced compileSdk to 35 for ${project.name} (was $currentSdkVersion)")
                    } catch (e: Exception) {
                        val setCompileSdkVersion = android.javaClass.getMethod("setCompileSdkVersion", String::class.java)
                        setCompileSdkVersion.invoke(android, "android-35")
                        println("Forced compileSdkVersion to android-35 for ${project.name} (was $currentSdkVersion)")
                    }
                }
            } catch (e: Exception) {}
        }
    }

    // 禁用 includeAndroidResources（原来在 settings 里想做但没做成的事）
    afterEvaluate {
        (extensions.findByName("android") as? BaseExtension)?.apply {
            testOptions.unitTests.isIncludeAndroidResources = false
        }
    }
}
