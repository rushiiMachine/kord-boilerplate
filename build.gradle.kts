import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jetbrains.kotlin.kapt") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
}

group = "com.github.diamondminer88"
version = "1.0.0"

repositories {
    maven("https://maven.kotlindiscord.com/repository/maven-public/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC2")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("dev.kord:kord-core:0.8.x-SNAPSHOT")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.1-RC1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

application {
    mainClassName = "bot.MainKt"
}

//tasks.withType<ShadowJar> {
//    manifest.attributes("Main-Class" to "bot.MainKt")
//}

// https://github.com/johnrengelman/shadow/issues/574#issuecomment-630766388
//tasks.register<JavaExec>("runShadowFixed") {
//    val jarFile = (project.tasks.findByName("shadowJar") as Jar).archiveFile
//    inputs.files(jarFile).withPropertyName("jarFile").withPathSensitivity(PathSensitivity.RELATIVE)
//    main = "-jar"
//    description = "Runs this project as a JVM application using the shadow jar (fixed for Gradle 6.4+)"
//    group = ApplicationPlugin.APPLICATION_GROUP
//    jvmArgs = application.applicationDefaultJvmArgs.toList()
//
//    doFirst {
//        setArgs(mutableListOf(jarFile.get().asFile.path) + getArgs())
//    }
//}

//tasks.register<JavaExec>("runJar") {
//    group = "bot"
//    dependsOn("shadowJar")
//    main = "bot.MainKt"
//    args(tasks.shadowJar.get().archiveFile.get())
//}

