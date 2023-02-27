import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.compose") version "1.0.0-alpha3"
    antlr
}

group = "me.vakosta"
version = "1.0"

val koinVersion = "3.1.2"
val antlrVersion = "4.9.2"
val intervalTreeVersion = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("com.lodborg:interval-tree:$intervalTreeVersion")

    antlr("org.antlr:antlr4-runtime:$antlrVersion")
    antlr("org.antlr:antlr4-maven-plugin:$antlrVersion")

//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.5.21")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
}

tasks {
    test {
        useJUnit()
    }

    generateGrammarSource {
        maxHeapSize = "64m"
        arguments = arguments + listOf("-visitor", "-long-messages")
        arguments = arguments + listOf("-package", "me.annenkov.proxima.antlr")
        outputDirectory = File("$buildDir/generated-src/antlr/main/me/annenkov/proxima/antlr")
    }

    withType<KotlinCompile> {
        dependsOn(generateGrammarSource)
        kotlinOptions {
            languageVersion = "1.5"
            apiVersion = "1.5"
            jvmTarget = "11"
        }
    }
}

compose.desktop {
    application {
        mainClass = "me.vakosta.proxima.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Proxima"
            packageVersion = "1.0.0"
        }

        jvmArgs("-Dcompose.application.configure.swing.globals=false")
    }
}
