// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}")
        classpath("gradle.plugin.org.mozilla.rust-android-gradle:plugin:0.8.3")
        classpath("androidx.benchmark:benchmark-gradle-plugin:1.0.0")
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1" apply false
    id("com.github.ben-manes.versions") version "0.36.0" apply false
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("com.github.ben-manes.versions")
    }
}

tasks.register("clean", Delete::class.java){
    delete(rootProject.buildDir)
}
