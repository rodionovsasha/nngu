plugins { `java-library` }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

sourceSets {
    main {
        java.srcDir(rootProject.projectDir)
        java.include("PotentialWellSpectrum.java", "ElectronPhotonTransition.java", "QuantumWellModel.java")
    }
    test {
        java.srcDir(rootProject.projectDir)
        java.include("QuantumWellSelfTest.java", "QuantumWellModelTest.java")
    }
}

val selfTest = tasks.register<JavaExec>("selfTest") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("QuantumWellSelfTest")
}
val modelTest = tasks.register<JavaExec>("modelTest") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("QuantumWellModelTest")
}
tasks.check { dependsOn(selfTest, modelTest) }
// Проверки запускаются как Java main, без JUnit и сторонних test engines.
tasks.test { enabled = false }
