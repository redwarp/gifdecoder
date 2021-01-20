rootProject.name = "gifdecoder"

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "androidx.benchmark") {
                useModule("androidx.benchmark:benchmark-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}

includeBuild("decoder")
include(":android-drawable", ":android-drawable-native", ":android", ":benchmark")
