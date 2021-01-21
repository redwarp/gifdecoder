import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version Versions.KOTLIN
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_GRADLE
    id("com.github.ben-manes.versions") version "0.36.0"
    id("org.jetbrains.dokka") version Versions.DOKKA
    id("maven-publish")
    id("com.jfrog.bintray") version Versions.BINTRAY
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
    publications {
        register<MavenPublication>("release") {
            from(components["java"])

            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourceJar"))

            pom {
                name.set("Kotlin Gif Decoder")
                url.set(Publication.Pom.URL)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
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

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    setPublications("release")
    publish = true

    pkg(
        delegateClosureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            name = "gif-decoder"
            websiteUrl = Publication.Pom.URL
            vcsUrl = Publication.Pom.VCS_URL
            issueTrackerUrl = Publication.Pom.ISSUE_TRACKER_URL
            setLicenses("Apache-2.0")
            setLabels("kotlin", "gif")
            userOrg = "redwarp"
            publicDownloadNumbers = true
            override = true

            version(
                delegateClosureOf<BintrayExtension.VersionConfig> {
                    name = Publication.VERSION_NAME
                    vcsTag = "v${Publication.VERSION_NAME}"
                }
            )
        }
    )
}
