// See https://medium.com/@saschpe/android-library-publication-in-2020-93e8c0e106c8
plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka") version Versions.DOKKA
    id("maven-publish")
    id("signing")
}

repositories {
    google()
    mavenCentral()
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    api("app.redwarp.gif:decoder:${Publication.VERSION_NAME}")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
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
    repositories {
        maven {
            name = "nexus"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = "NEXUS_USERNAME".byProperty
                password = "NEXUS_PASSWORD".byProperty
            }
        }
        maven {
            name = "snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            credentials {
                username = "NEXUS_USERNAME".byProperty
                password = "NEXUS_PASSWORD".byProperty
            }
        }
    }

    publications {
        register<MavenPublication>("release") {
            artifactId = "android-drawable"
            groupId = Publication.GROUP
            version = Publication.VERSION_NAME

            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))
            }
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourceJar"))

            pom {
                if (!"USE_SNAPSHOT".byProperty.isNullOrBlank()) {
                    version = "$version-SNAPSHOT"
                }
                name.set("Android Gif Drawable")
                url.set(Publication.Pom.URL)
                description.set(
                    """An implementation of an Android Drawable providing a simple way 
                    |to display a gif in an Android app""".trimMargin()
                )
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("redwarp")
                        name.set("Benoît Vermont")
                        email.set("redwarp@gmail.com")
                        url.set("https://github.com/redwarp")
                    }
                }
                scm {
                    connection.set(Publication.Pom.SCM_CONNECTION)
                    developerConnection.set(Publication.Pom.SCM_DEVELOPER_CONNECTION)
                    url.set(Publication.Pom.SCM_URL)
                }
                issueManagement {
                    system.set("GitHub issues")
                    url.set(Publication.Pom.ISSUE_TRACKER_URL)
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

    val signingKey = "SIGNING_KEY".byProperty
    val signingPwd = "SIGNING_PASSWORD".byProperty

    if (signingKey.isNullOrBlank() || signingPwd.isNullOrBlank()) {
        logger.info("Signing Disable as the PGP key was not found")
    } else {
        logger.info("GPG Key found - Signing enabled")
        signing {
            @Suppress("UnstableApiUsage")
            useInMemoryPgpKeys(signingKey, signingPwd)
            sign(publishing.publications["release"])
        }
    }
}

val String.byProperty: String? get() = findProperty(this) as? String
