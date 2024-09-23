import com.github.spotbugs.snom.SpotBugsTask

plugins {
    `java-library`
}

apply(plugin = "project")

subprojects {
    group = "miller.playground"
    version = "1.0.0"

    if (!plugins.hasPlugin("java")) {
        return@subprojects
    }

    sourceSets {
        val dev by creating {
            java.srcDir("src/dev/java")
            compileClasspath += sourceSets.named("main").get().compileClasspath + sourceSets.named("test").get().compileClasspath
            runtimeClasspath += sourceSets.named("main").get().runtimeClasspath + sourceSets.named("test").get().runtimeClasspath
        }
    }

    tasks.named<Pmd>("pmdDev") {
        group = "verification"
        ignoreFailures = true
    }

    tasks.named<SpotBugsTask>("spotbugsDev") {
        group = "verification"
        ignoreFailures = true
    }
}

project("datadog-service-catalog-validator") {
    apply(plugin = "app")
    dependencies {
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
        implementation("com.github.erosb:everit-json-schema:1.14.4")
    }
}
