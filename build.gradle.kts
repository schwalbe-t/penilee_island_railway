
plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-openxr")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.joml:joml:1.10.5")

    runtimeOnly("org.lwjgl:lwjgl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-openxr::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-stb::natives-windows")
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
