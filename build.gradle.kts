/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 */

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm") version "1.3.20"

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.commons:commons-dbcp2:2.6.0")
    runtimeOnly("mysql:mysql-connector-java:5.1.41")
}

application {
    // Define the main class for the application.
    mainClassName = "wanghe.sample.AppKt"
}