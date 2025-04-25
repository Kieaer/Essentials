dependencies{
    compileOnly(libs.bundles.game)

    implementation(libs.discord) {
        exclude(module = "opus-java")
    }
}