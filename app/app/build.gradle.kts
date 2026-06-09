plugins {
    alias(libs.plugins.android.library)
}

version = "0.1.0"
group = "com.suyashbelekar.exoplayerhdrutils"

android {
    namespace = "com.suyashbelekar.exoplayerhdrutils"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.media3.exoplayer)
}