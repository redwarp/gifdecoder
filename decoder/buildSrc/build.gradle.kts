plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

sourceSets {
    main {
        java.srcDir("../../buildSrc/src/main/kotlin")
    }
}
