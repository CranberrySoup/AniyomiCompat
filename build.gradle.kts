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
        classpath("com.github.recloudstream:gradle:master-SNAPSHOT")

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

//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//        // Shitpack repo which contains our tools and dependencies
//        maven("https://jitpack.io")
//    }
//
//    dependencies {
//        classpath("com.android.tools.build:gradle:7.0.4")
//        // Cloudstream gradle plugin which makes everything work and builds plugins
//        classpath("com.github.recloudstream:gradle:master-SNAPSHOT")
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
//    }
//}
//allprojects {
//    apply(plugin = "com.android.application")
//    apply(plugin = "kotlin-android")
//    apply(plugin = "com.lagradost.cloudstream3.gradle")
//}
//plugins {
//    id 'com.android.application' version '8.0.1' apply false
//    id 'com.android.library' version '8.0.1' apply false
//    id 'org.jetbrains.kotlin.android' version '1.8.20' apply false
//}


