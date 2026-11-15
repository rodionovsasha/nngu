plugins {
    application
    id("org.openjfx.javafxplugin")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}
javafx {
    version = "27.0.1"
    modules("javafx.controls")
}
dependencies { implementation(project(":physics")) }
application {
    mainClass.set("QuantumWellFxLauncher")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=javafx.graphics")
}
