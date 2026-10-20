
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

// ---- Functional test source set (integration tests that run inside the Forge server) ----
// Pattern taken from GT5-Unofficial. The test @Mod fires on FMLServerStartedEvent and runs
// JUnit 5 via the Platform Launcher API, writing XML results to ./junit-out/.

val functionalTest: SourceSet = sourceSets.create("functionalTest") {
    java {
        srcDir("src/functionalTest/java")
        compileClasspath += sourceSets.patchedMc.get().output + sourceSets.main.get().output
    }
}

configurations {
    // Inherit main mod's full compile/runtime classpath so the test mod can see all mod classes.
    named(functionalTest.compileClasspathConfigurationName).configure { extendsFrom(configurations.compileClasspath.get()) }
    named(functionalTest.runtimeClasspathConfigurationName).configure { extendsFrom(configurations.runtimeClasspath.get()) }
    named(functionalTest.annotationProcessorConfigurationName).configure { extendsFrom(configurations.annotationProcessor.get()) }
}

tasks.register<Jar>(functionalTest.jarTaskName) {
    from(functionalTest.output)
    archiveClassifier.set("functionalTests")
    archiveVersion.set("1.0")
    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
}

tasks.assemble.configure {
    dependsOn(functionalTest.jarTaskName)
}

// Include the functional test jar and its runtime classpath in every server run task (runServer and the
// modern-Java runServer17/21/25) so the test mod is loaded. The modern-Java runs matter because GT5U ships
// separate Java 17 class files (META-INF/versions/17) that only those JVMs load.
// Only the test-only extras (JUnit) are added: the run task already carries the main runtime classpath, and the
// modern-Java runs transform their copy of it differently, so re-adding it would load every mod twice.
tasks.withType<RunMinecraftTask>().matching { it.name.startsWith("runServer") }.configureEach {
    dependsOn(functionalTest.jarTaskName)
    classpath(
        configurations.named(functionalTest.runtimeClasspathConfigurationName)
            .map { it - configurations.runtimeClasspath.get() },
        tasks.named(functionalTest.jarTaskName))
}
