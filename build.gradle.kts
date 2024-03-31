plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "ru.omgtu.ivt213.mishenko.maksim"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.selenium.core)
    implementation(libs.selenium.support)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}