plugins {
    application
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
}

application {
    mainClass = "com.ivy.quickcode.MainKt"
    applicationName = "qc"
}

group = "com.ivy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.arrowkt.core)

    testImplementation(libs.bundles.testing)
}