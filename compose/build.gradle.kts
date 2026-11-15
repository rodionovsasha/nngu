import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(25)
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_25) }
    }
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":physics"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
        }
    }
}

compose.desktop {
    application {
        mainClass = "QuantumWellComposeKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "QuantumWellCompose"
            packageVersion = "1.0.0"
            modules("java.desktop", "java.logging", "jdk.unsupported")
        }
    }
}
