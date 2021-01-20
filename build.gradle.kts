plugins {
    id("com.android.application") version Versions.AGP apply false
    id("com.android.library") version Versions.AGP apply false
    id("androidx.benchmark") version Versions.BENCHMARK apply false
    kotlin("android") version Versions.KOTLIN apply false
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_GRADLE
    id("com.github.ben-manes.versions") version Versions.VERSIONS apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("com.github.ben-manes.versions")
    }

    ktlint {
        version.set(Versions.KTLINT)
    }
}

tasks.register("clean", Delete::class.java) {
    dependsOn(gradle.includedBuild("decoder").task(":clean"))
    delete(rootProject.buildDir)
}

tasks.register("testLibraries") {
    description = "Upload both decoder and gif-drawable artifact to bintray"

    dependsOn(gradle.includedBuild("decoder").task(":test"))
    dependsOn(":android-drawable:test")
}

tasks.register("uploadLibraries") {
    description = "Upload both decoder and gif-drawable artifact to bintray"

    dependsOn(gradle.includedBuild("decoder").task(":bintrayUpload"))
    dependsOn(":android-drawable:bintrayUpload")
}
