import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.shadowJar) apply false
    `maven-publish`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    tasks.shadowJar {
        mergeServiceFiles()

        exclude("arc/**")
        exclude("mindustry/**")
        exclude("server/**")

        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/proguard/**", "META-INF/com.android.tools/**")
        exclude("META-INF/maven/**", "META-INF/native-image/**", "META-INF/rewrite/**")
        exclude("META-INF/README*", "META-INF/CHANGELOG")
        exclude("META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider")
        exclude("META-INF/services/javax.annotation.processing.Processor")
        exclude("META-INF/services/reactor.blockhound.integration.BlockHoundIntegration")
        exclude("META-INF/services/io.micrometer.context.ContextAccessor")
        exclude("META-INF/*.kotlin_module")
        exclude("module-info.class", "META-INF/versions/**/module-info.class")
        exclude("arc64.dll", "libarc64.so", "libarcarm64.so", "libarc64.dylib", "libarcarm64.dylib")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        dependencies {
            exclude(dependency("Anuken:Mindustry:.*"))
        }

        minimize {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
            exclude(dependency("io.r2dbc:.*:.*"))
            exclude(dependency("org.postgresql:.*:.*"))
            exclude(dependency("io.asyncer:.*:.*"))
            exclude(dependency("org.mariadb:.*:.*"))
            exclude(dependency("org.mariadb.jdbc:.*:.*"))
            exclude(dependency("io.projectreactor:.*:.*"))
            exclude(dependency("io.ktor:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization.*:.*"))
            exclude(dependency("org.flywaydb:.*:.*"))
            exclude(dependency("com.fasterxml.jackson.*:.*:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib.*:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines.*:.*"))
            exclude(dependency("com.charleskorn.kaml:.*:.*"))
            exclude(dependency("it.krzeminski:.*:.*"))
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "essentials"
                artifactId = project.name.lowercase()
                version = "1.0.0"

                from(components["java"])
            }
        }
    }
}
