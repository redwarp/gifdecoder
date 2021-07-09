plugins {
    id("com.android.application") version Versions.AGP apply false
    id("com.android.library") version Versions.AGP apply false
    id("androidx.benchmark") version Versions.BENCHMARK apply false
    kotlin("android") version Versions.KOTLIN apply false
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_GRADLE
    id("com.github.ben-manes.versions") version Versions.VERSIONS apply false
    id("com.diffplug.spotless") version Versions.SPOTLESS apply false
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("com.github.ben-manes.versions")
        plugin("com.diffplug.spotless")
    }

    ktlint {
        version.set(Versions.KTLINT)
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target(
                project.fileTree(project.projectDir) {
                    include("**/app/redwarp/gif/**/*.kt")
                }
            )

            licenseHeaderFile("${project.rootDir}/license_header.txt")
        }
    }
}

tasks.register("clean", Delete::class.java) {
    dependsOn(gradle.includedBuild("decoder").task(":clean"))
    delete(rootProject.buildDir)
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.AGP}")
    }
}

tasks.register("testLibraries") {
    description = "Run tests on both libraries"

    dependsOn(gradle.includedBuild("decoder").task(":test"))
    dependsOn(":android-drawable:test")
}

tasks.register("uploadLibraries") {
    description = "Upload both decoder and gif-drawable artifact to maven central"

    dependsOn(gradle.includedBuild("decoder").task(":publishReleasePublicationToNexusRepository"))
    dependsOn(":android-drawable:publishReleasePublicationToNexusRepository")
}

tasks.register("addLicenseHeader") {
    description = "Make sure the license header is applied to every files"

    dependsOn(gradle.includedBuild("decoder").task(":spotlessApply"))
    val spotlessTasks = subprojects.map { project ->
        project.tasks.filter { it.name == "spotlessApply" }
    }.flatten().toTypedArray()
    dependsOn(*spotlessTasks)
}
