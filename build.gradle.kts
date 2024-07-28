import java.nio.file.Files.copy
import java.nio.file.Files.move
import java.nio.file.StandardCopyOption

plugins {
    base
}

tasks.register("jar") {
    dependsOn(subprojects.map { it.tasks.named("jar") })

    doLast {
        rootProject.layout.buildDirectory.get().asFile.mkdirs()
        subprojects.forEach { subproject ->
            val rootFile = rootProject.layout.buildDirectory.file("${subproject.name}.jar").get().asFile.toPath()
            copy(subproject.layout.buildDirectory.file("libs/${subproject.name}.jar").get().asFile.toPath(), rootFile, StandardCopyOption.REPLACE_EXISTING)
            println("project build: ${subproject.name}")
        }
    }
}

tasks.register("shadowJar") {
    dependsOn(subprojects.map { it.tasks.named("shadowJar") })

    doLast {
        subprojects.forEach { subproject ->
            val rootFile = rootProject.layout.buildDirectory.file("${subproject.name}.jar").get().asFile.toPath()
            delete(rootFile)
            move(subproject.layout.buildDirectory.file("libs/${subproject.name}-all.jar").get().asFile.toPath(), rootFile)
            println("project build: ${subproject.name}")
        }
    }
}

tasks.register("clear") {
    dependsOn(subprojects.map { it.tasks.named("clean") })

    subprojects.forEach { subproject ->
        val rootFile = rootProject.layout.buildDirectory.file("${subproject.name}.jar").get().asFile.toPath()
        delete(rootFile)
    }
}