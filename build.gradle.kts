import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        // we stay on low ver because prerelease build gradle is fucked
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.5.0")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
