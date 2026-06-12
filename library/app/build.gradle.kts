plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

version = "0.2.0"
group = "com.suyashbelekar.exoplayerhdrutils"

android {
    namespace = "com.suyashbelekar.exoplayerhdrutils"
    compileSdk = 36

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

mavenPublishing {
    coordinates("com.suyashbelekar", "exoplayerhdrutils", "0.2.0")

    pom {
        name.set("ExoPlayer HDR Utils")
        description.set("ExoPlayer utilities for Dolby Vision and HDR10+ metadata conversions using libdovi.")
        inceptionYear.set("2026")
        url.set("https://github.com/suyash192/ExoplayerHdrUtils")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("suyash192")
                name.set("Suyash Belekar")
                url.set("https://github.com/suyash192/")
            }
        }
        scm {
            url.set("https://github.com/suyash192/ExoplayerHdrUtils/")
            connection.set("scm:git:git://github.com/suyash192/ExoplayerHdrUtils.git")
            developerConnection.set("scm:git:ssh://git@github.com/suyash192/ExoplayerHdrUtils.git")
        }
    }

    publishToMavenCentral()

    signAllPublications()
}