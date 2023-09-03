// See https://medium.com/@saschpe/android-library-publication-in-2020-93e8c0e106c8
plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("signing")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "app.redwarp.gif.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api("app.redwarp.gif:decoder:${Publication.VERSION_NAME}")
    implementation("androidx.appcompat:appcompat:${Versions.APP_COMPAT}")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
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
                from(components["release"])
            }

            pom {
                if (!"USE_SNAPSHOT".byProperty.isNullOrBlank()) {
                    version = "$version-SNAPSHOT"
                }
                name.set("Android Gif Drawable")
                url.set(Publication.Pom.URL)
                description.set(
                    """An implementation of an Android Drawable providing a simple way 
                    |to display a gif in an Android app
                    """.trimMargin(),
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
