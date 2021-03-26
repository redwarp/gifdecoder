import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version Versions.KOTLIN
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_GRADLE
    id("com.github.ben-manes.versions") version Versions.VERSIONS
    id("org.jetbrains.dokka") version Versions.DOKKA
    id("maven-publish")
    id("signing")
}

base {
    archivesBaseName = "decoder"
    group = Publication.GROUP
    version = Publication.VERSION_NAME
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("io.netty:netty-buffer:${Versions.NETTY_BUFFER}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test") {
        java.srcDir("src/test/kotlin")
        resources.srcDir("../assets")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes", "-Xinline-classes")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

task("sourceJar", Jar::class) {
    from(sourceSets.getByName("main").java)
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
            from(components["java"])

            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourceJar"))

            pom {
                name.set("Kotlin Gif Decoder")
                url.set(Publication.Pom.URL)
                description.set("""A simple jvm library written 100% in kotlin that handles 
                    |the parsing of the gif format, headers, lzw decoder and so on.""".trimMargin())
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

    signing {
        @Suppress("UnstableApiUsage")
        useInMemoryPgpKeys(signingKey, signingPwd)
        sign(publishing.publications["release"])
    }
}

val isNonStable = { version: String ->
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = Regex("/^[0-9,.v-]+(-r)?$/")
    !stableKeyword && !(regex.matches(version))
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

val String.byProperty: String? get() = findProperty(this) as? String
