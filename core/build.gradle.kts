plugins {
    kotlin("jvm")
}

val gdxVersion = "1.12.1"

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    testImplementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
