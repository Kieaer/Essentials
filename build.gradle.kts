import java.nio.file.Files.move

plugins {
    base
}

tasks.register("jar") {
    dependsOn(subprojects.map { it.tasks.named("jar") })

    doLast {
        subprojects.forEach { subproject ->
            move(subproject.layout.buildDirectory.file("libs/${subproject.name}-all.jar").get().asFile.toPath(), rootProject.layout.buildDirectory.file("${subproject.name}.jar").get().asFile.toPath())
            println("project build: ${subproject.name}")
        }
    }
}