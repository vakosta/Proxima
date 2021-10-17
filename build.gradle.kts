import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"

    id("io.reflekt") version "1.5.21"
    id("org.jetbrains.compose") version "1.0.0-alpha3"
}

group = "ru.hse"
version = "1.0"

repositories {
    google()
    mavenCentral()

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven(url = uri("https://packages.jetbrains.team/maven/p/reflekt/reflekt"))
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.5.21")

    implementation(compose.desktop.currentOs)
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.reflekt", "reflekt-dsl", "1.5.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.5"
        apiVersion = "1.5"
        jvmTarget = "11"
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HSEditor"
            packageVersion = "1.0.0"
        }
    }
}
