
plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

repositories {
    mavenCentral()
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglVersion = "3.3.3"
val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("mac") && osArch.contains("aarch64") -> "natives-macos-arm64"
    osName.contains("mac") -> "natives-macos"
    osArch.contains("aarch64") || osArch.contains("arm") -> "natives-linux-arm64"
    else -> "natives-linux"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-openxr")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.joml:joml:1.10.5")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openxr::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
}

application {
    mainClass = "schwalbe.penilee.MainKt"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    sourceSets {
        main { kotlin.srcDirs("src") }
    }
    sourceSets.all {
        languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }
}
