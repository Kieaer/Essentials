import java.nio.file.Files.move

plugins {
    base
}

tasks.register("jar") {
    dependsOn(subprojects.map { it.tasks.named("jar") })

    doLast {
        subprojects.forEach { subproject ->
            val rootFile = file("E:\\test\\config\\mods\\${subproject.name}.jar").toPath()
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