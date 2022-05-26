// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val hiltVersion = "2.42"
    val kotlinVersion = "1.6.21"

    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
        classpath("com.google.gms:google-services:4.3.10")

        // Hilt
        classpath("com.google.dagger:hilt-android-gradle-plugin:${hiltVersion}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id("com.diffplug.gradle.spotless") version "4.2.1"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

spotless {
    format("misc") {
        target("**/*.gradle", "**/*.md", "**/.gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
    freshmark {
        target("**/*.md")
        propertiesFile("gradle.properties")
    }
    java {
        target("**/*.java")
        removeUnusedImports()
        googleJavaFormat().aosp()
    }
    kotlin {
        target("*/src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}
