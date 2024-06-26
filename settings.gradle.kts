plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

sequenceOf(
    "",
    "AntiCheat",
    "Chat",
    "Protect",
    "Bridge",
    "Discord",
    "Web"
).forEach {
    include(":Essential$it")
    project(":Essential$it").projectDir = file("Essential$it")
}