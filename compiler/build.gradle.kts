plugins {
    alias(libs.plugins.kotlin)
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
    jvmToolchain(8)
}

dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.arrowkt.core)

    testImplementation(libs.bundles.testing)
}