rootProject.name = "gifdecoder"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}

includeBuild("decoder")
include(":android-drawable", ":android-drawable-native", ":android", ":benchmark")
