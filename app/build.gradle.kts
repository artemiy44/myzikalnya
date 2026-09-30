import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.artemiy.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.artemiy.player"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "0.62 beta"
    }

    buildFeatures {
        compose = true
    }

    // The release key lives outside the project (never in git): keys/keystore.properties next to
    // it says where the key file is and its passwords. Without that file, releases are signed
    // with the debug key as before.
    val releaseKeyProps = rootProject.file("../keys/keystore.properties")
    if (releaseKeyProps.exists()) {
        val props = Properties().apply { releaseKeyProps.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // The build to actually use: not debuggable, so Android fully optimizes it (a debug
        // build runs Compose several times slower). Signed with the release key when there is
        // one (see above), otherwise with the debug key.
        // Debug builds carry the same signature, so they install over the release and back
        // again without losing the app's data (Android refuses an update with another key).
        debug {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            // Kuromoji's core and dictionary jars both ship these.
            excludes += setOf("META-INF/CONTRIBUTORS.md", "META-INF/LICENSE.md", "META-INF/NOTICE.md")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
}
