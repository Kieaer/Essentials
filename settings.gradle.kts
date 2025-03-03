rootProject.name = "Essentials"

sequenceOf(
    "",
    "Chat",
    "Protect",
    "Bridge",
    "Discord",
    "Web",
    "Achievements"
).forEach {
    include(":Essential$it")
    project(":Essential$it").projectDir = file("Essential$it")
}