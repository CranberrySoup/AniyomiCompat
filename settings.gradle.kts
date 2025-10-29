pluginManagement {
    resolutionStrategy {
        eachPlugin {
            val regex = "com.android.(library|application)".toRegex()
            if (regex matches requested.id.id) {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
        create("androidx") {
            from(files("gradle/androidx.versions.toml"))
        }
        create("compose") {
            from(files("gradle/compose.versions.toml"))
        }
        create("aniyomilibs") {
            from(files("gradle/aniyomi.versions.toml"))
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
}


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "AniyomiCompat"
val subdir = "aniyomi"
include(":app")
includeBuild(subdir)

gradle.projectsEvaluated {
    val localPropsFile = File(rootDir, "local.properties")
    if (localPropsFile.exists()) {
        val props = java.util.Properties()
        localPropsFile.inputStream().use { props.load(it) }

        val projectPropsFile = File(subdir, "local.properties")
        if (!projectPropsFile.exists()) {
            projectPropsFile.createNewFile()
        }

        projectPropsFile.writer().use { writer ->
            props.forEach { (key, value) ->
                val k = key.toString()
                val v = value.toString()
                writer.write("$k=$v\n")
            }
        }
    }
}

