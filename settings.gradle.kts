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
    val snapshotIp = System.getenv("LOCAL_REPO_IP")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        if (snapshotIp != null && snapshotIp.isNotEmpty()) {
            maven {
                url = uri("http://$snapshotIp/repository/maven-snapshots/")
                isAllowInsecureProtocol = true
            }
        } else {
            maven { url = uri("https://jitpack.io") }
        }
    }
}

include("Essential")
include("ksp-processor")