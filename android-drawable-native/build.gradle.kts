// See https://medium.com/@saschpe/android-library-publication-in-2020-93e8c0e106c8
plugins {
    id("com.android.library")
    kotlin("android")
    // id("org.mozilla.rust-android-gradle.rust-android")
}

base {
    group = Publication.GROUP
    archivesBaseName = "android-drawable-native"
    version = Publication.VERSION_NAME
}

repositories {
    google()
    jcenter()
    mavenCentral()
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode(1)
        versionName("1.0")

        testInstrumentationRunner("androidx.test.runner.AndroidJUnitRunner")
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    ndkVersion = "22.0.7026061"
}

// cargo {
//     module = "../giflzwdecoder"
//     libname = "giflzwdecoder"
//     targets = listOf("arm", "x86", "arm64", "x86_64")
//     profile = "release"
// }

dependencies {
    api("net.redwarp.gif:decoder:0.1.0")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")
    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

tasks.register("cargoClean", Exec::class.java) {
    workingDir("$rootDir/giflzwdecoder")
    commandLine("cargo", "clean")
    group = "rust"
}

tasks.whenTaskAdded {
    if (name == "clean") {
        dependsOn("cargoClean")
    }
}
