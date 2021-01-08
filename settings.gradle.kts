rootProject.name = "gifdecoder"

dependencyResolutionManagement {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

includeBuild("decoder")
include(":android-drawable",":android-drawable-native", ":android", ":benchmark")
