rootProject.name = "Essentials"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven {
            url = uri("http://127.0.0.1:8081/repository/maven-snapshots/")
            isAllowInsecureProtocol = true
        }

        maven { url = uri("https://jitpack.io") }
    }
}

include("Essential")
include("EssentialChat")
include("EssentialProtect")
include("EssentialBridge")
include("EssentialDiscord")
include("EssentialWeb")
include("EssentialAchievements")
include("ksp-processor")

include("common")