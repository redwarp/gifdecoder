plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version Versions.KOTLIN
    id("se.ascp.gradle.gradle-versions-filter") version Versions.VERSIONS
    id("com.diffplug.spotless") version Versions.SPOTLESS
    id("org.jetbrains.dokka") version Versions.DOKKA
    id("maven-publish")
    id("signing")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test") {
        java.srcDir("src/test/kotlin")
        resources.srcDir("../assets")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val javadocJar = task("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from("$buildDir/dokka/javadoc")
}

java {
    withJavadocJar()
    withSourcesJar()
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
            from(components["java"])

            artifactId = "decoder"
            group = Publication.GROUP
            version = Publication.VERSION_NAME

            pom {
                name.set("Kotlin Gif Decoder")
                url.set(Publication.Pom.URL)
                description.set(
                    """A simple jvm library written 100% in kotlin that handles 
                    |the parsing of the gif format, headers, lzw decoder and so on.
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
            useInMemoryPgpKeys(signingKey, signingPwd)
            sign(publishing.publications["release"])
        }
    }
}

val String.byProperty: String? get() = findProperty(this) as? String

spotless {
    kotlin {
        target(
            project.fileTree(
                project.projectDir,
            ) {
                include("**/app/redwarp/gif/decoder/**/*.kt")
            },
        )

        licenseHeaderFile("${project.rootDir}/../license_header.txt")
    }
}
