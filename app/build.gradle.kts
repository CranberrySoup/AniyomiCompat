plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.lagradost.cloudstream3.gradle")
}

android {
    namespace = "com.lagradost.aniyomicompat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lagradost.aniyomicompat"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Do not package huge aniyomi libs
            excludes += "lib/*/**"
        }
    }
}

val patchFile = tasks.register("patchSourceFile") {
    doFirst {
        val sourceFile = file("./../$subdir/core/common/src/main/java/tachiyomi/core/common/i18n/Localize.kt")

        val content = sourceFile.readText()
        if (content.contains("resource.resourceId.toString()")) return@doFirst

        // Resource strings do not work properly in extensions, use the id as opposed to resolving the string
        val patchedContent = content.replace("return StringDesc.", "return resource.resourceId.toString() // StringDesc.")

        sourceFile.writeText(patchedContent)

        println("Source file patched!")
    }
}

val subdir = "aniyomi"

tasks.named("build") {
    dependsOn(gradle.includedBuild(subdir).task(":app:build"))
}

tasks.named("clean") {
    dependsOn(gradle.includedBuild(subdir).task(":clean"))
}

dependencies {
    implementation(libs.activity)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    apk("com.lagradost:cloudstream3:pre-release")
    api(libs.moko.core)
    implementation(files("./../$subdir/app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar"))

//    implementation("Aniyomi:app") // Fucks up gradle
    implementation("Aniyomi:core-metadata")
    implementation("Aniyomi:data")
    implementation("Aniyomi:domain")
    implementation("Aniyomi:i18n")
    implementation("Aniyomi:i18n-aniyomi")
    implementation("Aniyomi:presentation-core")
    implementation("Aniyomi:presentation-widget")
    implementation("Aniyomi:source-api")
    implementation("Aniyomi:source-local")


    // Compose
    implementation(compose.activity)
    implementation(compose.foundation)
//    implementation(compose.material3.core)
    implementation("androidx.compose.material3:material3:1.4.0")
//    implementation(compose.material.icons)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(compose.animation)
    implementation(compose.animation.graphics)
    debugImplementation(compose.ui.tooling)
    implementation(compose.ui.tooling.preview)
    implementation(compose.ui.util)

    implementation(androidx.interpolator)

    implementation(androidx.paging.runtime)
    implementation(androidx.paging.compose)

    implementation(libs.bundles.sqlite)

    implementation(kotlinx.reflect)
    implementation(kotlinx.immutables)

    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.bundles.coroutines)

    // AndroidX libraries
    implementation(androidx.annotation)
    implementation(androidx.appcompat)
    implementation(androidx.biometricktx)
    implementation(androidx.constraintlayout)
    implementation(aniyomilibs.compose.constraintlayout)
    implementation(androidx.corektx)
    implementation(androidx.splashscreen)
    implementation(androidx.recyclerview)
    implementation(androidx.viewpager)
    implementation(androidx.profileinstaller)
    implementation(aniyomilibs.mediasession)

    implementation(androidx.bundles.lifecycle)

    // Job scheduling
    implementation(androidx.workmanager)

    // RxJava
    implementation(libs.rxjava)

    // Networking
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)
    implementation(libs.conscrypt.android) // TLS 1.3 support for Android < 10

    // Data serialization (JSON, protobuf, xml)
    implementation(kotlinx.bundles.serialization)

    // HTML parser
    implementation(libs.jsoup)

    // Disk
    implementation(libs.disklrucache)
    implementation(libs.unifile)

    // Preferences
    implementation(libs.preferencektx)

    // Dependency injection
    implementation(libs.injekt)

    // Image loading
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)
    implementation(libs.subsamplingscaleimageview) {
        exclude(module = "image-decoder")
    }
    implementation(libs.image.decoder)

    // UI libraries
    implementation(libs.material)
    implementation(libs.flexible.adapter.core)
    implementation(libs.photoview)
    implementation(libs.directionalviewpager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation(libs.insetter)
    implementation(libs.bundles.richtext)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.bundles.voyager)
    implementation(libs.compose.materialmotion)
    implementation(libs.swipe)
    implementation(libs.compose.webview)
    implementation(libs.compose.grid)
    implementation(libs.reorderable)

    // Logging
    implementation(libs.logcat)

    // Shizuku
    implementation(libs.bundles.shizuku)

    // Tests
    testImplementation(libs.bundles.test)

    // For detecting memory leaks; see https://square.github.io/leakcanary/
    // debugImplementation(libs.leakcanary.android)

    implementation(libs.leakcanary.plumber)

    testImplementation(kotlinx.coroutines.test)

    // mpv-android
    implementation(aniyomilibs.aniyomi.mpv)
    // FFmpeg-kit
    implementation(aniyomilibs.ffmpeg.kit)
    implementation(aniyomilibs.arthenica.smartexceptions)
    // seeker seek bar
    implementation(aniyomilibs.seeker)
    // true type parser
    implementation(aniyomilibs.truetypeparser)
}
