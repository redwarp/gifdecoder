plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version Versions.KOTLIN
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("org.jetbrains.dokka") version Versions.DOKKA
}

project.group = Configuration.GROUP
project.version = Configuration.VERSION

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
    getByName("test") {
        java.srcDir("src/test/kotlin")
        resources.srcDir("assets")
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

tasks.jar {
}

val isNonStable = { version: String ->
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { it -> version.toUpperCase().contains(it) }
    val regex = Regex("/^[0-9,.v-]+(-r)?$/")
    !stableKeyword && !(regex.matches(version))
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java)
    .configure {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
