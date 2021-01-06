import com.jfrog.bintray.gradle.BintrayExtension

// See https://medium.com/@saschpe/android-library-publication-in-2020-93e8c0e106c8
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka") version Versions.DOKKA
    id("maven-publish")
    id("com.jfrog.bintray") version Versions.BINTRAY
}

base {
    group = Publication.GROUP
    archivesBaseName = "android-drawable"
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
        versionCode(Publication.VERSION_CODE)
        versionName(Publication.VERSION_NAME)

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
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api("net.redwarp.gif:decoder:0.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2")

    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

task("sourceJar", Jar::class) {
    from(android.sourceSets.getByName("main").java.srcDirs)
    archiveClassifier.set("sources")
}

task("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from("$buildDir/dokka/javadoc")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = Publication.GROUP
            artifactId = "android-drawable"
            version = Publication.VERSION_NAME

            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))

            }
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourceJar"))

            pom {
                name.set("Android Gif Drawable")
                url.set(Publication.Pom.URL)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                withXml {
                    fun groovy.util.Node.addDependency(dependency: Dependency, scope: String) {
                        appendNode("dependency").apply {
                            appendNode("groupId", dependency.group)
                            appendNode("artifactId", dependency.name)
                            appendNode("version", dependency.version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").let { dependencies ->
                        // List all "api" dependencies as "compile" dependencies
                        configurations.api.get().allDependencies.forEach {
                            dependencies.addDependency(it, "compile")
                        }
                        // List all "implementation" dependencies as "runtime" dependencies
                        configurations.implementation.get().allDependencies.forEach {
                            dependencies.addDependency(it, "runtime")
                        }
                    }
                }
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    setPublications("release")
    publish = true

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "gif-android-drawable"
        websiteUrl = Publication.Pom.URL
        vcsUrl = Publication.Pom.VCS_URL
        issueTrackerUrl = Publication.Pom.ISSUE_TRACKER_URL
        setLicenses("Apache-2.0")
        setLabels("kotlin", "gif", "android", "drawable")
        userOrg = "redwarp"
        publicDownloadNumbers = true
        override = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = Publication.VERSION_NAME
            vcsTag = "v${Publication.VERSION_NAME}"
        })
    })
}