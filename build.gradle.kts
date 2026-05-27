
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

val functionalTest by sourceSets.creating {
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

// Include the functional test jar and its runtime classpath in runServer so the test mod is loaded.
tasks.named<RunMinecraftTask>("runServer").configure {
    dependsOn(functionalTest.jarTaskName)
    classpath(configurations.named(functionalTest.runtimeClasspathConfigurationName), tasks.named(functionalTest.jarTaskName))
}
