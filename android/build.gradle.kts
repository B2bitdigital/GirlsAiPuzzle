plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = "1.12.1"

configurations {
    create("natives")
}

android {
    namespace = "game.girlsaipanic"
    compileSdk = 36

    defaultConfig {
        applicationId = "game.girlsaipanic"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"YOUR_REAL_BANNER_ID_HERE\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"YOUR_REAL_INTERSTITIAL_ID_HERE\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { buildConfig = true }

    sourceSets {
        getByName("main") {
            assets.srcDirs("assets")
            jniLibs.srcDirs("libs")
        }
    }
}

val natives by configurations

tasks.register("copyAndroidNatives") {
    doFirst {
        listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").forEach {
            file("libs/$it").mkdirs()
        }
        natives.files.forEach { jar ->
            val outputDir = when {
                jar.name.endsWith("natives-armeabi-v7a.jar") -> file("libs/armeabi-v7a")
                jar.name.endsWith("natives-arm64-v8a.jar") -> file("libs/arm64-v8a")
                jar.name.endsWith("natives-x86.jar") -> file("libs/x86")
                jar.name.endsWith("natives-x86_64.jar") -> file("libs/x86_64")
                else -> null
            }
            outputDir?.let {
                copy {
                    from(zipTree(jar))
                    into(it)
                    include("*.so")
                }
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("package") || name.contains("assemble") || name.contains("Bundle")) {
        dependsOn("copyAndroidNatives")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
