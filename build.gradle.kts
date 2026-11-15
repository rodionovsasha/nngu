plugins {
    base
    kotlin("multiplatform") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
}

tasks.named("check") {
    dependsOn(":physics:check", ":javafx:check", ":compose:allTests")
}

tasks.named("build") {
    dependsOn(":physics:build", ":javafx:build", ":compose:build")
}
