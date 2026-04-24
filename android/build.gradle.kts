plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = "1.12.1"

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
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
