import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

/* Apply the plugins to the build */
plugins {
    kotlin("jvm") version "1.3.11"
}

/* Describe what we are building */
group = "com.nickwongdev.netperf"
version = "1.0-SNAPSHOT"

/* Where to pull dependencies from */
repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.jfrog.org/oss-release-local")
}

/* Gather Sources */
val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

/* Dependencies to build the project */
dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.rsocket.kotlin:rsocket-core:0.9.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}